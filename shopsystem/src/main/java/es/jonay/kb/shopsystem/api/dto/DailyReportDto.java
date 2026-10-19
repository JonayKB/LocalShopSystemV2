package es.jonay.kb.shopsystem.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Resumen de lo vendido en un día (solo ventas emitidas, sin anuladas). */
public class DailyReportDto {

    /** Producto vendido: unidades e importe con IGIC incluido. */
    public static class Row {
        private final String name;
        private final int quantity;
        private final BigDecimal total;

        public Row(String name, int quantity, BigDecimal total) {
            this.name = name;
            this.quantity = quantity;
            this.total = total;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }

    /** Productos de una categoría con su subtotal. */
    public static class CategoryBlock {
        private final String name;
        private final List<Row> rows;
        private final int quantity;
        private final BigDecimal total;

        public CategoryBlock(String name, List<Row> rows) {
            this.name = name;
            this.rows = rows;
            this.quantity = rows.stream().mapToInt(Row::getQuantity).sum();
            this.total = rows.stream().map(Row::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public String getName() {
            return name;
        }

        public List<Row> getRows() {
            return rows;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }

    private LocalDate date;
    private int tradeCount;
    private BigDecimal total = BigDecimal.ZERO;
    private List<Row> products = new ArrayList<>();
    private List<CategoryBlock> categories = new ArrayList<>();

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public void setTradeCount(int tradeCount) {
        this.tradeCount = tradeCount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<Row> getProducts() {
        return products;
    }

    public void setProducts(List<Row> products) {
        this.products = products;
    }

    public List<CategoryBlock> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryBlock> categories) {
        this.categories = categories;
    }
}