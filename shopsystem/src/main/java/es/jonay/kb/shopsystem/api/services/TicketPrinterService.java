package es.jonay.kb.shopsystem.api.services;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.stereotype.Controller;

import es.jonay.kb.shopsystem.api.config.TicketPrinterConfigurationProperties;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.dto.TradeLineDto;
import es.jonay.kb.shopsystem.verifactu.VerifactuProperties;
import es.jonay.kb.shopsystem.verifactu.VerifactuQr;

@Controller
public class TicketPrinterService {
    Logger logger = Logger.getLogger(TicketPrinterService.class.getName());

    private String street;
    private String nif;
    private String businessName;
    private int qrModuleSize = 6;
    private Socket socket;
    private OutputStream outputStream;
    private static final int TOTAL_WIDTH = 48; // Ancho total del ticket en caracteres
    private static final byte[] INITIALIZE = new byte[] { 0x1B, 0x40 };
    private static final byte[] HEADER = new byte[] {
            0x1B, 0x61, 0x01, // ESC a 1 → Centrado
            0x1B, 0x45, 0x01 // ESC E 1 → Negrita (emphasized ON)
    };
    private static final byte[] OPEN_DRAWER = new byte[] { 0x1B, 0x70, 0x00, 0x19, (byte) 0xFA };
    private static final byte[] CUT = new byte[] { 0x1D, 0x56, 0x00 }; // Full cut

    public TicketPrinterService(TicketPrinterConfigurationProperties config, VerifactuProperties verifactu)
            throws Exception {

        this.street = config.getStreet();
        this.businessName = config.getBusinessName();
        this.nif = config.getNif();
        this.qrModuleSize = Math.max(1, Math.min(16, verifactu.getQr().getModuleSize()));
    }

    /**
     * QR tributario VERI*FACTU (ESC/POS, GS ( k): modelo 2, corrección de errores M, centrado, con el
     * texto "QR tributario:" encima y la leyenda de verificable debajo. Debe medir entre 30 y 40 mm:
     * ajusta verifactu.qr.module-size midiendo un ticket impreso.
     */
    private void writeQr(ByteArrayOutputStream buffer, String url) throws IOException {
        byte[] data = url.getBytes(StandardCharsets.US_ASCII);
        int len = data.length + 3;
        buffer.write(new byte[] { 0x1B, 0x61, 0x01, 0x1B, 0x45, 0x00 }); // centrado, sin negrita
        buffer.write((VerifactuQr.TEXTO_ENCIMA + "\n").getBytes("CP437"));
        buffer.write(new byte[] { 0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00 }); // modelo 2
        buffer.write(new byte[] { 0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (byte) qrModuleSize }); // tamaño módulo
        buffer.write(new byte[] { 0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31 }); // corrección nivel M
        buffer.write(new byte[] { 0x1D, 0x28, 0x6B, (byte) (len & 0xFF), (byte) ((len >> 8) & 0xFF), 0x31, 0x50,
                0x30 }); // guardar datos
        buffer.write(data);
        buffer.write(new byte[] { 0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30 }); // imprimir
        buffer.write(("\n" + VerifactuQr.TEXTO_DEBAJO + "\n\n").getBytes("CP437"));
    }

    public void connect(String ip, int port) throws IOException {
        if (socket == null || !socket.isConnected()) {
            try {
                // Timeout corto: si no hay impresora, la venta no debe quedarse esperando.
                Socket s = new Socket();
                s.connect(new java.net.InetSocketAddress(ip, port), 2000);
                socket = s;
                outputStream = socket.getOutputStream();
                logger.info("Connected to printer at " + ip + ":" + port);
            } catch (UnknownHostException e) {
                socket = null;
                outputStream = null;
                throw new IOException("Impresora no encontrada en " + ip + ":" + port, e);
            } catch (IOException e) {
                socket = null;
                outputStream = null;
                throw new IOException("No se pudo conectar con la impresora en " + ip + ":" + port, e);
            }
        }
    }

    public void close() throws IOException {
        if (socket != null && socket.isConnected()) {
            socket.close();
            outputStream = null;
            socket = null;
            logger.info("Printer connection closed.");
        }
    }

    public void print(TradeDto tradeDto, String ip, int port) throws Exception {
        connect(ip, port);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Inicializar impresora
        buffer.write(INITIALIZE);

        // El QR tributario va al principio de la factura, antes del resto del contenido.
        if (tradeDto.getVerifactuQrUrl() != null) {
            writeQr(buffer, tradeDto.getVerifactuQrUrl());
        }

        buffer.write(HEADER); // centrado + negrita

        buffer.write((businessName + "\n").getBytes("CP437"));
        buffer.write((street + "\n").getBytes("CP437"));
        buffer.write(("NIF : " + nif + "\n").getBytes("CP437"));
        buffer.write("FACTURA SIMPLIFICADA\n\n".getBytes("CP437"));

        // Alineación izquierda
        buffer.write(new byte[] { 0x1B, 0x61, 0x00, 0x1B, 0x45, 0x00 });

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String numero = tradeDto.getInvoiceNumber() != null ? tradeDto.getInvoiceNumber() : String.valueOf(tradeDto.getId());
        buffer.write(("Nro : " + numero + "\n").getBytes("CP437"));
        buffer.write(("Fecha : " + sdf.format(tradeDto.getDate()) + "\n\n").getBytes("CP437"));

        buffer.write("------------------------------------------------\n".getBytes("CP437"));
        buffer.write("DESCRIPCION                   PRECIO CANT  TOTAL\n".getBytes("CP437"));
        buffer.write("------------------------------------------------\n".getBytes("CP437"));

        // Productos: las ventas nuevas usan sus líneas congeladas (precio e IGIC al vender)
        if (tradeDto.getLines() != null && !tradeDto.getLines().isEmpty()) {
            for (TradeLineDto line : tradeDto.getLines()) {
                String nombre = line.getDescription();
                if (nombre.length() > 16)
                    nombre = nombre.substring(0, 16);

                String rightPart = String.format(java.util.Locale.US, "%6.2f x%-2d  %6.2f",
                        line.getUnitPrice(), line.getQuantity(), line.getLineTotal());
                int spaces = TOTAL_WIDTH - nombre.length() - rightPart.length();
                if (spaces < 1)
                    spaces = 1;
                buffer.write((nombre.toUpperCase() + " ".repeat(spaces) + rightPart + "\n")
                        .getBytes(StandardCharsets.US_ASCII));
            }

            // Desglose de IGIC por tipo
            Map<java.math.BigDecimal, java.math.BigDecimal[]> byRate = new java.util.TreeMap<>();
            for (TradeLineDto line : tradeDto.getLines()) {
                java.math.BigDecimal[] acc = byRate.computeIfAbsent(line.getTaxRate(),
                        k -> new java.math.BigDecimal[] { java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO });
                acc[0] = acc[0].add(line.getTaxBase());
                acc[1] = acc[1].add(line.getTaxAmount());
            }
            buffer.write("\n".getBytes("CP437"));
            for (Map.Entry<java.math.BigDecimal, java.math.BigDecimal[]> e : byRate.entrySet()) {
                buffer.write(String.format(java.util.Locale.US, "Base %5.2f%%: %9.2f   IGIC: %9.2f\n",
                        e.getKey(), e.getValue()[0], e.getValue()[1]).getBytes(StandardCharsets.US_ASCII));
            }
            buffer.write("\n".getBytes("CP437"));
            buffer.write(String.format(java.util.Locale.US, "TOTAL:        %33.2f\n", tradeDto.getTotal())
                    .getBytes(StandardCharsets.US_ASCII));
        } else {
            // Venta anterior a Verifactu: se imprime como antes
            Map<ItemDto, Integer> itemCountMap = new HashMap<>();
            for (ItemDto item : tradeDto.getItems())
                itemCountMap.put(item, itemCountMap.getOrDefault(item, 0) + 1);

            double subtotal = 0.0;
            for (Map.Entry<ItemDto, Integer> entry : itemCountMap.entrySet()) {
                ItemDto item = entry.getKey();
                int cantidad = entry.getValue();
                double precio = item.getPrice();
                double totalLinea = precio * cantidad;
                subtotal += totalLinea;

                String nombre = item.getName();
                if (nombre.length() > 16)
                    nombre = nombre.substring(0, 16);

                String rightPart = String.format("%6.2f x%-2d  %6.2f", precio, cantidad, totalLinea);
                int spaces = TOTAL_WIDTH - nombre.length() - rightPart.length();
                if (spaces < 1)
                    spaces = 1;

                buffer.write((nombre.toUpperCase() + " ".repeat(spaces) + rightPart + "\n")
                        .getBytes(StandardCharsets.US_ASCII));
            }

            buffer.write("\n".getBytes("CP437"));
            buffer.write(String.format("Subtotal:        %30.2f\n", subtotal).getBytes(StandardCharsets.US_ASCII));
            buffer.write(String.format("TOTAL:        %33.2f\n", subtotal).getBytes(StandardCharsets.US_ASCII));
        }

        buffer.write("\n\n".getBytes("CP437"));
        buffer.write(HEADER);
        buffer.write("Gracias por su compra!\n".getBytes("CP437"));
        buffer.write("\n\n\n\n".getBytes("CP437"));
        buffer.write(CUT);
        buffer.write(OPEN_DRAWER);

        // Enviar a la impresora
        outputStream.write(buffer.toByteArray());
        outputStream.flush();

        close();
    }

    public void openCashDrawer(String ip, int port) throws Exception {
        connect(ip, port);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Inicializar impresora
        buffer.write(INITIALIZE);
        buffer.write(OPEN_DRAWER);

        // Enviar a la impresora
        outputStream.write(buffer.toByteArray());
        outputStream.flush();

        close();

    }

}