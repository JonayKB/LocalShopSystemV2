package es.jonay.kb.shopsystem.api.services;

import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;

import es.jonay.kb.shopsystem.api.config.TicketPrinterConfigurationProperties;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;

@Controller
public class TicketPrinterService {

    private Socket socket;
    private OutputStream outputStream;
    private String street;
    private String nif;
    private String businessName;
    private static final byte[] INITIALIZE = new byte[] { 0x1B, 0x40 };
    private static final byte[] HEADER = new byte[] { 0x1B, 0x61, 0x01, 0x1B, 0x21, 0x08 }; // Centered, bold
    private static final byte[] CUT = new byte[] { 0x1D, 0x56, 0x00 }; // Full cut

    public TicketPrinterService(TicketPrinterConfigurationProperties config) throws Exception {
        socket = new Socket(config.getIp(), config.getPort());
        outputStream = socket.getOutputStream();
        this.street = config.getStreet();
        this.businessName = config.getBusinessName();
        this.nif = config.getNif();
        if (!socket.isConnected()) {
            throw new Exception("Could not connect to printer at " + config.getIp() + ":" + config.getPort());
        } 
    }

    public void print(TradeDto tradeDto) throws Exception {
        StringBuilder ticket = new StringBuilder();
        Map<Long, Integer> itemCountMap = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String fechaFormateada = sdf.format(tradeDto.getDate());
        for (ItemDto item : tradeDto.getItems()) {
            itemCountMap.put(item.getId(), itemCountMap.getOrDefault(item.getId(), 0) + 1);
        }

        // Encabezado centrado y en negrita
        // ticket.append(new String(HEADER));
        ticket.append(this.businessName + "\n");
        ticket.append(this.street + "\n");
        ticket.append("NIF: " + this.nif + "\n");
        ticket.append(fechaFormateada + "\n");

        // Volvemos a alineación izquierda y tamaño normal
        // ticket.append(new String(new byte[] { 0x1B, 0x61, 0x00, 0x1B, 0x21, 0x00 }));
        ticket.append("--------------------------------\n");

        // Productos (alineados)
        double subtotal = 0.0;
        for (ItemDto item : tradeDto.getItems()) {
            double precio = item.getPrice();
            int cantidad = itemCountMap.get(item.getId());
            double totalLinea = precio * cantidad;
            subtotal += totalLinea;

            // Producto (máximo 16 caracteres para dejar espacio a precio y cantidad)
            String nombre = item.getName();
            if (nombre.length() > 16) {
                nombre = nombre.substring(0, 16);
            }

            ticket.append(String.format("%-16s %6.2f€ x%-2d\n", nombre, precio, cantidad));
        }

        ticket.append("--------------------------------\n");
        ticket.append(String.format("Subtotal:       %.2f€\n", subtotal));
        ticket.append(String.format("TOTAL:          %.2f€\n", subtotal)); // si hay impuestos, podrías sumarlos aquí

        // Enviar a la impresora
        outputStream.write(INITIALIZE);
        outputStream.write(ticket.toString().getBytes());
        outputStream.write(CUT);
        System.out.println("Ticket printed:\n" + ticket.toString().getBytes());
        outputStream.flush();
    }

}
