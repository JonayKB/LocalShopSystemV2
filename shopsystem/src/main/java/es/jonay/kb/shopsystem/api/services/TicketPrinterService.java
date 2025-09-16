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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Controller;

import es.jonay.kb.shopsystem.api.config.TicketPrinterConfigurationProperties;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;

@Controller
public class TicketPrinterService {
    Logger logger = Logger.getLogger(TicketPrinterService.class.getName());

    private Socket socket;
    private OutputStream outputStream;
    private String street;
    private String nif;
    private String businessName;
    private static final int TOTAL_WIDTH = 42; // Ancho total del ticket en caracteres
    private static final byte[] INITIALIZE = new byte[] { 0x1B, 0x40 };
    private static final byte[] HEADER = new byte[] {
            0x1B, 0x61, 0x01, // ESC a 1 → Centrado
            0x1B, 0x45, 0x01 // ESC E 1 → Negrita (emphasized ON)
    };
    private static final byte[] CUT = new byte[] { 0x1D, 0x56, 0x00 }; // Full cut

    public TicketPrinterService(TicketPrinterConfigurationProperties config) throws Exception {
        try {
            socket = new Socket(config.getIp(), config.getPort());
            outputStream = socket.getOutputStream();
            this.street = config.getStreet();
            this.businessName = config.getBusinessName();
            this.nif = config.getNif();
            if (!socket.isConnected()) {
                logger.severe("Could not connect to printer at " + config.getIp() + ":" + config.getPort());
            }
        } catch (UnknownHostException e) {
            logger.severe("Could not connect to printer at " + config.getIp() + ":" + config.getPort());
        } catch (IOException e) {
            logger.severe("Could not connect to printer at " + config.getIp() + ":" + config.getPort());
        }

    }

    public void print(TradeDto tradeDto) throws Exception {
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
        String totalValue = String.format("%.2f", subtotal); // si hay impuestos, podrías sumarlos aquí
        int totalSpaces = TOTAL_WIDTH - totalLabel.length() - totalValue.length();
        if (totalSpaces < 1)
            totalSpaces = 1;
        ticket.append(totalLabel + " ".repeat(totalSpaces) + totalValue + "\n");

        // Enviar a la impresora
        outputStream.write(INITIALIZE);
        outputStream.write(ticket.toString().getBytes("Cp858"));
        outputStream.write(CUT);
        outputStream.flush();
    }

    private void printLogo(OutputStream out) throws Exception {
        // Load image from resources (adjust path as needed)
        InputStream is = getClass().getResourceAsStream("/logo.png");
        if (is == null) {
            logger.warning("Logo image not found");
            return;
        }
        BufferedImage image = ImageIO.read(is);
        is.close();

        // Resize image width to printer width in pixels (e.g., 384 pixels for 48mm
        // printer at 8 dots/mm)
        // Adjust width according to your printer's pixel width
        int printerWidthPx = 384;
        int newHeight = (int) ((double) image.getHeight() * printerWidthPx / image.getWidth());
        Image scaled = image.getScaledInstance(printerWidthPx, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(printerWidthPx, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();

        // Convert to monochrome bitmap
        BufferedImage monoImage = new BufferedImage(printerWidthPx, newHeight, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2 = monoImage.createGraphics();
        g2.drawImage(resized, 0, 0, null);
        g2.dispose();

        // ESC/POS command: Set line spacing to 24 dots
        out.write(new byte[] { 0x1B, 0x33, 24 });

        // Print image in slices of 24 dots height
        for (int y = 0; y < newHeight; y += 24) {
            // Select bit image mode: ESC * m nL nH
            // m=33 for 24-dot double density
            out.write(0x1B);
            out.write('*');
            out.write(33);

            int widthBytes = printerWidthPx / 8;
            out.write(widthBytes & 0xFF); // nL
            out.write((widthBytes >> 8) & 0xFF); // nH

            for (int x = 0; x < printerWidthPx; x++) {
                byte[] slice = new byte[3];
                for (int sliceIndex = 0; sliceIndex < 3; sliceIndex++) {
                    byte b = 0;
                    for (int bit = 0; bit < 8; bit++) {
                        int yPos = y + sliceIndex * 8 + bit;
                        int pixel = 0;
                        if (yPos < newHeight) {
                            int color = monoImage.getRGB(x, yPos);
                            // In TYPE_BYTE_BINARY, black pixels have RGB=0xFF000000 (opaque black)
                            // White pixels are 0xFFFFFFFF
                            pixel = (color == 0xFF000000) ? 1 : 0;
                        }
                        b |= (pixel << (7 - bit));
                    }
                    slice[sliceIndex] = b;
                }
                out.write(slice);
            }
            out.write(0x0A); // line feed
        }

        // Reset line spacing to default (30)
        out.write(new byte[] { 0x1B, 0x33, 30 });
        out.flush();
    }

}
