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

@Controller
public class TicketPrinterService {
    Logger logger = Logger.getLogger(TicketPrinterService.class.getName());

    private String street;
    private String nif;
    private String businessName;
    private Socket socket;
    private OutputStream outputStream;
    private static final int TOTAL_WIDTH = 48; // Ancho total del ticket en caracteres
    private static final byte[] INITIALIZE = new byte[] { 0x1B, 0x40 };
    private static final byte[] HEADER = new byte[] {
            0x1B, 0x61, 0x01, // ESC a 1 → Centrado
            0x1B, 0x45, 0x01 // ESC E 1 → Negrita (emphasized ON)
    };
    private static final byte[] CUT = new byte[] { 0x1D, 0x56, 0x00 }; // Full cut

    public TicketPrinterService(TicketPrinterConfigurationProperties config) throws Exception {

        this.street = config.getStreet();
        this.businessName = config.getBusinessName();
        this.nif = config.getNif();

        this.street = config.getStreet();
        this.businessName = config.getBusinessName();
        this.nif = config.getNif();

    }

    public void connect(String ip, int port) throws IOException {
        if (socket == null || !socket.isConnected()) {
            try {
                socket = new Socket(ip, port);
                outputStream = socket.getOutputStream();
                logger.info("Connected to printer at " + ip + ":" + port);
                if (!socket.isConnected()) {
                    logger.severe("Could not connect to printer at " + ip + ":" + port);
                }
            } catch (UnknownHostException e) {
                logger.severe("Could not connect to printer at " + ip + ":" + port);
            } catch (IOException e) {
                logger.severe("Could not connect to printer at " + ip + ":" + port);
            }
        }
    }

    public void close() throws IOException {
        if (socket != null && socket.isConnected()) {
            socket.close();
            outputStream = null;
            logger.info("Printer connection closed.");
        }
    }

    public void print(TradeDto tradeDto, String ip, int port) throws Exception {
    connect(ip, port);
    if (outputStream == null)
        throw new IllegalStateException("Printer is not connected");

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    // Inicializar impresora
    buffer.write(INITIALIZE);
    buffer.write(HEADER); // centrado + negrita

    buffer.write((businessName + "\n").getBytes("CP437"));
    buffer.write((street + "\n").getBytes("CP437"));
    buffer.write(("NIF : " + nif + "\n").getBytes("CP437"));
    buffer.write("TICKET\n\n".getBytes("CP437"));

    // Alineación izquierda
    buffer.write(new byte[]{0x1B, 0x61, 0x00, 0x1B, 0x45, 0x00});

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    buffer.write(("Nro : " + tradeDto.getId() + "\n").getBytes("CP437"));
    buffer.write(("Fecha : " + sdf.format(tradeDto.getDate()) + "\n\n").getBytes("CP437"));

    buffer.write("------------------------------------------------\n".getBytes("CP437"));
    buffer.write("DESCRIPCION                   PRECIO CANT  TOTAL\n".getBytes("CP437"));
    buffer.write("------------------------------------------------\n".getBytes("CP437"));

    // Productos
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
        if (nombre.length() > 16) nombre = nombre.substring(0, 16);

        String rightPart = String.format("%6.2f x%-2d  %6.2f", precio, cantidad, totalLinea);
        int spaces = TOTAL_WIDTH - nombre.length() - rightPart.length();
        if (spaces < 1) spaces = 1;

        buffer.write((nombre.toUpperCase() + " ".repeat(spaces) + rightPart + "\n").getBytes(StandardCharsets.US_ASCII));
    }

    buffer.write("\n".getBytes("CP437"));
    buffer.write(String.format("Subtotal:        %30.2f\n", subtotal).getBytes(StandardCharsets.US_ASCII));
    buffer.write(String.format("TOTAL:        %33.2f\n", subtotal).getBytes(StandardCharsets.US_ASCII));

    buffer.write("\n\n".getBytes("CP437"));
    buffer.write(HEADER);
    buffer.write("Gracias por su compra!\n".getBytes("CP437"));
    buffer.write("\n\n\n\n".getBytes("CP437"));
    buffer.write(CUT);

    // Enviar a la impresora
    outputStream.write(buffer.toByteArray());
    outputStream.flush();

    close();
}


}
