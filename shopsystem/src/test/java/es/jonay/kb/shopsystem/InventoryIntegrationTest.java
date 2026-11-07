package es.jonay.kb.shopsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import es.jonay.kb.shopsystem.api.dto.CategoryDto;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.PriceChangeDto;
import es.jonay.kb.shopsystem.api.dto.StockMovementDto;
import es.jonay.kb.shopsystem.api.dto.StockMovementRequest;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.dto.TradeLineDto;
import es.jonay.kb.shopsystem.api.exceptions.ConflictException;
import es.jonay.kb.shopsystem.service.CategoryService;
import es.jonay.kb.shopsystem.service.InventoryService;
import es.jonay.kb.shopsystem.service.ItemService;
import es.jonay.kb.shopsystem.service.PricingService;
import es.jonay.kb.shopsystem.service.TradeService;
import es.jonay.kb.shopsystem.model.entities.PaymentMethod;

@SpringBootTest
@Import(TestDatabase.class)
class InventoryIntegrationTest {

    private static final long AGUA = 8410000000001L;
    private static final long PAN = 8410000000002L;

    @Autowired
    private ItemService items;
    @Autowired
    private CategoryService categories;
    @Autowired
    private InventoryService inventory;
    @Autowired
    private PricingService pricing;
    @Autowired
    private TradeService trades;
    @Autowired
    private JdbcTemplate jdbc;

    private Long bebidas;

    @BeforeEach
    void clean() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String t : new String[] { "stock_movements", "item_price_history", "trade_lines", "trades_items",
                "trades", "invoice_counters", "items", "categories" }) {
            jdbc.execute("DELETE FROM " + t);
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        CategoryDto c = new CategoryDto();
        c.setName("Bebidas");
        c.setIgicRate(new BigDecimal("7.00"));
        bebidas = categories.save(c).getId();
    }

    private ItemDto item(long id, String name, String price, String cost, int stock) {
        ItemDto d = new ItemDto(id, name, new BigDecimal(price), bebidas, stock, 2, false, new BigDecimal(cost));
        return items.save(d);
    }

    private static ItemDto ref(long id) {
        ItemDto d = new ItemDto();
        d.setId(id);
        return d;
    }

    private TradeDto sell(long... ids) {
        List<ItemDto> list = new ArrayList<>();
        for (long id : ids) {
            list.add(ref(id));
        }
        return trades.saveList(list, PaymentMethod.CASH);
    }

    private int stock(long id) {
        return items.findById(id).orElseThrow().getStock();
    }

    private long movementsSum(long id) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM stock_movements WHERE item_id = ?", Long.class, id);
    }

    // ------------------------------------------------------------------ alta y edición

    @Test
    void altaConStockInicialYPrimerPrecio() {
        item(AGUA, "Agua", "1.20", "0.50", 10);

        assertThat(stock(AGUA)).isEqualTo(10);
        List<StockMovementDto> m = inventory.forItem(AGUA, 0, 10).getContent();
        assertThat(m).extracting(StockMovementDto::type, StockMovementDto::quantity, StockMovementDto::stockAfter)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("OPENING_BALANCE", 10, 10));
        List<PriceChangeDto> h = pricing.historyOf(AGUA);
        assertThat(h).hasSize(1);
        assertThat(h.get(0).price()).isEqualByComparingTo("1.20");
        assertThat(h.get(0).cost()).isEqualByComparingTo("0.50");
        // Margen sobre base: 1,20 / 1,07 = 1,1215 -> (1,1215 - 0,50) / 1,1215 = 55,4 %
        assertThat(h.get(0).margin()).isEqualByComparingTo("55.4");
    }

    @Test
    void editarElPrecioDejaHistorialYNoLoDuplicaSiNoCambia() {
        item(AGUA, "Agua", "1.20", "0.50", 0);

        ItemDto edit = items.findById(AGUA).orElseThrow();
        edit.setPrice(new BigDecimal("1.35"));
        items.save(edit); // el panel usa POST para editar: debe conservar el historial
        items.save(edit); // mismo precio otra vez: sin fila nueva

        List<PriceChangeDto> h = pricing.historyOf(AGUA);
        assertThat(h).extracting(PriceChangeDto::price).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1.35"), new BigDecimal("1.20"));
        assertThat(h.get(0).reason()).isEqualTo("Edición de la ficha del producto");
    }

    @Test
    void editarElStockDesdeLaFichaEsUnAjusteYNoDeshaceVentasHechasMientras() {
        item(AGUA, "Agua", "1.00", "0.40", 10);
        ItemDto form = items.findById(AGUA).orElseThrow(); // el formulario se abre con stock 10

        sell(AGUA, AGUA); // mientras tanto se venden 2 -> 8

        form.setOriginalStock(10);
        form.setStock(15); // el usuario añade 5 a lo que veía
        form.setStockNote("Recuento de estantería");
        items.save(form);

        assertThat(stock(AGUA)).isEqualTo(13); // 8 + 5, no 15
        StockMovementDto last = inventory.forItem(AGUA, 0, 1).getContent().get(0);
        assertThat(last.type()).isEqualTo("ADJUSTMENT");
        assertThat(last.quantity()).isEqualTo(5);
        assertThat(last.note()).isEqualTo("Recuento de estantería");
        assertThat(movementsSum(AGUA)).isEqualTo(13);
    }

    @Test
    void validaLosDatosDelProducto() {
        assertThatThrownBy(() -> item(AGUA, " ", "1.00", "0", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> item(AGUA, "Agua", "-1", "0", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> item(AGUA, "Agua", "1", "-0.01", 0)).isInstanceOf(IllegalArgumentException.class);
        ItemDto badCat = new ItemDto(AGUA, "Agua", BigDecimal.ONE, 999999L, 0, 0, false, BigDecimal.ZERO);
        assertThatThrownBy(() -> items.save(badCat)).isInstanceOf(IllegalArgumentException.class);
        assertThat(items.findById(AGUA)).isEmpty();
    }

    // ------------------------------------------------------------------ ventas

    @Test
    void laVentaCongelaPrecioCosteYCategoriaYDescuentaStock() {
        item(AGUA, "Agua", "1.20", "0.50", 10);
        item(PAN, "Pan", "0.80", "0", 5);

        TradeDto t = sell(AGUA, PAN, AGUA);

        assertThat(t.getTotal()).isEqualByComparingTo("3.20");
        TradeLineDto agua = t.getLines().stream().filter(l -> l.getItemId() == AGUA).findFirst().orElseThrow();
        assertThat(agua.getQuantity()).isEqualTo(2);
        assertThat(agua.getUnitCost()).isEqualByComparingTo("0.50");
        assertThat(agua.getCategoryName()).isEqualTo("Bebidas");
        TradeLineDto pan = t.getLines().stream().filter(l -> l.getItemId() == PAN).findFirst().orElseThrow();
        assertThat(pan.getUnitCost()).isNull(); // coste 0 = desconocido
        assertThat(stock(AGUA)).isEqualTo(8);
        assertThat(stock(PAN)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trades_items", Long.class)).isZero();

        // Cambiar el precio después no altera la venta.
        ItemDto edit = items.findById(AGUA).orElseThrow();
        edit.setPrice(new BigDecimal("2.00"));
        items.save(edit);
        assertThat(trades.findById(t.getId()).orElseThrow().getTotal()).isEqualByComparingTo("3.20");
    }

    @Test
    void anularDevuelveElStockConSuMovimiento() {
        item(AGUA, "Agua", "1.00", "0.40", 10);
        TradeDto t = sell(AGUA, AGUA, AGUA);

        trades.cancel(t.getId(), "Cliente se arrepiente");
        trades.cancel(t.getId(), "otra vez"); // idempotente

        assertThat(stock(AGUA)).isEqualTo(10);
        assertThat(inventory.forItem(AGUA, 0, 10).getContent()).extracting(StockMovementDto::type)
                .containsExactly("SALE_CANCEL", "SALE", "OPENING_BALANCE");
        assertThat(movementsSum(AGUA)).isEqualTo(10);
    }

    @Test
    void ventasSimultaneasNoPierdenDescuentosDeStock() throws Exception {
        item(AGUA, "Agua", "1.00", "0.40", 100);
        int sales = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<TradeDto>> futures = new ArrayList<>();
        for (int i = 0; i < sales; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                return sell(AGUA, AGUA);
            }));
        }
        go.countDown();
        List<Long> numbers = new ArrayList<>();
        for (Future<TradeDto> f : futures) {
            numbers.add(f.get(60, TimeUnit.SECONDS).getNumber());
        }
        pool.shutdown();

        assertThat(stock(AGUA)).isEqualTo(100 - 2 * sales);
        assertThat(movementsSum(AGUA)).isEqualTo(100 - 2 * sales);
        // Numeración correlativa sin huecos ni repetidos.
        Collections.sort(numbers);
        for (int i = 0; i < sales; i++) {
            assertThat(numbers.get(i)).isEqualTo(i + 1L);
        }
    }

    // ------------------------------------------------------------------ movimientos manuales

    @Test
    void entradaDeMercanciaActualizaElCosteYDejaHistorial() {
        item(AGUA, "Agua", "1.20", "0.50", 4);

        inventory.register(new StockMovementRequest("RESTOCK", "Albarán 123",
                List.of(new StockMovementRequest.Line(AGUA, 24, null, new BigDecimal("0.45")))));

        assertThat(stock(AGUA)).isEqualTo(28);
        assertThat(items.findById(AGUA).orElseThrow().getNet()).isEqualByComparingTo("0.45");
        PriceChangeDto last = pricing.historyOf(AGUA).get(0);
        assertThat(last.cost()).isEqualByComparingTo("0.45");
        assertThat(last.reason()).isEqualTo("Coste de la entrada de mercancía");
        StockMovementDto m = inventory.forItem(AGUA, 0, 1).getContent().get(0);
        assertThat(m.note()).isEqualTo("Albarán 123");
        assertThat(m.unitCost()).isEqualByComparingTo("0.45");
    }

    @Test
    void mermaDevolucionYRecuento() {
        item(AGUA, "Agua", "1.00", "0.40", 20);

        inventory.register(new StockMovementRequest("LOSS", "Botellas rotas",
                List.of(new StockMovementRequest.Line(AGUA, 2, null, null))));
        inventory.register(new StockMovementRequest("SUPPLIER_RETURN", null,
                List.of(new StockMovementRequest.Line(AGUA, 3, null, null))));
        inventory.register(new StockMovementRequest("ADJUSTMENT", "Recuento anual",
                List.of(new StockMovementRequest.Line(AGUA, null, 14, null))));

        assertThat(stock(AGUA)).isEqualTo(14);
        assertThat(inventory.forItem(AGUA, 0, 10).getContent())
                .extracting(StockMovementDto::type, StockMovementDto::quantity, StockMovementDto::stockAfter)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("ADJUSTMENT", -1, 14),
                        org.assertj.core.groups.Tuple.tuple("SUPPLIER_RETURN", -3, 15),
                        org.assertj.core.groups.Tuple.tuple("LOSS", -2, 18),
                        org.assertj.core.groups.Tuple.tuple("OPENING_BALANCE", 20, 20));
    }

    @Test
    void losMovimientosSonTodoONadaYSeValidan() {
        item(AGUA, "Agua", "1.00", "0.40", 5);

        // La segunda línea es de un producto que no existe: no se aplica ninguna.
        assertThatThrownBy(() -> inventory.register(new StockMovementRequest("RESTOCK", null,
                List.of(new StockMovementRequest.Line(AGUA, 10, null, null),
                        new StockMovementRequest.Line(999L, 1, null, null)))))
                .isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(stock(AGUA)).isEqualTo(5);

        assertThatThrownBy(() -> inventory.register(new StockMovementRequest("SALE", null,
                List.of(new StockMovementRequest.Line(AGUA, 1, null, null))))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inventory.register(new StockMovementRequest("RESTOCK", null,
                List.of(new StockMovementRequest.Line(AGUA, 0, null, null))))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inventory.register(new StockMovementRequest("INVENTADO", null,
                List.of(new StockMovementRequest.Line(AGUA, 1, null, null))))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inventory.register(new StockMovementRequest("RESTOCK", null, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------ categorías y borrado

    @Test
    void categoriasConIdGeneradoYBorradoProtegido() {
        CategoryDto c = new CategoryDto();
        c.setName("Prensa");
        Long id = categories.save(c).getId();
        assertThat(id).isNotNull().isNotEqualTo(bebidas);

        item(AGUA, "Agua", "1.00", "0", 0);
        assertThatThrownBy(() -> categories.deleteById(bebidas)).isInstanceOf(ConflictException.class);
        categories.deleteById(id);
        assertThat(categories.findById(id)).isEmpty();
    }

    @Test
    void borrarUnProductoConHistorial() {
        item(AGUA, "Agua", "1.00", "0.40", 3);
        sell(AGUA);

        items.deleteById(AGUA);

        assertThat(items.findById(AGUA)).isEmpty();
        // Las ventas y el libro de movimientos se conservan.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trade_lines WHERE item_id = ?", Long.class, AGUA)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE item_id = ?", Long.class, AGUA)).isEqualTo(2);
    }
}
