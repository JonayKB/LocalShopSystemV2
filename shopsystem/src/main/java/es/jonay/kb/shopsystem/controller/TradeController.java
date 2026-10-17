package es.jonay.kb.shopsystem.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
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

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.mappers.TradeMapper;
import es.jonay.kb.shopsystem.model.entities.Category;
import es.jonay.kb.shopsystem.model.entities.InvoiceCounter;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.Trade;
import es.jonay.kb.shopsystem.model.entities.TradeLine;
import es.jonay.kb.shopsystem.model.entities.TradeStatus;
import es.jonay.kb.shopsystem.model.repository.ICategoryRepository;
import es.jonay.kb.shopsystem.model.repository.IInvoiceCounterRepository;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;
import es.jonay.kb.shopsystem.model.repository.ITradeRepository;

@Controller
public class TradeController {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private ITradeRepository tradeRepository;
    private ICategoryRepository categoryRepository;
    private IInvoiceCounterRepository counterRepository;
    private ItemController itemController;
    private IItemRepository itemRepository;
    private TradeMapper tradeMapper;

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
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<TradeDto> findById(Long id) {
        Optional<Trade> trade = tradeRepository.findById(id);
        return trade.map(tradeMapper::toTradeDto);
    }

    /**
     * Emite una venta nueva. Siempre pasa por {@link #saveList(List)}: la fecha,
     * el número y los importes los fija el servidor, nunca el cliente.
     */
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
        return tradeMapper.toTradeDto(tradeRepository.save(trade));
    }

    /**
     * Anula una venta (no la borra): conserva el registro, marca el estado y
     * devuelve el stock. Más adelante aquí se generará el registro de anulación
     * de Verifactu.
     */
    @Transactional
    public TradeDto cancel(Long id, String reason) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Venta no encontrada: " + id));
        if (trade.getStatus() == TradeStatus.CANCELLED) {
            return tradeMapper.toTradeDto(trade);
        }
        trade.setStatus(TradeStatus.CANCELLED);
        trade.setCancelledAt(new Date());
        trade.setCancellationReason(reason == null || reason.isBlank() ? null : reason.trim());
        for (Item item : trade.getItems()) {
            itemController.addStock(item.getId(), 1);
        }
        return tradeMapper.toTradeDto(tradeRepository.save(trade));
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
        return tradesDtoList;
    }

    /** Incluye también las anuladas, para que se vean (marcadas) en el panel. */
    @Transactional(readOnly = true)
    public Page<TradeDto> getPage(int page, int size) {
        Page<Trade> trades = tradeRepository.findAll(PageRequest.of(page, size).withSort(Sort.by("date").descending()));
        return trades.map(tradeMapper::toTradeDto);
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
