package es.jonay.kb.shopsystem.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.controller.ItemController;
import es.jonay.kb.shopsystem.model.entities.Item;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("stock")
@CrossOrigin
@Tag(name = "STOCK", description = "Stock management endpoints")
public class StockService {

    private final ItemController itemController;
    Logger logger = Logger.getLogger(StockService.class.getName());

    public StockService(ItemController itemController) {
        this.itemController = itemController;
    }

    @PostMapping("/")
    public ResponseEntity<String> addItem(@RequestBody Map<Long, Integer> itemsStock) {
        try {
            Map<Long, Integer> result = new HashMap<>();
            for (Map.Entry<Long, Integer> entry : itemsStock.entrySet()) {
                Long itemId = entry.getKey();
                Integer quantity = entry.getValue();
                Item item = itemController.addStock(itemId, quantity);
                result.put(itemId, item.getStock());
            }
            return ResponseEntity.ok("Items stock added successfully: " + result.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/")
    public void removeItem(HttpServletResponse response, @RequestBody Map<Long, Integer> itemsStock) {
        try {
            Map<Long, Integer> result = new HashMap<>();
            for (Map.Entry<Long, Integer> entry : itemsStock.entrySet()) {
                Long itemId = entry.getKey();
                Integer quantity = entry.getValue();
                Item item = itemController.removeStock(itemId, quantity);
                result.put(itemId, item.getStock());
            }
            // Suponiendo que tienes acceso a una lista de ItemDto con los datos necesarios
            List<ItemDto> items = new ArrayList<>();
            for (Long itemId : result.keySet()) {
                ItemDto itemDto = itemController.findById(itemId).get();
                items.add(itemDto);
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Devoluciones");

            // Encabezados
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Nombre");
            headerRow.createCell(1).setCellValue("PVP");
            headerRow.createCell(2).setCellValue("Neto");
            headerRow.createCell(3).setCellValue("Cantidad");
            headerRow.createCell(4).setCellValue("Total");

            int rowIndex = 1;
            double grandTotal = 0.0;

            for (ItemDto item : items) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.getName());
                row.createCell(1).setCellValue(item.getPrice());
                row.createCell(2).setCellValue(item.getNet());
                int cantidad = itemsStock.get(item.getId());
                row.createCell(3).setCellValue(cantidad);
                double totalPorItem = item.getPrice() * cantidad;
                row.createCell(4).setCellValue(totalPorItem);
                grandTotal += totalPorItem;
            }

            // Fila de total general
            Row totalRow = sheet.createRow(rowIndex);
            totalRow.createCell(3).setCellValue("TOTAL");
            totalRow.createCell(4).setCellValue(grandTotal);

            // Configurar respuesta HTTP para descargar el archivo Excel
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=devoluciones.xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();

        } catch (Exception e) {
            logger.severe("Error al generar el Excel: " + e.getMessage());
        }
    }

}
