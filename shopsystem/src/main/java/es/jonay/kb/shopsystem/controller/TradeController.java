package es.jonay.kb.shopsystem.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SortedMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.api.dto.DailyReportDto;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.mappers.TradeMapper;
import es.jonay.kb.shopsystem.model.entities.BillingRecord;
import es.jonay.kb.shopsystem.model.entities.BillingRecordType;
import es.jonay.kb.shopsystem.model.entities.Category;
import es.jonay.kb.shopsystem.model.entities.InvoiceCounter;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.Trade;
import es.jonay.kb.shopsystem.model.entities.TradeLine;
import es.jonay.kb.shopsystem.model.entities.TradeStatus;
import es.jonay.kb.shopsystem.model.repository.IBillingRecordRepository;
import es.jonay.kb.shopsystem.model.repository.ICategoryRepository;
import es.jonay.kb.shopsystem.model.repository.IInvoiceCounterRepository;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;
import es.jonay.kb.shopsystem.model.repository.ITradeRepository;
import es.jonay.kb.shopsystem.verifactu.BillingRecordService;
import es.jonay.kb.shopsystem.verifactu.VerifactuProperties;
import es.jonay.kb.shopsystem.verifactu.VerifactuQr;

@Controller
public class TradeController {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private ITradeRepository tradeRepository;
    private ICategoryRepository categoryRepository;
    private IInvoiceCounterRepository counterRepository;
    private ItemController itemController;
    private IItemRepository itemRepository;
    private TradeMapper tradeMapper;
    private BillingRecordService billingRecordService;
    private IBillingRecordRepository billingRecordRepository;
    private VerifactuProperties verifactuProperties;

    /** Serie de las facturas simplificadas (tickets). */
    @Value("${shop.invoice.series:T}")
    private String invoiceSeries;

    /**
     * Tipo de IGIC (%) por defecto si la categoría no tiene uno propio.
     * Confirma los tipos reales de cada categoría con tu gestoría.
     */
    @Value("${shop.tax.default-igic-rate:7.00}")
    private BigDecimal defaultIgicRate;

    @Autowired
    public void setBillingRecordService(BillingRecordService billingRecordService) {
        this.billingRecordService = billingRecordService;
    }

    @Autowired
    public void setBillingRecordRepository(IBillingRecordRepository billingRecordRepository) {
        this.billingRecordRepository = billingRecordRepository;
    }

    @Autowired
    public void setVerifactuProperties(VerifactuProperties verifactuProperties) {
        this.verifactuProperties = verifactuProperties;
    }

    @Autowired
    public void setTradeMapper(TradeMapper tradeMapper) {
        this.tradeMapper = tradeMapper;
    }

    @Autowired
    public void setIICategoryRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public ICategoryRepository getICategoryRepository() {
        return this.categoryRepository;
    }

    public ITradeRepository getITradeRepository() {
        return this.tradeRepository;
    }

    @Autowired
    public void setITradeRepository(ITradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Autowired
    public void setInvoiceCounterRepository(IInvoiceCounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Autowired
    public void setItemController(ItemController itemController) {
        this.itemController = itemController;
    }

    @Autowired
    public void setIItemRepository(IItemRepository iItemRepository) {
        this.itemRepository = iItemRepository;
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findAll() {
        List<Trade> trades = tradeRepository.findAll();
        List<TradeDto> result = new ArrayList<>();
        for (Trade trade : trades) {
            result.add(tradeMapper.toTradeDto(trade));
        }
        enrich(result);
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<TradeDto> findById(Long id) {
        Optional<Trade> trade = tradeRepository.findById(id);
        Optional<TradeDto> dto = trade.map(tradeMapper::toTradeDto);
        dto.ifPresent(d -> enrich(List.of(d)));
        return dto;
    }

    /**
     * Emite una venta nueva. Siempre pasa por {@link #saveList(List)}: la fecha,
     * el número y los importes los fija el servidor, nunca el cliente.
     */
    @Transactional
    public TradeDto save(TradeDto tradeDto) {
        return saveList(tradeDto.getItems());
    }

    /**
     * Emite una factura simplificada: agrupa los productos en líneas, congela
     * precio e IGIC, descuenta stock y asigna el siguiente número de la serie.
     * Todo en una única transacción: si algo falla, no se consume número.
     */
    @Transactional
    public TradeDto saveList(List<ItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La venta no tiene productos");
        }

        // Agrupa por producto manteniendo el orden de entrada.
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (ItemDto itemDto : items) {
            quantities.merge(itemDto.getId(), 1, Integer::sum);
        }

        InvoiceCounter counter = counterRepository.findBySeriesForUpdate(invoiceSeries)
                .orElseGet(() -> {
                    counterRepository.saveAndFlush(new InvoiceCounter(invoiceSeries));
                    return counterRepository.findBySeriesForUpdate(invoiceSeries).orElseThrow();
                });

        Trade trade = new Trade();
        List<Item> itemList = new ArrayList<>();
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Item item = itemRepository.findById(entry.getKey())
                    .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + entry.getKey()));
            int quantity = entry.getValue();

            BigDecimal unitPrice = latestPrice(item);
            BigDecimal rate = igicRateOf(item.getCategory());

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal divisor = BigDecimal.ONE.add(rate.divide(HUNDRED, 6, RoundingMode.HALF_UP));
            BigDecimal base = lineTotal.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal tax = lineTotal.subtract(base);

            TradeLine line = new TradeLine();
            line.setItemId(item.getId());
            line.setDescription(item.getName());
            line.setQuantity(quantity);
            line.setUnitPrice(unitPrice);
            line.setTaxRate(rate);
            line.setTaxBase(base);
            line.setTaxAmount(tax);
            line.setLineTotal(lineTotal);
            trade.addLine(line);

            totalBase = totalBase.add(base);
            totalTax = totalTax.add(tax);
            total = total.add(lineTotal);

            for (int i = 0; i < quantity; i++) {
                itemList.add(item);
                itemController.removeStock(item.getId(), 1);
            }
        }

        trade.setItems(itemList);
        trade.setDate(new Date());
        trade.setSeries(invoiceSeries);
        trade.setNumber(counter.next());
        trade.setStatus(TradeStatus.ISSUED);
        trade.setTotalBase(totalBase);
        trade.setTotalTax(totalTax);
        trade.setTotal(total);

        counterRepository.save(counter);
        Trade saved = tradeRepository.save(trade);
        // Registro de alta VERI*FACTU en la MISMA transacción: si falla, la venta no se emite
        // y no se consume número. (No hace nada si verifactu.enabled=false.)
        billingRecordService.createAlta(saved);
        TradeDto dto = tradeMapper.toTradeDto(saved);
        enrich(List.of(dto));
        return dto;
    }

    /**
     * Anula una venta (no la borra): conserva el registro, marca el estado y
     * devuelve el stock y genera el registro de anulación de VERI*FACTU (misma
     * transacción).
     */
    @Transactional
    public TradeDto cancel(Long id, String reason) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Venta no encontrada: " + id));
        if (trade.getStatus() == TradeStatus.CANCELLED) {
            TradeDto already = tradeMapper.toTradeDto(trade);
            enrich(List.of(already));
            return already;
        }
        trade.setStatus(TradeStatus.CANCELLED);
        trade.setCancelledAt(new Date());
        trade.setCancellationReason(reason == null || reason.isBlank() ? null : reason.trim());
        for (Item item : trade.getItems()) {
            itemController.addStock(item.getId(), 1);
        }
        Trade saved = tradeRepository.save(trade);
        billingRecordService.createAnulacion(saved);
        TradeDto dto = tradeMapper.toTradeDto(saved);
        enrich(List.of(dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findTradesInRange(LocalDateTime startDate, LocalDateTime endDate) {

        // Ensure that the time range is correctly defined
        if (startDate == null) {
            startDate = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIDNIGHT);
        }

        if (endDate == null) {
            endDate = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MAX);
        }
        List<TradeDto> tradesDtoList = new ArrayList<>();
        // Solo ventas emitidas: las anuladas no cuentan en caja ni en informes.
        for (Trade trade : tradeRepository.findAllTradesInRange(startDate, endDate)) {
            tradesDtoList.add(tradeMapper.toTradeDto(trade));
        }
        enrich(tradesDtoList);
        return tradesDtoList;
    }

    /** Incluye también las anuladas, para que se vean (marcadas) en el panel. */
    @Transactional(readOnly = true)
    public Page<TradeDto> getPage(int page, int size) {
        Page<Trade> trades = tradeRepository.findAll(PageRequest.of(page, size).withSort(Sort.by("date").descending()));
        Page<TradeDto> dtos = trades.map(tradeMapper::toTradeDto);
        enrich(dtos.getContent());
        return dtos;
    }

    /**
     * Resumen de lo vendido en un día: todos los productos con unidades e importe, y
     * los mismos agrupados por categoría. Solo ventas emitidas (las anuladas no cuentan).
     */
    @Transactional(readOnly = true)
    public DailyReportDto dailyReport(LocalDate day) {
        LocalDate d = day != null ? day : LocalDate.now();
        List<Trade> trades = tradeRepository.findAllTradesInRange(d.atStartOfDay(), d.plusDays(1).atStartOfDay());

        // clave = id de producto; guarda nombre, unidades, importe y categoría
        Map<Long, Object[]> byItem = new LinkedHashMap<>();
        for (Trade trade : trades) {
            if (trade.getLines() != null && !trade.getLines().isEmpty()) {
                for (TradeLine l : trade.getLines()) {
                    addToReport(byItem, l.getItemId(), l.getDescription(), l.getQuantity(), l.getLineTotal());
                }
            } else {
                // Venta anterior a las líneas congeladas: se cuenta por producto al precio actual.
                for (Item item : trade.getItems()) {
                    addToReport(byItem, item.getId(), item.getName(), 1, latestPrice(item));
                }
            }
        }

        Map<Long, String> categoryOf = new HashMap<>();
        List<DailyReportDto.Row> products = new ArrayList<>();
        Map<String, List<DailyReportDto.Row>> byCategory = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Object[]> e : byItem.entrySet()) {
            Object[] v = e.getValue();
            DailyReportDto.Row row = new DailyReportDto.Row((String) v[0], (Integer) v[1], (BigDecimal) v[2]);
            products.add(row);
            total = total.add(row.getTotal());
            String cat = categoryOf.computeIfAbsent(e.getKey() == null ? -1L : e.getKey(), id -> {
                if (id < 0) {
                    return "Sin categoría";
                }
                return itemRepository.findById(id)
                        .map(i -> i.getCategory() != null ? i.getCategory().getName() : "Sin categoría")
                        .orElse("Sin categoría");
            });
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(row);
        }
        products.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        DailyReportDto report = new DailyReportDto();
        report.setDate(d);
        report.setTradeCount(trades.size());
        report.setTotal(total);
        report.setProducts(products);
        for (Map.Entry<String, List<DailyReportDto.Row>> e : byCategory.entrySet()) {
            e.getValue().sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            report.getCategories().add(new DailyReportDto.CategoryBlock(e.getKey(), e.getValue()));
        }
        return report;
    }

    private static void addToReport(Map<Long, Object[]> byItem, Long itemId, String name, int quantity,
            BigDecimal total) {
        Object[] acc = byItem.computeIfAbsent(itemId, k -> new Object[] { name, 0, BigDecimal.ZERO });
        acc[1] = (Integer) acc[1] + quantity;
        acc[2] = ((BigDecimal) acc[2]).add(total);
    }

    /**
     * Añade a cada venta el estado de envío a la AEAT y la URL del QR. Una sola consulta
     * para toda la lista. No hace nada si VERI*FACTU está desactivado.
     */
    private void enrich(Collection<TradeDto> dtos) {
        if (!verifactuProperties.isEnabled() || dtos.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (TradeDto d : dtos) {
            ids.add(d.getId());
        }
        Map<Long, BillingRecord> latest = new HashMap<>();
        Map<Long, BillingRecord> alta = new HashMap<>();
        for (BillingRecord r : billingRecordRepository.findByTradeIdIn(ids)) {
            latest.merge(r.getTradeId(), r, (a, b) -> a.getId() > b.getId() ? a : b);
            if (r.getType() == BillingRecordType.ALTA) {
                alta.put(r.getTradeId(), r);
            }
        }
        for (TradeDto d : dtos) {
            BillingRecord l = latest.get(d.getId());
            if (l != null) {
                d.setVerifactuStatus(l.getStatus().name());
            }
            BillingRecord a = alta.get(d.getId());
            if (a != null) {
                d.setVerifactuQrUrl(VerifactuQr.url(verifactuProperties.qrBaseUrl(), a.getNif(), a.getNumSerie(),
                        a.getFechaExpedicion(), a.getImporteTotal()));
            }
        }
    }

    private BigDecimal latestPrice(Item item) {
        SortedMap<LocalDateTime, Double> history = item.getPriceHistory();
        if (history == null || history.isEmpty()) {
            throw new IllegalStateException("El producto " + item.getId() + " no tiene precio");
        }
        return BigDecimal.valueOf(history.get(history.lastKey())).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal igicRateOf(Category category) {
        if (category != null && category.getIgicRate() != null) {
            return category.getIgicRate();
        }
        return defaultIgicRate;
    }

}