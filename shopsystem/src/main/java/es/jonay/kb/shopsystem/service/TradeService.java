package es.jonay.kb.shopsystem.service;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.api.dto.DailyReportDto;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.SaleRequest;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.mappers.TradeMapper;
import es.jonay.kb.shopsystem.model.entities.BillingRecord;
import es.jonay.kb.shopsystem.model.entities.BillingRecordType;
import es.jonay.kb.shopsystem.model.entities.Category;
import es.jonay.kb.shopsystem.model.entities.InvoiceCounter;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.PaymentMethod;
import es.jonay.kb.shopsystem.model.entities.StockMovementType;
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

@Service
public class TradeService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ITradeRepository tradeRepository;
    private final ICategoryRepository categoryRepository;
    private final IInvoiceCounterRepository counterRepository;
    private final InventoryService inventory;
    private final IItemRepository itemRepository;
    private final TradeMapper tradeMapper;
    private final BillingRecordService billingRecordService;
    private final IBillingRecordRepository billingRecordRepository;
    private final VerifactuProperties verifactuProperties;

    /** Serie de las facturas simplificadas (tickets). */
    @Value("${shop.invoice.series:T}")
    private String invoiceSeries;

    /**
     * Tipo de IGIC (%) por defecto si la categoría no tiene uno propio.
     * Confirma los tipos reales de cada categoría con tu gestoría.
     */
    @Value("${shop.tax.default-igic-rate:7.00}")
    private BigDecimal defaultIgicRate;

    public TradeService(BillingRecordService billingRecordService, IBillingRecordRepository billingRecordRepository, VerifactuProperties verifactuProperties, TradeMapper tradeMapper, ICategoryRepository categoryRepository, ITradeRepository tradeRepository, IInvoiceCounterRepository counterRepository, InventoryService inventory, IItemRepository itemRepository) {
        this.billingRecordService = billingRecordService;
        this.billingRecordRepository = billingRecordRepository;
        this.verifactuProperties = verifactuProperties;
        this.tradeMapper = tradeMapper;
        this.categoryRepository = categoryRepository;
        this.tradeRepository = tradeRepository;
        this.counterRepository = counterRepository;
        this.inventory = inventory;
        this.itemRepository = itemRepository;
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
     * Emite una venta nueva. Siempre pasa por {@link #saveList(List, PaymentMethod)}: la fecha,
     * el número y los importes los fija el servidor, nunca el cliente.
     */
    @Transactional
    public TradeDto save(TradeDto tradeDto) {
        return saveList(tradeDto.getItems(), PaymentMethod.parse(tradeDto.getPaymentMethod()));
    }

    /**
     * Emite una factura simplificada: agrupa los productos en líneas, congela
     * precio e IGIC, descuenta stock y asigna el siguiente número de la serie.
     * Todo en una única transacción: si algo falla, no se consume número.
     */
    @Transactional
    public TradeDto saveList(List<ItemDto> items, PaymentMethod paymentMethod) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La venta no tiene productos");
        }
        // Formato antiguo (un elemento por unidad): se agrupa por producto en el orden de entrada.
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (ItemDto itemDto : items) {
            if (itemDto == null || itemDto.getId() == null) {
                throw new IllegalArgumentException("Hay un producto sin código en la venta");
            }
            quantities.merge(itemDto.getId(), 1, Integer::sum);
        }
        return sell(quantities, paymentMethod);
    }

    /** Venta con líneas {producto, cantidad}. Las líneas repetidas de un mismo producto se suman. */
    @Transactional
    public TradeDto sell(List<SaleRequest.Line> lines, PaymentMethod paymentMethod) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("La venta no tiene productos");
        }
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (SaleRequest.Line l : lines) {
            if (l == null || l.itemId() == null) {
                throw new IllegalArgumentException("Hay un producto sin código en la venta");
            }
            if (l.quantity() == null || l.quantity() <= 0 || l.quantity() > 10_000) {
                throw new IllegalArgumentException("Cantidad no válida para el producto " + l.itemId());
            }
            quantities.merge(l.itemId(), l.quantity(), Integer::sum);
        }
        return sell(quantities, paymentMethod);
    }

    private TradeDto sell(Map<Long, Integer> quantities, PaymentMethod paymentMethod) {
        counterRepository.lockOrCreate(invoiceSeries);
        InvoiceCounter counter = counterRepository.findBySeriesForUpdate(invoiceSeries).orElseThrow();

        // Bloquea los productos en orden de id (evita bloqueos cruzados con otras operaciones).
        Map<Long, Item> locked = new java.util.TreeMap<>();
        for (Long id : new java.util.TreeSet<>(quantities.keySet())) {
            locked.put(id, inventory.lock(id));
        }

        Trade trade = new Trade();
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Item item = locked.get(entry.getKey());
            int quantity = entry.getValue();

            BigDecimal unitPrice = currentPrice(item);
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
            line.setUnitCost(item.getCost() != null && item.getCost().signum() > 0 ? item.getCost() : null);
            if (item.getCategory() != null) {
                line.setCategoryId(item.getCategory().getId());
                line.setCategoryName(item.getCategory().getName());
            }
            trade.addLine(line);

            totalBase = totalBase.add(base);
            totalTax = totalTax.add(tax);
            total = total.add(lineTotal);
        }

        trade.setDate(new Date());
        trade.setSeries(invoiceSeries);
        trade.setNumber(counter.next());
        trade.setStatus(TradeStatus.ISSUED);
        trade.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.CASH);
        trade.setTotalBase(totalBase);
        trade.setTotalTax(totalTax);
        trade.setTotal(total);

        counterRepository.save(counter);
        Trade saved = tradeRepository.save(trade);
        String ref = "Venta " + BillingRecordService.invoiceNumber(saved.getSeries(), saved.getNumber());
        for (TradeLine line : saved.getLines()) {
            inventory.apply(locked.get(line.getItemId()), StockMovementType.SALE, -line.getQuantity(),
                    line.getUnitCost(), saved.getId(), ref);
        }
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
        // Devuelve el stock según las líneas (ventas antiguas sin líneas: según sus productos).
        Map<Long, Integer> toReturn = new java.util.TreeMap<>();
        if (trade.getLines() != null && !trade.getLines().isEmpty()) {
            for (TradeLine l : trade.getLines()) {
                if (l.getItemId() != null) {
                    toReturn.merge(l.getItemId(), l.getQuantity(), Integer::sum);
                }
            }
        } else {
            for (Item item : trade.getItems()) {
                toReturn.merge(item.getId(), 1, Integer::sum);
            }
        }
        String ref = "Anulación de la venta " + (trade.getSeries() != null && trade.getNumber() != null
                ? BillingRecordService.invoiceNumber(trade.getSeries(), trade.getNumber())
                : String.valueOf(trade.getId()));
        for (Map.Entry<Long, Integer> e : toReturn.entrySet()) {
            if (!itemRepository.existsById(e.getKey())) {
                continue; // producto borrado después de la venta: no hay stock que devolver
            }
            inventory.apply(inventory.lock(e.getKey()), StockMovementType.SALE_CANCEL, e.getValue(), null,
                    trade.getId(), ref);
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

        // clave = id de producto (o descripción si no tiene); guarda nombre, unidades, importe y categoría.
        // Todo sale de las líneas congeladas: sin consultas extra por producto.
        Map<String, Object[]> byItem = new LinkedHashMap<>();
        BigDecimal cashTotal = BigDecimal.ZERO;
        BigDecimal cardTotal = BigDecimal.ZERO;
        int cardCount = 0;
        for (Trade trade : trades) {
            BigDecimal tradeTotal = BigDecimal.ZERO;
            for (TradeLine l : trade.getLines()) {
                String key = l.getItemId() != null ? "id:" + l.getItemId() : "name:" + l.getDescription();
                String category = l.getCategoryName() != null ? l.getCategoryName() : "Sin categoría";
                Object[] acc = byItem.computeIfAbsent(key,
                        k -> new Object[] { l.getDescription(), 0, BigDecimal.ZERO, category });
                acc[1] = (Integer) acc[1] + l.getQuantity();
                acc[2] = ((BigDecimal) acc[2]).add(l.getLineTotal());
                tradeTotal = tradeTotal.add(l.getLineTotal());
            }
            if (trade.getPaymentMethod() == PaymentMethod.CARD) {
                cardTotal = cardTotal.add(tradeTotal);
                cardCount++;
            } else {
                cashTotal = cashTotal.add(tradeTotal);
            }
        }

        List<DailyReportDto.Row> products = new ArrayList<>();
        Map<String, List<DailyReportDto.Row>> byCategory = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] v : byItem.values()) {
            DailyReportDto.Row row = new DailyReportDto.Row((String) v[0], (Integer) v[1], (BigDecimal) v[2]);
            products.add(row);
            total = total.add(row.getTotal());
            byCategory.computeIfAbsent((String) v[3], k -> new ArrayList<>()).add(row);
        }
        products.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        DailyReportDto report = new DailyReportDto();
        report.setDate(d);
        report.setTradeCount(trades.size());
        report.setTotal(total);
        report.setCashTotal(cashTotal);
        report.setCardTotal(cardTotal);
        report.setCashCount(trades.size() - cardCount);
        report.setCardCount(cardCount);
        report.setProducts(products);
        for (Map.Entry<String, List<DailyReportDto.Row>> e : byCategory.entrySet()) {
            e.getValue().sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            report.getCategories().add(new DailyReportDto.CategoryBlock(e.getKey(), e.getValue()));
        }
        return report;
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

    private static BigDecimal currentPrice(Item item) {
        if (item.getPrice() == null || item.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("El producto \"" + item.getName() + "\" no tiene precio de venta");
        }
        return item.getPrice().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal igicRateOf(Category category) {
        if (category != null && category.getIgicRate() != null) {
            return category.getIgicRate();
        }
        return defaultIgicRate;

    }

}