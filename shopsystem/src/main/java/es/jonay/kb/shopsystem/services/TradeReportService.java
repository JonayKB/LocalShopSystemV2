package es.jonay.kb.shopsystem.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.CategoryDto;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.controller.CategoryController;
import es.jonay.kb.shopsystem.controller.TradeController;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.time.format.DateTimeFormatter;

@RestController
@CrossOrigin
public class TradeReportService {
    TradeController tradeController;
    CategoryController categoryController;
    private static final Logger logger = LoggerFactory.getLogger(TradeReportService.class);

    public TradeController getTradeController() {
        return this.tradeController;
    }

    @Autowired
    public void setTradeController(TradeController tradeController) {
        this.tradeController = tradeController;
    }

    public CategoryController getCategoryController() {
        return this.categoryController;
    }

    @Autowired
    public void setCategoryController(CategoryController categoryController) {
        this.categoryController = categoryController;
    }

    @GetMapping("/generate-report")
    public void generateExcelReport(HttpServletResponse response,
            @RequestParam(value = "month", required = false) Integer month) throws IOException {
        // Obtener datos de la API o de la base de datos
        List<TradeDto> trades = fetchTradesFromDatabase(month);

        // Organizar datos por fecha (utilizando LocalDate en lugar de String para un
        // orden correcto)
        Map<LocalDate, Map<String, Double>> dataByDate = organizeDataByDate(trades);

        // Crear archivo Excel
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reporte de Trades");

        // Crear fila de encabezados
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Fecha");
        int columnIndex = 1;

        // Crear columnas de encabezado de categoría dinámicamente
        List<String> categoryNames = fetchCategoryNames();
        for (String category : categoryNames) {
            headerRow.createCell(columnIndex++).setCellValue(category.toUpperCase());
        }
        headerRow.createCell(columnIndex).setCellValue("Total");

        // Crear filas de datos
        int rowIndex = 1;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Map.Entry<LocalDate, Map<String, Double>> entry : dataByDate.entrySet()) {
            Row row = sheet.createRow(rowIndex++);

            // Convertir LocalDate a String usando un formato adecuado
            String formattedDate = entry.getKey().format(dateFormatter);
            row.createCell(0).setCellValue(formattedDate); // Fecha

            Map<String, Double> categoryData = entry.getValue();
            columnIndex = 1;
            double total = 0.0;

            for (String category : categoryNames) {
                double amount = categoryData.getOrDefault(category, 0.0);
                row.createCell(columnIndex++).setCellValue(amount);
                total += amount;
            }

            row.createCell(columnIndex).setCellValue(total);
        }

        // Configurar respuesta HTTP para descargar el archivo
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_trades.xlsx");

        // Escribir Excel en la respuesta HTTP
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private List<TradeDto> fetchTradesFromDatabase(Integer month) {
        LocalDate selectedDay;
        if (month == null) {
            selectedDay = LocalDate.now();
        } else if (month >= 1 && month <= 12) {
            // Usar el mes proporcionado y el año actual
            int currentYear = LocalDate.now().getYear();
            selectedDay = LocalDate.of(currentYear, month, 1);
        } else {
            selectedDay = LocalDate.now();
        }

        // Obtener el primer día del mes actual
        LocalDate firstDayOfMonth = selectedDay.withDayOfMonth(1);

        // Obtener el último día del mes actual
        LocalDate lastDayOfMonth = selectedDay.withDayOfMonth(selectedDay.lengthOfMonth());

        // Definir la hora de inicio y fin del día
        LocalTime startOfDay = LocalTime.MIN; // 00:00:00
        LocalTime endOfDay = LocalTime.MAX; // 23:59:59.999999999

        // Combinar LocalDate con LocalTime para el comienzo y final del mes
        ZonedDateTime startOfMonthZoned = ZonedDateTime.of(firstDayOfMonth, startOfDay, ZoneId.systemDefault());
        ZonedDateTime endOfMonthZoned = ZonedDateTime.of(lastDayOfMonth, endOfDay, ZoneId.systemDefault());

        LocalDateTime startOfMonth = startOfMonthZoned.toLocalDateTime();
        LocalDateTime endOfMonth = endOfMonthZoned.toLocalDateTime();

        return tradeController.findTradesInRange(startOfMonth, endOfMonth);
    }

    // Esto está mal, pk sería obtenerlo desde la base de datos así, no
    // transformarlo
    @GetMapping("/total-income-by-category")
    public Map<String, Double> getTotalIncomeByCategory(@RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        List<TradeDto> trades = tradeController.findTradesInRange(startDate, endDate);
        Map<String, Double> incomeByCategory = new HashMap<>();

        for (TradeDto trade : trades) {
            for (ItemDto item : trade.getItems()) {
                String categoryName = fetchCategoryNames().get(Integer.parseInt(item.getCategoryId().toString()));
                incomeByCategory.merge(categoryName, item.getPrice(), Double::sum);
            }
        }

        return incomeByCategory;
    }

    private Map<LocalDate, Map<String, Double>> organizeDataByDate(List<TradeDto> trades) {
        Map<LocalDate, Map<String, Double>> dataByDate = new TreeMap<>(); // TreeMap para ordenar por fecha
                                                                          // automáticamente

        for (TradeDto trade : trades) {
            // Convertir la fecha a LocalDate para asegurar el orden
            LocalDate date = trade.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            dataByDate.putIfAbsent(date, new HashMap<>());
            Map<String, Double> categoryData = dataByDate.get(date);

            for (ItemDto item : trade.getItems()) {
                categoryData.merge(fetchCategoryNames().get(Integer.parseInt(item.getCategoryId().toString())),
                        item.getPrice(), Double::sum);
            }
        }

        logger.info(dataByDate.toString());
        return dataByDate;
    }

    private List<String> fetchCategoryNames() {
        List<String> categoryNames = new ArrayList<>();
        for (CategoryDto categoryDto : categoryController.findAll()) {
            categoryNames.add(categoryDto.getName());
        }
        return categoryNames; // Ejemplo de categorías
    }
}
