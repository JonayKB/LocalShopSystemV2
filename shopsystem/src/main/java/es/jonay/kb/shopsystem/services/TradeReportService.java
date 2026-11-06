package es.jonay.kb.shopsystem.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.analytics.SaleLineRow;
import es.jonay.kb.shopsystem.model.repository.IGraphsRepository;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Informe mensual en Excel: ventas por día y categoría (con IGIC incluido), más una hoja con
 * el desglose de base, IGIC, coste y beneficio. Usa las líneas congeladas de cada venta, así
 * que cambiar precios o categorías después no altera meses pasados.
 */
@RestController
public class TradeReportService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IGraphsRepository graphs;
    private final ZoneId zone;

    public TradeReportService(IGraphsRepository graphs, @Value("${shop.timezone:Atlantic/Canary}") String zone) {
        this.graphs = graphs;
        this.zone = ZoneId.of(zone);
    }

    @GetMapping("/generate-report")
    @Transactional(readOnly = true)
    public void generateExcelReport(HttpServletResponse response,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) throws IOException {
        LocalDate today = LocalDate.now(zone);
        int m = month != null && month >= 1 && month <= 12 ? month : today.getMonthValue();
        int y = year != null && year >= 2000 && year <= 2100 ? year : today.getYear();
        LocalDate first = LocalDate.of(y, m, 1);
        LocalDate next = first.plusMonths(1);

        List<SaleLineRow> lines = graphs.saleLines(Date.from(first.atStartOfDay(zone).toInstant()),
                Date.from(next.atStartOfDay(zone).toInstant()));

        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Map<LocalDate, Map<String, BigDecimal>> byDay = new TreeMap<>();
        Map<LocalDate, BigDecimal[]> fiscal = new TreeMap<>(); // base, igic, total, coste, beneficio
        for (LocalDate d = first; d.isBefore(next); d = d.plusDays(1)) {
            byDay.put(d, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
            fiscal.put(d, zeros());
        }
        for (SaleLineRow l : lines) {
            LocalDate d = l.date().toInstant().atZone(zone).toLocalDate();
            String cat = l.categoryName() != null ? l.categoryName() : "Sin categoría";
            categories.add(cat);
            byDay.get(d).merge(cat, l.lineTotal(), BigDecimal::add);
            BigDecimal[] f = fiscal.get(d);
            f[0] = f[0].add(l.taxBase());
            f[1] = f[1].add(l.taxAmount());
            f[2] = f[2].add(l.lineTotal());
            if (l.unitCost() != null && l.unitCost().signum() > 0) {
                BigDecimal cost = l.unitCost().multiply(BigDecimal.valueOf(l.quantity()));
                f[3] = f[3].add(cost);
                f[4] = f[4].add(l.taxBase().subtract(cost));
            }
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            // Hoja 1: ventas por categoría (lo que ya usaba el panel)
            Sheet sheet = wb.createSheet("Ventas por categoría");
            List<String> cats = new ArrayList<>(categories);
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Fecha");
            for (int i = 0; i < cats.size(); i++) {
                h.createCell(i + 1).setCellValue(cats.get(i).toUpperCase());
            }
            h.createCell(cats.size() + 1).setCellValue("TOTAL");
            h.forEach(c -> c.setCellStyle(bold));
            int r = 1;
            BigDecimal[] colTotals = new BigDecimal[cats.size() + 1];
            java.util.Arrays.fill(colTotals, BigDecimal.ZERO);
            for (Map.Entry<LocalDate, Map<String, BigDecimal>> e : byDay.entrySet()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(e.getKey().format(DAY));
                BigDecimal dayTotal = BigDecimal.ZERO;
                for (int i = 0; i < cats.size(); i++) {
                    BigDecimal v = e.getValue().getOrDefault(cats.get(i), BigDecimal.ZERO);
                    cell(row, i + 1, v, money);
                    dayTotal = dayTotal.add(v);
                    colTotals[i] = colTotals[i].add(v);
                }
                cell(row, cats.size() + 1, dayTotal, money);
                colTotals[cats.size()] = colTotals[cats.size()].add(dayTotal);
            }
            Row total = sheet.createRow(r);
            total.createCell(0).setCellValue("TOTAL");
            total.getCell(0).setCellStyle(bold);
            for (int i = 0; i < colTotals.length; i++) {
                cell(total, i + 1, colTotals[i], money);
            }
            for (int i = 0; i <= cats.size() + 1; i++) {
                sheet.autoSizeColumn(i);
            }

            // Hoja 2: desglose fiscal y beneficio
            Sheet f = wb.createSheet("Base, IGIC y beneficio");
            String[] titles = { "Fecha", "Base imponible", "IGIC", "Total", "Coste", "Beneficio bruto" };
            Row fh = f.createRow(0);
            for (int i = 0; i < titles.length; i++) {
                fh.createCell(i).setCellValue(titles[i]);
                fh.getCell(i).setCellStyle(bold);
            }
            BigDecimal[] sum = zeros();
            int fr = 1;
            for (Map.Entry<LocalDate, BigDecimal[]> e : fiscal.entrySet()) {
                Row row = f.createRow(fr++);
                row.createCell(0).setCellValue(e.getKey().format(DAY));
                for (int i = 0; i < 5; i++) {
                    cell(row, i + 1, e.getValue()[i], money);
                    sum[i] = sum[i].add(e.getValue()[i]);
                }
            }
            Row ft = f.createRow(fr);
            ft.createCell(0).setCellValue("TOTAL");
            ft.getCell(0).setCellStyle(bold);
            for (int i = 0; i < 5; i++) {
                cell(ft, i + 1, sum[i], money);
            }
            for (int i = 0; i < titles.length; i++) {
                f.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=informe_" + y + "-" + String.format("%02d", m) + ".xlsx");
            wb.write(response.getOutputStream());
        }
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };
    }

    private static void cell(Row row, int col, BigDecimal value, CellStyle style) {
        row.createCell(col).setCellValue(value.doubleValue());
        row.getCell(col).setCellStyle(style);
    }
}
