package es.jonay.kb.shopsystem.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

/**
 * Migra a V1, mete datos "como los de producción" (historial de precios en mapas de DOUBLE,
 * ventas antiguas sin líneas, categoría con id 0...) y comprueba lo que hace V2.
 */
class V2MigrationTest {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0").withDatabaseName("mig")
            .withCommand("--default-time-zone=+00:00");
    private static final BigDecimal DEFAULT_IGIC = new BigDecimal("7.00");

    private Connection db;

    @BeforeAll
    static void start() {
        MYSQL.start();
    }

    @AfterAll
    static void stop() {
        MYSQL.stop();
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .javaMigrations(new V2__PricesStockAndAnalytics(DEFAULT_IGIC))
                .cleanDisabled(false)
                .target(target)
                .load();
    }

    @BeforeEach
    void setUp() throws Exception {
        flyway("latest").clean();
        flyway("1").migrate();
        db = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        legacyData();
    }

    /** Datos tal y como los dejaba la versión anterior. */
    private void legacyData() throws SQLException {
        exec("SET SESSION sql_mode = CONCAT(@@SESSION.sql_mode, ',NO_AUTO_VALUE_ON_ZERO')");
        exec("INSERT INTO categories (id, name, igic_rate) VALUES (0, 'Prensa', NULL), (1, 'Chuches', 3.00)");
        exec("""
                INSERT INTO items (id, name, category_id, stock, bare_minimun, ignore_stock) VALUES
                  (8410000000001, 'Periodico', 0, 12, 5, 0),
                  (8410000000002, 'Gominolas', 1, 0, 0, 0),
                  (8410000000003, 'Sin historial', 1, -2, 0, 1)""");
        // Periódico: 1,00 € (coste 0,60) y luego 1,20 € (coste 0,70). Precio y coste se guardaban
        // con microsegundos de diferencia en dos tablas.
        exec("""
                INSERT INTO price_history (item_id, date, price) VALUES
                  (8410000000001, '2025-01-10 10:00:00.100000', 1.0),
                  (8410000000001, '2025-03-01 09:00:00.100000', 1.2),
                  (8410000000002, '2025-02-01 12:00:00.000000', 0.5)""");
        exec("""
                INSERT INTO net_history (item_id, date, net) VALUES
                  (8410000000001, '2025-01-10 10:00:00.100050', 0.6),
                  (8410000000001, '2025-03-01 09:00:00.100050', 0.7),
                  (8410000000002, '2025-02-01 12:00:00.000040', 0.0)""");
        // Venta antigua del 15/02 (sin líneas): 2 periódicos y 3 gominolas, al precio de entonces.
        exec("INSERT INTO trades (id, date, status, payment_method) VALUES (100, '2025-02-15 18:30:00', 'ISSUED', 'CASH')");
        exec("""
                INSERT INTO trades_items (trades_id, items_id) VALUES
                  (100, 8410000000001), (100, 8410000000001),
                  (100, 8410000000002), (100, 8410000000002), (100, 8410000000002)""");
        // Venta nueva con líneas pero sin coste ni categoría (antes de V2).
        exec("""
                INSERT INTO trades (id, date, status, payment_method, series, number, total, total_base, total_tax)
                VALUES (101, '2025-03-05 11:00:00', 'ISSUED', 'CARD', 'T', 1, 1.20, 1.12, 0.08)""");
        exec("""
                INSERT INTO trade_lines (trade_id, item_id, description, quantity, unit_price, tax_rate,
                                         tax_base, tax_amount, line_total)
                VALUES (101, 8410000000001, 'Periodico', 1, 1.20, 7.00, 1.12, 0.08, 1.20)""");
    }

    private void migrateV2() {
        flyway("latest").migrate();
    }

    @Test
    void precioYCosteActualesPasanADecimal() throws SQLException {
        migrateV2();

        assertThat(one("SELECT sale_price FROM items WHERE id = 8410000000001")).isEqualTo("1.20");
        assertThat(one("SELECT cost_price FROM items WHERE id = 8410000000001")).isEqualTo("0.70");
        assertThat(one("SELECT sale_price FROM items WHERE id = 8410000000002")).isEqualTo("0.50");
        assertThat(one("SELECT sale_price FROM items WHERE id = 8410000000003")).isEqualTo("0.00");
    }

    @Test
    void elHistorialSeImportaUnaFilaPorEdicionConPrecioYCoste() throws SQLException {
        migrateV2();

        assertThat(rows("""
                SELECT CONCAT(DATE_FORMAT(valid_from, '%Y-%m-%d %H:%i:%s'), ' ', sale_price, ' ', cost_price)
                FROM item_price_history WHERE item_id = 8410000000001 ORDER BY valid_from""")).containsExactly(
                "2025-01-10 10:00:00 1.00 0.60",
                "2025-03-01 09:00:00 1.20 0.70");
        // Producto sin historial anterior: una fila con su precio actual.
        assertThat(one("SELECT COUNT(*) FROM item_price_history WHERE item_id = 8410000000003")).isEqualTo("1");
    }

    @Test
    void stockInicialEnElLibroDeMovimientos() throws SQLException {
        migrateV2();

        assertThat(rows("SELECT CONCAT(item_id, ' ', type, ' ', quantity, ' ', stock_after) FROM stock_movements ORDER BY item_id"))
                .containsExactly("8410000000001 OPENING_BALANCE 12 12", "8410000000003 OPENING_BALANCE -2 -2");
        // La suma de movimientos de cada producto es su stock.
        assertThat(one("""
                SELECT COUNT(*) FROM items i
                WHERE i.stock <> COALESCE((SELECT SUM(m.quantity) FROM stock_movements m WHERE m.item_id = i.id), 0)"""))
                .isEqualTo("0");
    }

    @Test
    void lasVentasAntiguasRecibenLineasConElPrecioEIgicDeSuFecha() throws SQLException {
        migrateV2();

        assertThat(rows("""
                SELECT CONCAT(description, ' x', quantity, ' ', unit_price, ' ', tax_rate, '% base ', tax_base,
                              ' igic ', tax_amount, ' total ', line_total, ' coste ', COALESCE(unit_cost, '-'),
                              ' ', category_name)
                FROM trade_lines WHERE trade_id = 100 ORDER BY item_id""")).containsExactly(
                // Precio del 15/02 = 1,00 (el 1,20 es de marzo). Prensa sin IGIC propio -> 7 %.
                "Periodico x2 1.00 7.00% base 1.87 igic 0.13 total 2.00 coste 0.60 Prensa",
                // Chuches al 3 %. Coste 0 = desconocido -> NULL.
                "Gominolas x3 0.50 3.00% base 1.46 igic 0.04 total 1.50 coste - Chuches");
        assertThat(one("SELECT CONCAT(total, ' ', total_base, ' ', total_tax) FROM trades WHERE id = 100"))
                .isEqualTo("3.50 3.33 0.17");
    }

    @Test
    void lasLineasExistentesRecibenCosteYCategoria() throws SQLException {
        migrateV2();

        assertThat(one("SELECT CONCAT(unit_cost, ' ', category_id, ' ', category_name) FROM trade_lines WHERE trade_id = 101"))
                .isEqualTo("0.70 0 Prensa");
        // Los importes de la venta ya emitida no se tocan.
        assertThat(one("SELECT CONCAT(total, ' ', total_base, ' ', total_tax) FROM trades WHERE id = 101"))
                .isEqualTo("1.20 1.12 0.08");
    }

    @Test
    void categoriasAutoincrementalesConservandoLaDeId0() throws SQLException {
        migrateV2();

        assertThat(one("SELECT name FROM categories WHERE id = 0")).isEqualTo("Prensa");
        assertThat(one("SELECT category_id FROM items WHERE id = 8410000000001")).isEqualTo("0");
        exec("INSERT INTO categories (name) VALUES ('Nueva')");
        assertThat(one("SELECT id FROM categories WHERE name = 'Nueva'")).isEqualTo("2");
    }

    @Test
    void sePuedenBorrarProductosConHistorialAntiguo() throws SQLException {
        migrateV2();

        exec("DELETE FROM items WHERE id = 8410000000003");
        assertThat(one("SELECT COUNT(*) FROM items WHERE id = 8410000000003")).isEqualTo("0");
    }

    @Test
    void columnaPriceDelVolcadoAntiguoNoBloqueaAltasYSirveDeRespaldo() throws SQLException {
        // Producción pudo crearse desde database.sql, con items.price DOUBLE NOT NULL.
        exec("ALTER TABLE items ADD COLUMN price double NOT NULL DEFAULT 0");
        exec("UPDATE items SET price = 2.5 WHERE id = 8410000000003");
        exec("ALTER TABLE items ALTER COLUMN price DROP DEFAULT");

        migrateV2();

        assertThat(one("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'items' AND column_name = 'price'")).isEqualTo("YES");
        assertThat(one("SELECT sale_price FROM items WHERE id = 8410000000003")).isEqualTo("2.50");
        exec("INSERT INTO items (id, name, sale_price, cost_price) VALUES (1, 'Alta nueva', 1, 0)");
    }

    @Test
    void ventaAnteriorATodoElHistorialUsaElPrecioDeLaColumnaAntigua() throws SQLException {
        // Sistema original: items.price = 0,80 para las gominolas. El historial nuevo empieza el 01/02
        // (0,50) pero hay una venta del 10/01, anterior: se vendió a 0,80.
        exec("ALTER TABLE items ADD COLUMN price double NULL");
        exec("UPDATE items SET price = 0.8 WHERE id = 8410000000002");
        exec("INSERT INTO trades (id, date, status, payment_method) VALUES (102, '2025-01-10 10:00:00', 'ISSUED', 'CASH')");
        exec("INSERT INTO trades_items (trades_id, items_id) VALUES (102, 8410000000002)");

        migrateV2();

        assertThat(one("SELECT unit_price FROM trade_lines WHERE trade_id = 102")).isEqualTo("0.80");
        // La venta del 15/02 (ya con historial) sigue usando el historial: 0,50.
        assertThat(one("SELECT unit_price FROM trade_lines WHERE trade_id = 100 AND item_id = 8410000000002"))
                .isEqualTo("0.50");
    }

    @Test
    void esIdempotente() throws Exception {
        migrateV2();
        long lines = Long.parseLong(one("SELECT COUNT(*) FROM trade_lines"));
        long history = Long.parseLong(one("SELECT COUNT(*) FROM item_price_history"));
        long moves = Long.parseLong(one("SELECT COUNT(*) FROM stock_movements"));

        // Volver a ejecutarla (p. ej. tras un fallo a medias y un "flyway repair") no duplica nada.
        new V2__PricesStockAndAnalytics(DEFAULT_IGIC).migrate(context());

        assertThat(Long.parseLong(one("SELECT COUNT(*) FROM trade_lines"))).isEqualTo(lines);
        assertThat(Long.parseLong(one("SELECT COUNT(*) FROM item_price_history"))).isEqualTo(history);
        assertThat(Long.parseLong(one("SELECT COUNT(*) FROM stock_movements"))).isEqualTo(moves);
    }

    @Test
    void enUnaBaseDeDatosVaciaTambienFunciona() throws SQLException {
        exec("SET FOREIGN_KEY_CHECKS = 0");
        for (String t : new String[] { "trades_items", "trade_lines", "trades", "price_history", "net_history",
                "items", "categories" }) {
            exec("DELETE FROM " + t);
        }
        exec("SET FOREIGN_KEY_CHECKS = 1");

        migrateV2();

        assertThat(one("SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1"))
                .isEqualTo("2");
    }

    // ------------------------------------------------------------------ utilidades

    private Context context() {
        return new Context() {
            @Override
            public Configuration getConfiguration() {
                return flyway("latest").getConfiguration();
            }

            @Override
            public Connection getConnection() {
                return db;
            }
        };
    }

    private void exec(String sql) throws SQLException {
        try (Statement st = db.createStatement()) {
            st.execute(sql);
        }
    }

    private String one(String sql) throws SQLException {
        List<String> r = rows(sql);
        assertThat(r).as(sql).hasSize(1);
        return r.get(0);
    }

    private List<String> rows(String sql) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }
}
