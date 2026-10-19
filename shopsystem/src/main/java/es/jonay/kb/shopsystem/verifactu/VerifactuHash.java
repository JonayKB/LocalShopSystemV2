package es.jonay.kb.shopsystem.verifactu;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Huella (hash) de los registros de facturación VERI*FACTU.
 * Spec: AEAT "Detalle de las especificaciones técnicas para generación de la huella
 * o hash de los registros de facturación" v0.1.2 (27/08/2024).
 *
 * Cadena "campo=valor&campo=valor..." -> UTF-8 -> SHA-256 -> hex en MAYÚSCULAS (64 car.).
 * Campo ausente o vacío: se escribe "nombre=" sin valor.
 */
public final class VerifactuHash {

    private VerifactuHash() {}

    /** Huella de un registro de ALTA. previousHash null/vacío si es el primer registro. */
    public static String alta(String idEmisorFactura, String numSerieFactura, String fechaExpedicion,
                              String tipoFactura, BigDecimal cuotaTotal, BigDecimal importeTotal,
                              String previousHash, String fechaHoraHusoGenRegistro) {
        return sha256Upper(altaInput(idEmisorFactura, numSerieFactura, fechaExpedicion, tipoFactura,
                cuotaTotal, importeTotal, previousHash, fechaHoraHusoGenRegistro));
    }

    /** Huella de un registro de ANULACIÓN. */
    public static String anulacion(String idEmisorFacturaAnulada, String numSerieFacturaAnulada,
                                   String fechaExpedicionAnulada, String previousHash,
                                   String fechaHoraHusoGenRegistro) {
        return sha256Upper(anulacionInput(idEmisorFacturaAnulada, numSerieFacturaAnulada,
                fechaExpedicionAnulada, previousHash, fechaHoraHusoGenRegistro));
    }

    static String altaInput(String nif, String serie, String fecha, String tipo,
                            BigDecimal cuota, BigDecimal importe, String prev, String ts) {
        return "IDEmisorFactura=" + v(nif)
                + "&NumSerieFactura=" + v(serie)
                + "&FechaExpedicionFactura=" + v(fecha)
                + "&TipoFactura=" + v(tipo)
                + "&CuotaTotal=" + money(cuota)
                + "&ImporteTotal=" + money(importe)
                + "&Huella=" + v(prev)
                + "&FechaHoraHusoGenRegistro=" + v(ts);
    }

    static String anulacionInput(String nif, String serie, String fecha, String prev, String ts) {
        return "IDEmisorFacturaAnulada=" + v(nif)
                + "&NumSerieFacturaAnulada=" + v(serie)
                + "&FechaExpedicionFacturaAnulada=" + v(fecha)
                + "&Huella=" + v(prev)
                + "&FechaHoraHusoGenRegistro=" + v(ts);
    }

    /** Importe como en el XML: punto decimal, máx. 2 decimales (el hash tolera 12.3 o 12.30). */
    public static String money(BigDecimal value) {
        if (value == null) return "";
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String v(String s) { return s == null ? "" : s.trim(); }

    static String sha256Upper(String input) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : d) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
