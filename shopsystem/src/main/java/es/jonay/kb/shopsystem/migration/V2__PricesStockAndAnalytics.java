package es.jonay.kb.shopsystem.migration;

import java.math.BigDecimal;
import java.util.logging.Logger;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * V2: precios, stock e informes "de negocio serio".
 * <ol>
 * <li>{@code refresh_tokens}: sesiones con refresh token (fase 1).</li>
 * <li>{@code items.sale_price / cost_price}: precio de venta y coste actuales en DECIMAL
 * (antes eran el último valor de dos mapas de DOUBLE).</li>
 * <li>{@code item_price_history}: historial de precios (una fila por cambio: venta + coste).</li>
 * <li>{@code stock_movements}: libro de movimientos de stock (ventas, entradas,
 * devoluciones, mermas, ajustes) con el saldo resultante. Arranca con el saldo inicial.</li>
 * <li>{@code trade_lines}: se congelan también coste y categoría; las ventas antiguas que
 * no tenían líneas reciben las suyas (precio e IGIC de la fecha de la venta).</li>
 * <li>{@code categories.id} pasa a AUTO_INCREMENT (adiós a MAX(id)+1).</li>
 * </ol>
 * Es adaptativa e idempotente: comprueba cada tabla, columna e índice antes de tocarla.
 * Las tablas antiguas {@code price_history} y {@code net_history} NO se borran: quedan como
 * archivo (solo se quitan sus claves foráneas para poder borrar productos).
 */
@Component
public class V2__PricesStockAndAnalytics extends BaseJavaMigration {

    private final Logger logger = Logger.getLogger(V2__PricesStockAndAnalytics.class.getName());
    private final BigDecimal defaultIgicRate;

    public V2__PricesStockAndAnalytics(@Value("${shop.tax.default-igic-rate:7.00}") BigDecimal defaultIgicRate) {
        this.defaultIgicRate = defaultIgicRate;
    }

    @Override
    public void migrate(Context context) throws Exception {
        SchemaInspector db = new SchemaInspector(context.getConnection());
        refreshTokens(db);
        itemPrices(db);
        priceHistory(db);
        stockMovements(db);
        tradeLines(db);
        categoriesAutoIncrement(db);
        indexes(db);
    }

    // ------------------------------------------------------------------ 1. sesiones

    private void refreshTokens(SchemaInspector db) throws Exception {
        if (db.tableExists("refresh_tokens")) {
            return;
        }
        db.execute("""
                CREATE TABLE refresh_tokens (
                  id bigint NOT NULL AUTO_INCREMENT,
                  token_hash varchar(64) NOT NULL,
                  user_email varchar(255) NOT NULL,
                  created_at datetime(6) NOT NULL,
                  expires_at datetime(6) NOT NULL,
                  revoked bit(1) NOT NULL DEFAULT b'0',
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_refresh_token_hash (token_hash),
                  KEY idx_refresh_token_email (user_email)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
    }

    // ------------------------------------------------------------------ 2. precio actual

    private void itemPrices(SchemaInspector db) throws Exception {
        if (!db.columnExists("items", "sale_price")) {
            db.execute("ALTER TABLE items ADD COLUMN sale_price decimal(10,2) NULL");
        }
        if (!db.columnExists("items", "cost_price")) {
            db.execute("ALTER TABLE items ADD COLUMN cost_price decimal(10,2) NULL");
        }
        // Columna "price" del volcado antiguo (database.sql): si existe y es NOT NULL, impediría
        // dar de alta productos. Se conserva el dato, solo se permite NULL.
        boolean legacyPrice = db.columnExists("items", "price");
        if (legacyPrice && !db.columnIsNullable("items", "price")) {
            db.execute("ALTER TABLE items MODIFY price double NULL");
        }

        String fallback = legacyPrice ? "i.price" : "NULL";
        db.execute("""
                UPDATE items i SET i.sale_price = ROUND(COALESCE(
                  (SELECT ph.price FROM price_history ph WHERE ph.item_id = i.id ORDER BY ph.date DESC LIMIT 1),
                  %s, 0), 2)
                WHERE i.sale_price IS NULL""".formatted(fallback));
        db.execute("""
                UPDATE items i SET i.cost_price = ROUND(COALESCE(
                  (SELECT nh.net FROM net_history nh WHERE nh.item_id = i.id ORDER BY nh.date DESC LIMIT 1),
                  0), 2)
                WHERE i.cost_price IS NULL""");

        db.execute("ALTER TABLE items MODIFY sale_price decimal(10,2) NOT NULL DEFAULT 0.00");
        db.execute("ALTER TABLE items MODIFY cost_price decimal(10,2) NOT NULL DEFAULT 0.00");
        // Valores sin nulos para los campos de control (el código antiguo podía dejarlos a NULL).
        db.execute("UPDATE items SET stock = 0 WHERE stock IS NULL");
        db.execute("UPDATE items SET bare_minimun = 0 WHERE bare_minimun IS NULL");
        db.execute("UPDATE items SET ignore_stock = 0 WHERE ignore_stock IS NULL");
    }

    // ------------------------------------------------------------------ 3. historial de precios

    private void priceHistory(SchemaInspector db) throws Exception {
        if (!db.tableExists("item_price_history")) {
            db.execute("""
                    CREATE TABLE item_price_history (
                      id bigint NOT NULL AUTO_INCREMENT,
                      item_id bigint NOT NULL,
                      sale_price decimal(10,2) NOT NULL,
                      cost_price decimal(10,2) NOT NULL,
                      valid_from datetime(6) NOT NULL,
                      changed_by varchar(255) NULL,
                      reason varchar(255) NULL,
                      PRIMARY KEY (id),
                      KEY idx_price_history_item (item_id, valid_from)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
        }
        if (db.count("SELECT COUNT(*) FROM item_price_history") > 0) {
            return; // ya importado
        }
        boolean oldTables = db.tableExists("price_history") && db.tableExists("net_history");
        if (oldTables) {
            // Un cambio de ficha guardaba precio y coste con microsegundos de diferencia: se agrupan
            // por segundo para que cada edición sea una sola fila con ambos valores.
            int imported = db.execute("""
                    INSERT INTO item_price_history (item_id, sale_price, cost_price, valid_from, changed_by, reason)
                    SELECT d.item_id,
                      ROUND(COALESCE(
                        (SELECT ph.price FROM price_history ph WHERE ph.item_id = d.item_id
                           AND ph.date < d.at + INTERVAL 1 SECOND ORDER BY ph.date DESC LIMIT 1),
                        (SELECT ph.price FROM price_history ph WHERE ph.item_id = d.item_id ORDER BY ph.date LIMIT 1),
                        0), 2),
                      ROUND(COALESCE(
                        (SELECT nh.net FROM net_history nh WHERE nh.item_id = d.item_id
                           AND nh.date < d.at + INTERVAL 1 SECOND ORDER BY nh.date DESC LIMIT 1),
                        (SELECT nh.net FROM net_history nh WHERE nh.item_id = d.item_id ORDER BY nh.date LIMIT 1),
                        0), 2),
                      d.at, NULL, 'Importado del historial anterior'
                    FROM (
                      SELECT item_id, CAST(DATE_FORMAT(date, '%Y-%m-%d %H:%i:%s') AS DATETIME(6)) AS at FROM price_history
                      UNION
                      SELECT item_id, CAST(DATE_FORMAT(date, '%Y-%m-%d %H:%i:%s') AS DATETIME(6)) AS at FROM net_history
                    ) d
                    WHERE d.item_id IN (SELECT id FROM items)
                    ORDER BY d.item_id, d.at""");
            logger.info("V2: " + imported + " cambios de precio importados del historial anterior");
        }
        // Productos sin ningún historial: su precio actual es el punto de partida.
        db.execute("""
                INSERT INTO item_price_history (item_id, sale_price, cost_price, valid_from, changed_by, reason)
                SELECT i.id, i.sale_price, i.cost_price, NOW(6), NULL, 'Precio inicial al migrar'
                FROM items i
                WHERE NOT EXISTS (SELECT 1 FROM item_price_history h WHERE h.item_id = i.id)""");

        // Las tablas antiguas quedan como archivo; sin sus claves foráneas para poder borrar productos.
        if (oldTables) {
            for (String table : new String[] { "price_history", "net_history" }) {
                for (String fk : db.foreignKeys(table, "items")) {
                    db.execute("ALTER TABLE " + table + " DROP FOREIGN KEY `" + fk + "`");
                }
            }
        }
    }

    // ------------------------------------------------------------------ 4. movimientos de stock

    private void stockMovements(SchemaInspector db) throws Exception {
        if (!db.tableExists("stock_movements")) {
            db.execute("""
                    CREATE TABLE stock_movements (
                      id bigint NOT NULL AUTO_INCREMENT,
                      item_id bigint NOT NULL,
                      item_name varchar(255) NULL,
                      type varchar(30) NOT NULL,
                      quantity int NOT NULL,
                      stock_after int NOT NULL,
                      unit_cost decimal(10,2) NULL,
                      trade_id bigint NULL,
                      note varchar(255) NULL,
                      created_by varchar(255) NULL,
                      created_at datetime(6) NOT NULL,
                      PRIMARY KEY (id),
                      KEY idx_stock_mov_item (item_id, created_at),
                      KEY idx_stock_mov_date (created_at),
                      KEY idx_stock_mov_trade (trade_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
        }
        if (db.count("SELECT COUNT(*) FROM stock_movements") > 0) {
            return;
        }
        // Saldo inicial: a partir de aquí, la suma de movimientos de cada producto = su stock.
        db.execute("""
                INSERT INTO stock_movements (item_id, item_name, type, quantity, stock_after, unit_cost, note, created_at)
                SELECT i.id, i.name, 'OPENING_BALANCE', i.stock, i.stock, i.cost_price,
                       'Saldo inicial al activar el registro de movimientos', NOW(6)
                FROM items i
                WHERE i.stock <> 0""");
    }

    // ------------------------------------------------------------------ 5. líneas de venta

    private void tradeLines(SchemaInspector db) throws Exception {
        if (!db.columnExists("trade_lines", "unit_cost")) {
            db.execute("ALTER TABLE trade_lines ADD COLUMN unit_cost decimal(12,2) NULL");
        }
        if (!db.columnExists("trade_lines", "category_id")) {
            db.execute("ALTER TABLE trade_lines ADD COLUMN category_id bigint NULL");
        }
        if (!db.columnExists("trade_lines", "category_name")) {
            db.execute("ALTER TABLE trade_lines ADD COLUMN category_name varchar(255) NULL");
        }
        boolean history = db.tableExists("price_history") && db.tableExists("net_history");
        // Precio vigente en la fecha de la venta: el del historial en esa fecha; si la venta es
        // anterior a todo el historial, el de la columna antigua items.price (el que tenía el
        // sistema original entonces); si no, el primero del historial; y por último el actual.
        String legacyPrice = db.columnExists("items", "price") ? "NULLIF(i.price, 0)," : "";
        String priceAt = history ? """
                COALESCE(
                  (SELECT ph.price FROM price_history ph WHERE ph.item_id = g.item_id AND ph.date <= g.trade_date
                     ORDER BY ph.date DESC LIMIT 1),
                  %s
                  (SELECT ph.price FROM price_history ph WHERE ph.item_id = g.item_id ORDER BY ph.date LIMIT 1),
                  i.sale_price)""".formatted(legacyPrice) : "COALESCE(" + legacyPrice + " i.sale_price)";
        String costAt = history ? """
                NULLIF(COALESCE(
                  (SELECT nh.net FROM net_history nh WHERE nh.item_id = g.item_id AND nh.date <= g.trade_date
                     ORDER BY nh.date DESC LIMIT 1),
                  (SELECT nh.net FROM net_history nh WHERE nh.item_id = g.item_id ORDER BY nh.date LIMIT 1),
                  0), 0)""" : "NULLIF(i.cost_price, 0)";

        // 5a. Ventas antiguas sin líneas: se generan a partir de trades_items, con el precio y el
        // IGIC vigentes en la fecha de cada venta.
        if (db.tableExists("trades_items")) {
            db.execute("DROP TEMPORARY TABLE IF EXISTS tmp_legacy_lines");
            db.execute("""
                    CREATE TEMPORARY TABLE tmp_legacy_lines AS
                    SELECT g.trade_id, g.item_id, g.qty,
                           COALESCE(i.name, CONCAT('Producto ', g.item_id)) AS description,
                           ROUND(%s, 2) AS unit_price,
                           ROUND(%s, 2) AS unit_cost,
                           COALESCE(c.igic_rate, %s) AS rate,
                           c.id AS category_id, c.name AS category_name
                    FROM (
                      SELECT ti.trades_id AS trade_id, ti.items_id AS item_id, COUNT(*) AS qty, t.date AS trade_date
                      FROM trades_items ti JOIN trades t ON t.id = ti.trades_id
                      WHERE NOT EXISTS (SELECT 1 FROM trade_lines l WHERE l.trade_id = ti.trades_id)
                      GROUP BY ti.trades_id, ti.items_id, t.date
                    ) g
                    LEFT JOIN items i ON i.id = g.item_id
                    LEFT JOIN categories c ON c.id = i.category_id""".formatted(priceAt, costAt,
                    defaultIgicRate.toPlainString()));
            int created = db.execute("""
                    INSERT INTO trade_lines (trade_id, item_id, description, quantity, unit_price, tax_rate,
                                             line_total, tax_base, tax_amount, unit_cost, category_id, category_name)
                    SELECT x.trade_id, x.item_id, x.description, x.qty, x.unit_price, x.rate,
                           x.total, x.base, x.total - x.base, x.unit_cost, x.category_id, x.category_name
                    FROM (
                      SELECT l.*, ROUND(l.unit_price * l.qty, 2) AS total,
                             ROUND(ROUND(l.unit_price * l.qty, 2) / (1 + l.rate / 100), 2) AS base
                      FROM tmp_legacy_lines l
                    ) x
                    ORDER BY x.trade_id, x.item_id""");
            db.execute("DROP TEMPORARY TABLE IF EXISTS tmp_legacy_lines");
            logger.info("V2: " + created + " líneas generadas para ventas antiguas");
        }

        // 5b. Totales de las ventas antiguas a partir de sus líneas.
        db.execute("""
                UPDATE trades t JOIN (
                  SELECT trade_id, SUM(line_total) AS total, SUM(tax_base) AS base, SUM(tax_amount) AS tax
                  FROM trade_lines GROUP BY trade_id
                ) s ON s.trade_id = t.id
                SET t.total = s.total, t.total_base = s.base, t.total_tax = s.tax
                WHERE t.total IS NULL""");

        // 5c. Coste y categoría de las líneas ya existentes (ventas con líneas pero sin estos datos).
        db.execute("""
                UPDATE trade_lines l
                JOIN trades t ON t.id = l.trade_id
                LEFT JOIN items i ON i.id = l.item_id
                SET l.unit_cost = %s
                WHERE l.unit_cost IS NULL AND l.item_id IS NOT NULL""".formatted(
                costAt.replace("g.item_id", "l.item_id").replace("g.trade_date", "t.date")));
        db.execute("""
                UPDATE trade_lines l
                JOIN items i ON i.id = l.item_id
                LEFT JOIN categories c ON c.id = i.category_id
                SET l.category_id = c.id, l.category_name = c.name
                WHERE l.category_name IS NULL""");
    }

    // ------------------------------------------------------------------ 6. ids de categorías

    private void categoriesAutoIncrement(SchemaInspector db) throws Exception {
        if (db.columnIsAutoIncrement("categories", "id")) {
            return;
        }
        // NO_AUTO_VALUE_ON_ZERO: la categoría con id 0 (la primera, creada con MAX(id)+1 = 0) se
        // conserva tal cual; sin esto MySQL la renumeraría y rompería sus productos.
        db.execute("SET @old_sql_mode = @@SESSION.sql_mode");
        db.execute("SET SESSION sql_mode = CONCAT(@@SESSION.sql_mode, ',NO_AUTO_VALUE_ON_ZERO')");
        db.execute("SET @old_fk_checks = @@SESSION.foreign_key_checks");
        db.execute("SET SESSION foreign_key_checks = 0");
        try {
            db.execute("ALTER TABLE categories MODIFY id bigint NOT NULL AUTO_INCREMENT");
        } finally {
            db.execute("SET SESSION foreign_key_checks = @old_fk_checks");
            db.execute("SET SESSION sql_mode = @old_sql_mode");
        }
    }

    // ------------------------------------------------------------------ 7. índices para informes

    private void indexes(SchemaInspector db) throws Exception {
        if (!db.indexExists("trades", "idx_trades_status_date")) {
            db.execute("CREATE INDEX idx_trades_status_date ON trades (status, date)");
        }
        if (!db.indexExists("trade_lines", "idx_trade_lines_item")) {
            db.execute("CREATE INDEX idx_trade_lines_item ON trade_lines (item_id)");
        }
    }
}
