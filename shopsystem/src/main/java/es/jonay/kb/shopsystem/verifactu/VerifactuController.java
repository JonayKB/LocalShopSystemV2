package es.jonay.kb.shopsystem.verifactu;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.model.entities.BillingSendStatus;
import es.jonay.kb.shopsystem.model.repository.IBillingRecordRepository;

/** Estado de VERI*FACTU para el panel de administración. */
@RestController
@RequestMapping("verifactu")
@CrossOrigin
public class VerifactuController {

    private IBillingRecordRepository records;
    private VerifactuProperties props;
    private VerifactuSender sender;

    @Autowired
    public void setRecords(IBillingRecordRepository records) { this.records = records; }

    @Autowired
    public void setProps(VerifactuProperties props) { this.props = props; }

    @Autowired
    public void setSender(VerifactuSender sender) { this.sender = sender; }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", props.isEnabled());
        out.put("environment", props.getEnvironment());
        Map<String, Long> counts = new LinkedHashMap<>();
        for (BillingSendStatus s : BillingSendStatus.values()) {
            counts.put(s.name(), 0L);
        }
        for (Object[] row : records.countGroupedByStatus()) {
            counts.put(((BillingSendStatus) row[0]).name(), (Long) row[1]);
        }
        out.put("counts", counts);
        out.put("lastError", sender.getLastError());
        out.put("lastSuccess", sender.getLastSuccess());
        return out;
    }

    /** Lanza un ciclo de envío ahora mismo (botón "Enviar ahora"). */
    @PostMapping("/send-now")
    public ResponseEntity<Map<String, Object>> sendNow() {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!props.isEnabled()) {
            out.put("error", "VERI*FACTU está desactivado");
            return ResponseEntity.badRequest().body(out);
        }
        try {
            out.put("sent", sender.sendPending());
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(502).body(out);
        }
    }
}
