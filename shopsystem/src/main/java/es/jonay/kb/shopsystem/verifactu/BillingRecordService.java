package es.jonay.kb.shopsystem.verifactu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.model.entities.BillingChain;
import es.jonay.kb.shopsystem.model.entities.BillingRecord;
import es.jonay.kb.shopsystem.model.entities.BillingRecordType;
import es.jonay.kb.shopsystem.model.entities.Trade;
import es.jonay.kb.shopsystem.model.entities.TradeLine;
import es.jonay.kb.shopsystem.model.repository.IBillingChainRepository;
import es.jonay.kb.shopsystem.model.repository.IBillingRecordRepository;
import es.jonay.kb.shopsystem.verifactu.RecordXmlBuilder.Issuer;
import es.jonay.kb.shopsystem.verifactu.RecordXmlBuilder.Previous;
import es.jonay.kb.shopsystem.verifactu.RecordXmlBuilder.SystemInfo;
import es.jonay.kb.shopsystem.verifactu.RecordXmlBuilder.TaxLine;

/**
 * Genera los registros de alta y anulación encadenados. Se llama siempre desde la misma
 * transacción que guarda (o anula) la venta: si algo falla, no queda registro ni se
 * consume el eslabón de la cadena.
 */
@Service
public class BillingRecordService {

    public static final ZoneId ZONE = ZoneId.of("Atlantic/Canary");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    /** 'xxx' (minúscula) escribe siempre +00:00 y nunca "Z". */
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private static final String TIPO_FACTURA = "F2"; // factura simplificada

    private final Logger logger = Logger.getLogger(BillingRecordService.class.getName());

    private IBillingRecordRepository records;
    private IBillingChainRepository chains;
    private VerifactuProperties props;

    @Autowired
    public void setRecords(IBillingRecordRepository records) { this.records = records; }

    @Autowired
    public void setChains(IBillingChainRepository chains) { this.chains = chains; }

    @Autowired
    public void setProps(VerifactuProperties props) { this.props = props; }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    public static String invoiceNumber(String series, Long number) {
        return String.format("%s-%06d", series, number);
    }

    public static String fecha(Date date) {
        return FECHA.format(date.toInstant().atZone(ZONE));
    }

    /** Registro de alta de una venta recién emitida. Devuelve null si VERI*FACTU está desactivado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public BillingRecord createAlta(Trade trade) {
        if (!props.isEnabled()) {
            return null;
        }
        requireConfigured();
        if (trade.getSeries() == null || trade.getNumber() == null || trade.getTotal() == null) {
            throw new IllegalStateException("La venta " + trade.getId() + " no tiene número de factura o total");
        }

        String nif = props.getNif();
        String numSerie = invoiceNumber(trade.getSeries(), trade.getNumber());
        String fecha = fecha(trade.getDate());

        // Desglose de IGIC por tipo, a partir de las líneas congeladas de la venta.
        Map<BigDecimal, BigDecimal[]> byRate = new TreeMap<>();
        for (TradeLine l : trade.getLines()) {
            BigDecimal[] acc = byRate.computeIfAbsent(l.getTaxRate().setScale(2, RoundingMode.HALF_UP),
                    k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            acc[0] = acc[0].add(l.getTaxBase());
            acc[1] = acc[1].add(l.getTaxAmount());
        }
        List<TaxLine> lines = new ArrayList<>();
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal cuota = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, BigDecimal[]> e : byRate.entrySet()) {
            lines.add(new TaxLine(e.getKey(), e.getValue()[0], e.getValue()[1]));
            base = base.add(e.getValue()[0]);
            cuota = cuota.add(e.getValue()[1]);
        }
        if (lines.isEmpty() || lines.size() > 12) {
            throw new IllegalStateException("Desglose de IGIC no válido (" + lines.size() + " tipos)");
        }
        BigDecimal importe = trade.getTotal();
        if (base.add(cuota).compareTo(importe) != 0) {
            throw new IllegalStateException("Base + cuota (" + base.add(cuota) + ") no cuadra con el total " + importe);
        }

        BillingChain chain = lockChain();
        String ts = TS.format(ZonedDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS));
        String prevHash = chain.getLastHash();
        Previous previous = prevHash == null ? null
                : new Previous(chain.getLastNif(), chain.getLastNumSerie(), chain.getLastFecha(), prevHash);

        String hash = VerifactuHash.alta(nif, numSerie, fecha, TIPO_FACTURA, cuota, importe, prevHash, ts);
        String xml = RecordXmlBuilder.alta(new RecordXmlBuilder.Alta(issuer(), numSerie, fecha, TIPO_FACTURA,
                props.getDescription(), props.getClaveRegimen(), lines, cuota, importe, previous, systemInfo(), ts,
                hash));

        BillingRecord record = records.save(BillingRecord.of(BillingRecordType.ALTA, trade.getId(), nif, numSerie,
                fecha, importe, ts, prevHash, hash, xml));
        chain.advance(hash, nif, numSerie, fecha);
        chains.save(chain);
        return record;
    }

    /**
     * Registro de anulación de una venta que ya tenía registro de alta. Si la venta no tiene
     * alta (anterior a VERI*FACTU) o ya estaba anulada, no hace nada y devuelve null.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BillingRecord createAnulacion(Trade trade) {
        if (!props.isEnabled()) {
            return null;
        }
        requireConfigured();
        Optional<BillingRecord> alta = records.findFirstByTradeIdAndTypeOrderByIdDesc(trade.getId(),
                BillingRecordType.ALTA);
        if (alta.isEmpty()) {
            logger.warning("Venta " + trade.getId() + " anulada sin registro de alta previo: no se genera anulación");
            return null;
        }
        if (records.existsByTradeIdAndType(trade.getId(), BillingRecordType.ANULACION)) {
            return null;
        }
        BillingRecord a = alta.get();

        BillingChain chain = lockChain();
        String ts = TS.format(ZonedDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS));
        String prevHash = chain.getLastHash();
        Previous previous = prevHash == null ? null
                : new Previous(chain.getLastNif(), chain.getLastNumSerie(), chain.getLastFecha(), prevHash);

        String hash = VerifactuHash.anulacion(a.getNif(), a.getNumSerie(), a.getFechaExpedicion(), prevHash, ts);
        String xml = RecordXmlBuilder.anulacion(new RecordXmlBuilder.Anulacion(a.getNif(), a.getNumSerie(),
                a.getFechaExpedicion(), previous, systemInfo(), ts, hash));

        BillingRecord record = records.save(BillingRecord.of(BillingRecordType.ANULACION, trade.getId(), a.getNif(),
                a.getNumSerie(), a.getFechaExpedicion(), null, ts, prevHash, hash, xml));
        chain.advance(hash, a.getNif(), a.getNumSerie(), a.getFechaExpedicion());
        chains.save(chain);
        return record;
    }

    public Issuer issuer() {
        return new Issuer(props.getBusinessName(), props.getNif());
    }

    private SystemInfo systemInfo() {
        VerifactuProperties.Software s = props.getSoftware();
        return new SystemInfo(s.getProducerName(), s.getProducerNif(), s.getName(), s.getId(), s.getVersion(),
                s.getInstallationNumber(), true, false, false);
    }

    private BillingChain lockChain() {
        return chains.findByIdForUpdate(BillingChain.MAIN).orElseGet(() -> {
            chains.saveAndFlush(new BillingChain());
            return chains.findByIdForUpdate(BillingChain.MAIN).orElseThrow();
        });
    }

    /** Falla pronto y claro si falta configuración, en vez de generar registros que la AEAT rechazaría. */
    private void requireConfigured() {
        VerifactuProperties.Software s = props.getSoftware();
        require(props.getNif() != null && props.getNif().length() == 9, "verifactu.nif (9 caracteres)");
        require(notBlank(props.getBusinessName()), "verifactu.business-name");
        require(notBlank(s.getProducerName()), "verifactu.software.producer-name");
        require(s.getProducerNif() != null && s.getProducerNif().length() == 9, "verifactu.software.producer-nif (9 caracteres)");
        require(s.getName() != null && s.getName().length() <= 30, "verifactu.software.name (máx. 30)");
        require(s.getId() != null && s.getId().length() >= 1 && s.getId().length() <= 2, "verifactu.software.id (1-2 caracteres)");
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static void require(boolean ok, String what) {
        if (!ok) {
            throw new IllegalStateException("Configuración VERI*FACTU incompleta: " + what);
        }
    }
}
