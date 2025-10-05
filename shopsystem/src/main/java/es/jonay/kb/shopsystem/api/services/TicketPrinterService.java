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
        if (outputStream == null) {
            throw new IllegalStateException("Printer is not connected");
        }
        StringBuilder ticket = new StringBuilder();
        Map<ItemDto, Integer> itemCountMap = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String fechaFormateada = sdf.format(tradeDto.getDate());
        for (ItemDto item : tradeDto.getItems()) {
            itemCountMap.put(item, itemCountMap.getOrDefault(item, 0) + 1);
        }

        // Encabezado centrado y en negrita
        // printLogo(outputStream);
        // ticket.append("\n\n\n\n");
        ticket.append(HEADER);
        ticket.append(this.businessName + "\n");
        ticket.append(this.street + "\n");
        ticket.append("NIF : " + this.nif + "\n");
        ticket.append("TICKET");
        ticket.append("\n\n\n\n\n\n");
        ticket.append(new String(new byte[] {
                0x1B, 0x61, 0x00, // ESC a 0 → Alineación izquierda
                0x1B, 0x45, 0x00 // ESC E 0 → Negrita OFF
        }));
        ticket.append("Nro : " + tradeDto.getId() + "\n");
        ticket.append("Fecha : " + fechaFormateada + "\n");

        ticket.append("\n\n\n");

        ticket.append("------------------------------------------\n");
        ticket.append("DESCRIPCION             PRECIO CANT  TOTAL\n");
        ticket.append("------------------------------------------\n");

        // Productos (alineados)
        double subtotal = 0.0;
        for (Map.Entry<ItemDto, Integer> entry : itemCountMap.entrySet()) {
            ItemDto item = entry.getKey();
            int cantidad = entry.getValue();
            double precio = item.getPrice();
            double totalLinea = precio * cantidad;
            subtotal += totalLinea;

            // Producto (máximo 16 caracteres para dejar espacio a precio y cantidad)
            String nombre = item.getName();
            if (nombre.length() > 16) {
                nombre = nombre.substring(0, 16);
            }

            String rightPart = String.format("%6.2f x%-2d  %6.2f", precio, cantidad, precio * cantidad);

            // Calcula espacios necesarios para empujar rightPart a la derecha
            int spaces = TOTAL_WIDTH - nombre.length() - rightPart.length();
            if (spaces < 1)
                spaces = 1; // evitar valores negativos

            String linea = nombre.toUpperCase() + " ".repeat(spaces) + rightPart + "\n";
            ticket.append(linea);
        }

        ticket.append("\n\n\n");
        // Subtotal
        String subtotalLabel = "Subtotal:";
        String subtotalValue = String.format("%.2f", subtotal);
        int subtotalSpaces = TOTAL_WIDTH - subtotalLabel.length() - subtotalValue.length();
        if (subtotalSpaces < 1)
            subtotalSpaces = 1;
        ticket.append(subtotalLabel + " ".repeat(subtotalSpaces) + subtotalValue + "\n");

        // Total
        String totalLabel = "TOTAL:";
        String totalValue = String.format("%.2f", subtotal);
        int totalSpaces = TOTAL_WIDTH - totalLabel.length() - totalValue.length();
        if (totalSpaces < 1)
            totalSpaces = 1;
        ticket.append(totalLabel + " ".repeat(totalSpaces) + totalValue + "\n");

        ticket.append("\n\n\n");
        ticket.append(HEADER);
        ticket.append("Gracias por su compra!\n");
        ticket.append("\n\n\n\n\n\n\n\n\n\n\n\n");
        ticket.append(new String(new byte[] {
                0x1B, 0x61, 0x00, // ESC a 0 → Alineación izquierda
                0x1B, 0x45, 0x00 // ESC E 0 → Negrita OFF
        }));

        outputStream.write(INITIALIZE);
        outputStream.write(ticket.toString().getBytes(StandardCharsets.US_ASCII));
        outputStream.write(CUT);
        outputStream.flush();

        close();
    }

}
