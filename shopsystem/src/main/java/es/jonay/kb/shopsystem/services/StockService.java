package es.jonay.kb.shopsystem.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.StockMovementDto;
import es.jonay.kb.shopsystem.api.dto.StockMovementRequest;
import es.jonay.kb.shopsystem.controller.InventoryController;
import es.jonay.kb.shopsystem.model.entities.StockMovementType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("stock")
@Tag(name = "STOCK", description = "Movimientos de stock")
public class StockService {

    private final InventoryController inventory;

    public StockService(InventoryController inventory) {
        this.inventory = inventory;
    }

    /** Registra entradas, devoluciones, mermas o ajustes (todo o nada). */
    @PostMapping("/movements")
    public List<StockMovementDto> register(@RequestBody StockMovementRequest request) {
        return inventory.register(request);
    }

    /** Libro de movimientos, del más reciente al más antiguo, con filtros opcionales. */
    @GetMapping("/movements")
    public Page<StockMovementDto> movements(@RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return inventory.search(itemId, type, from, to, page, size);
    }

    /** Tipos que se pueden registrar a mano, con su etiqueta. */
    @GetMapping("/movement-types")
    public List<Map<String, Object>> types() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (StockMovementType t : StockMovementType.values()) {
            out.add(Map.of("type", t.name(), "label", t.label(), "sign", t.sign(), "manual", t.isManual()));
        }
        return out;
    }

    /** Compatibilidad: {itemId: cantidad} = entrada de mercancía. */
    @PostMapping("/")
    public ResponseEntity<List<StockMovementDto>> addItem(@RequestBody Map<Long, Integer> itemsStock) {
        return ResponseEntity.ok(inventory.register(toRequest(StockMovementType.RESTOCK, itemsStock, null)));
    }

    /**
     * Compatibilidad: {itemId: cantidad} = devolución al proveedor. Devuelve el albarán de
     * devolución en Excel (precios y costes vigentes en el momento).
     */
    @DeleteMapping("/")
    public void removeItem(HttpServletResponse response, @RequestBody Map<Long, Integer> itemsStock,
            @RequestParam(required = false) String note) throws IOException {
        List<StockMovementDto> done = inventory.register(toRequest(StockMovementType.SUPPLIER_RETURN, itemsStock,
                note == null ? "Devolución al proveedor" : note));
        writeReturnExcel(response, done);
    }

    private static StockMovementRequest toRequest(StockMovementType type, Map<Long, Integer> itemsStock, String note) {
        List<StockMovementRequest.Line> lines = new ArrayList<>();
        if (itemsStock != null) {
            itemsStock.forEach((id, qty) -> lines.add(new StockMovementRequest.Line(id, qty, null, null)));
        }
        return new StockMovementRequest(type.name(), note, lines);
    }

    private static void writeReturnExcel(HttpServletResponse response, List<StockMovementDto> moves)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Devoluciones");
            Row header = sheet.createRow(0);
            String[] titles = { "Código", "Nombre", "Coste unitario", "Cantidad", "Total coste", "Stock restante" };
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }
            int rowIndex = 1;
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (StockMovementDto m : moves) {
                int qty = -m.quantity();
                BigDecimal unit = m.unitCost() == null ? BigDecimal.ZERO : m.unitCost();
                BigDecimal total = unit.multiply(BigDecimal.valueOf(qty));
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(String.valueOf(m.itemId()));
                row.createCell(1).setCellValue(m.itemName());
                row.createCell(2).setCellValue(unit.doubleValue());
                row.createCell(3).setCellValue(qty);
                row.createCell(4).setCellValue(total.doubleValue());
                row.createCell(5).setCellValue(m.stockAfter());
                grandTotal = grandTotal.add(total);
            }
            Row totalRow = sheet.createRow(rowIndex);
            totalRow.createCell(3).setCellValue("TOTAL");
            totalRow.createCell(4).setCellValue(grandTotal.doubleValue());
            for (int i = 0; i < titles.length; i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=devoluciones.xlsx");
            workbook.write(response.getOutputStream());
        }
    }
}
