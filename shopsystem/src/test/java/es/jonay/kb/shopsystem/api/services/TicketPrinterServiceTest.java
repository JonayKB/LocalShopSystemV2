package es.jonay.kb.shopsystem.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.jonay.kb.shopsystem.api.config.TicketPrinterConfigurationProperties;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.dto.TradeLineDto;
import es.jonay.kb.shopsystem.verifactu.VerifactuProperties;

/** Usa "impresoras" falsas: ServerSockets locales que guardan lo que reciben. */
class TicketPrinterServiceTest {

    /** ESC p 0: orden de abrir el cajón. */
    private static final String OPEN_DRAWER = "\u001Bp\u0000";

    private TicketPrinterService printer;
    private final List<FakePrinter> printers = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        TicketPrinterConfigurationProperties config = new TicketPrinterConfigurationProperties();
        config.setStreet("Calle Test 1");
        config.setBusinessName("Tienda Test");
        config.setNif("B12345674");
        printer = new TicketPrinterService(config, new VerifactuProperties());
    }

    @AfterEach
    void tearDown() throws IOException {
        for (FakePrinter p : printers) {
            p.close();
        }
    }

    @Test
    void imprimeElTicketYAbreElCajon() throws Exception {
        FakePrinter p = start();

        printer.print(trade("CAFE"), "127.0.0.1", p.port(), true);

        String out = p.nextJob();
        assertThat(out).contains("Tienda Test", "FACTURA SIMPLIFICADA", "T-000001", "CAFE", "Efectivo");
        assertThat(out).contains(OPEN_DRAWER);
    }

    @Test
    void sinCajonNoEnviaLaOrdenDeAbrirlo() throws Exception {
        FakePrinter p = start();

        printer.print(trade("CAFE"), "127.0.0.1", p.port(), false);

        assertThat(p.nextJob()).doesNotContain(OPEN_DRAWER);
    }

    @Test
    void variasCajasALaVezNoMezclanTicketsNiImpresoras() throws Exception {
        FakePrinter a = start();
        FakePrinter b = start();
        int perPrinter = 15;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> jobs = new ArrayList<>();
        for (int i = 0; i < perPrinter; i++) {
            jobs.add(pool.submit(job(go, "PRODUCTOA", a.port())));
            jobs.add(pool.submit(job(go, "PRODUCTOB", b.port())));
        }
        go.countDown();
        for (Future<?> f : jobs) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        for (int i = 0; i < perPrinter; i++) {
            String ja = a.nextJob();
            String jb = b.nextJob();
            // Cada conexión lleva exactamente un ticket completo y de su propia caja.
            assertThat(ja).contains("PRODUCTOA").doesNotContain("PRODUCTOB");
            assertThat(jb).contains("PRODUCTOB").doesNotContain("PRODUCTOA");
            assertThat(count(ja, "Gracias por su compra")).isEqualTo(1);
            assertThat(count(jb, "Gracias por su compra")).isEqualTo(1);
        }
    }

    @Test
    void siNoHayImpresoraFallaRapidoSinBloquear() throws Exception {
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        long start = System.nanoTime();
        assertThatThrownBy(() -> printer.openCashDrawer("127.0.0.1", closedPort)).isInstanceOf(IOException.class)
                .hasMessageContaining("127.0.0.1:" + closedPort);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
                .isLessThan(TicketPrinterService.TIMEOUT_MS + 1000L);

        // Un fallo no deja estado roto: la siguiente impresión funciona.
        FakePrinter p = start();
        printer.openCashDrawer("127.0.0.1", p.port());
        assertThat(p.nextJob()).contains(OPEN_DRAWER);
    }

    private Callable<Void> job(CountDownLatch go, String product, int port) {
        return () -> {
            go.await();
            printer.print(trade(product), "127.0.0.1", port, false);
            return null;
        };
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static TradeDto trade(String product) {
        TradeLineDto line = new TradeLineDto();
        line.setItemId(1L);
        line.setDescription(product);
        line.setQuantity(2);
        line.setUnitPrice(new BigDecimal("1.50"));
        line.setTaxRate(new BigDecimal("7.00"));
        line.setTaxBase(new BigDecimal("2.80"));
        line.setTaxAmount(new BigDecimal("0.20"));
        line.setLineTotal(new BigDecimal("3.00"));
        TradeDto t = new TradeDto();
        t.setId(1L);
        t.setDate(new Date());
        t.setInvoiceNumber("T-000001");
        t.setPaymentMethod("CASH");
        t.setLines(List.of(line));
        t.setTotal(new BigDecimal("3.00"));
        return t;
    }

    private FakePrinter start() throws IOException {
        FakePrinter p = new FakePrinter();
        printers.add(p);
        return p;
    }

    /** Acepta conexiones y guarda los bytes de cada una como un "trabajo" independiente. */
    static final class FakePrinter implements AutoCloseable {
        private final ServerSocket server;
        private final java.util.concurrent.BlockingQueue<String> jobs = new java.util.concurrent.LinkedBlockingQueue<>();

        FakePrinter() throws IOException {
            server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread t = new Thread(this::acceptLoop, "fake-printer-" + server.getLocalPort());
            t.setDaemon(true);
            t.start();
        }

        int port() {
            return server.getLocalPort();
        }

        String nextJob() throws InterruptedException {
            String job = jobs.poll(5, TimeUnit.SECONDS);
            assertThat(job).as("la impresora debería haber recibido un ticket").isNotNull();
            return job;
        }

        private void acceptLoop() {
            while (!server.isClosed()) {
                try {
                    Socket s = server.accept();
                    CompletableFuture.runAsync(() -> read(s));
                } catch (IOException e) {
                    return;
                }
            }
        }

        private void read(Socket s) {
            try (s; InputStream in = s.getInputStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                in.transferTo(buf);
                jobs.add(buf.toString(StandardCharsets.ISO_8859_1));
            } catch (IOException e) {
                jobs.add("ERROR " + e.getMessage());
            }
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}
