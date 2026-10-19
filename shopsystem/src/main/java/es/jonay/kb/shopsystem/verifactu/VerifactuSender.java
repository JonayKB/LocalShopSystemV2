package es.jonay.kb.shopsystem.verifactu;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import es.jonay.kb.shopsystem.model.entities.BillingRecord;
import es.jonay.kb.shopsystem.model.entities.BillingRecordType;
import es.jonay.kb.shopsystem.model.entities.BillingSendStatus;
import es.jonay.kb.shopsystem.model.repository.IBillingRecordRepository;

/**
 * Envía a la AEAT los registros pendientes, en orden de cadena y en lotes de hasta 1000.
 * Respeta el TiempoEsperaEnvio que devuelve la AEAT y reintenta con espera creciente
 * si no hay conexión o el servicio falla. No hace falta tiempo real: si no hay internet,
 * los registros se acumulan y se envían al volver.
 */
@Service
public class VerifactuSender {

    private static final int MAX_BATCH = 1000;
    private static final long MAX_BACKOFF_SECONDS = 3600;

    private final Logger logger = Logger.getLogger(VerifactuSender.class.getName());

    private IBillingRecordRepository records;
    private VerifactuProperties props;
    private AeatClient client;
    private BillingRecordService billing;
    private TransactionTemplate tx;

    /** No enviar antes de este instante (TiempoEsperaEnvio de la AEAT), salvo que haya 1000 pendientes. */
    private volatile Instant notBefore = Instant.EPOCH;
    private volatile String lastError;
    private volatile Date lastSuccess;

    @Autowired
    public void setRecords(IBillingRecordRepository records) { this.records = records; }

    @Autowired
    public void setProps(VerifactuProperties props) { this.props = props; }

    @Autowired
    public void setClient(AeatClient client) { this.client = client; }

    @Autowired
    public void setBilling(BillingRecordService billing) { this.billing = billing; }

    @Autowired
    public void setTransactionManager(PlatformTransactionManager tm) { this.tx = new TransactionTemplate(tm); }

    public String getLastError() { return lastError; }
    public Date getLastSuccess() { return lastSuccess; }

    @Scheduled(initialDelayString = "30000", fixedDelayString = "${verifactu.sender.interval-ms:60000}")
    public void scheduled() {
        if (!props.isEnabled()) {
            return;
        }
        try {
            sendPending();
        } catch (Exception e) {
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            logger.severe("VERI*FACTU: error en el envío: " + lastError);
        }
    }

    /** Un ciclo de envío. Público para poder lanzarlo a mano desde el panel. */
    public synchronized int sendPending() throws Exception {
        Optional<BillingRecord> oldest = tx.execute(s -> records.findFirstByStatusOrderByIdAsc(BillingSendStatus.PENDING));
        if (oldest == null || oldest.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        Date due = oldest.get().getNextAttemptAt();
        if (due != null && due.after(now)) {
            return 0; // el más antiguo está en espera de reintento: no se adelanta a los demás
        }
        long pending = tx.execute(s -> records.countByStatus(BillingSendStatus.PENDING));
        if (Instant.now().isBefore(notBefore) && pending < MAX_BATCH) {
            return 0;
        }

        List<BillingRecord> batch = tx.execute(
                s -> records.findByStatusOrderByIdAsc(BillingSendStatus.PENDING, PageRequest.of(0, MAX_BATCH)));
        if (batch == null || batch.isEmpty()) {
            return 0;
        }
        List<Long> ids = new ArrayList<>();
        List<String> fragments = new ArrayList<>();
        for (BillingRecord r : batch) {
            ids.add(r.getId());
            fragments.add(r.getRegistroXml());
        }
        String envelope = RecordXmlBuilder.envelope(billing.issuer(), fragments);

        AeatResponse response;
        try {
            response = client.send(envelope);
        } catch (Exception e) {
            // Sin conexión, certificado, SOAP Fault, etc.: nada se ha aceptado; se reintenta más tarde.
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            markFailedAttempt(ids, lastError);
            throw e;
        }

        notBefore = Instant.now().plusSeconds(Math.max(response.getWaitSeconds(), 1));
        applyResponse(ids, response);
        lastError = null;
        lastSuccess = new Date();
        logger.info("VERI*FACTU: envío " + response.getEstadoEnvio() + " (" + ids.size() + " registros, CSV "
                + response.getCsv() + ")");
        return ids.size();
    }

    private void markFailedAttempt(List<Long> ids, String error) {
        tx.executeWithoutResult(s -> {
            for (BillingRecord r : records.findAllById(ids)) {
                int attempts = r.getAttempts() + 1;
                long wait = Math.min(MAX_BACKOFF_SECONDS, 60L * (1L << Math.min(attempts - 1, 6)));
                r.setAttempts(attempts);
                r.setLastAttemptAt(new Date());
                r.setNextAttemptAt(new Date(System.currentTimeMillis() + wait * 1000));
                r.setAeatErrorDescription(error);
                records.save(r);
            }
        });
    }

    private void applyResponse(List<Long> ids, AeatResponse response) {
        tx.executeWithoutResult(s -> {
            Map<String, AeatResponse.Line> byKey = new HashMap<>();
            for (AeatResponse.Line l : response.getLines()) {
                byKey.put(key(l.operacion(), l.numSerie(), l.fecha()), l);
            }
            boolean envioIncorrecto = "Incorrecto".equalsIgnoreCase(response.getEstadoEnvio())
                    && response.getLines().isEmpty();
            for (BillingRecord r : records.findAllById(ids)) {
                r.setAttempts(r.getAttempts() + 1);
                r.setLastAttemptAt(new Date());
                String op = r.getType() == BillingRecordType.ALTA ? "Alta" : "Anulacion";
                AeatResponse.Line l = byKey.get(key(op, r.getNumSerie(), r.getFechaExpedicion()));
                if (l == null) {
                    if (envioIncorrecto) {
                        r.setStatus(BillingSendStatus.REJECTED);
                        r.setAeatErrorDescription("Envío rechazado en conjunto por la AEAT");
                    } else {
                        // Sin línea de respuesta: se deja pendiente y se reintenta.
                        r.setNextAttemptAt(new Date(System.currentTimeMillis() + 60_000));
                    }
                    records.save(r);
                    continue;
                }
                r.setAeatErrorCode(l.errorCode());
                r.setAeatErrorDescription(l.errorDescription());
                if ("Correcto".equalsIgnoreCase(l.estadoRegistro())) {
                    r.setStatus(BillingSendStatus.ACCEPTED);
                } else if ("AceptadoConErrores".equalsIgnoreCase(l.estadoRegistro())) {
                    r.setStatus(BillingSendStatus.ACCEPTED_WITH_ERRORS);
                } else if (l.duplicateState() != null && l.duplicateState().toLowerCase().startsWith("correcta")) {
                    // Ya estaba registrado en la AEAT (p. ej. se perdió la respuesta de un envío anterior).
                    r.setStatus(BillingSendStatus.ACCEPTED);
                    r.setAeatErrorDescription("Registro ya existente en la AEAT (duplicado aceptado)");
                } else {
                    r.setStatus(BillingSendStatus.REJECTED);
                }
                r.setCsv(response.getCsv());
                r.setSentAt(new Date());
                r.setNextAttemptAt(null);
                records.save(r);
            }
        });
    }

    private static String key(String op, String numSerie, String fecha) {
        return op + "|" + numSerie + "|" + fecha;
    }
}
