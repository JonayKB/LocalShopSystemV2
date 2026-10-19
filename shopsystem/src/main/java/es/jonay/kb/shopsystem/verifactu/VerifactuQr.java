package es.jonay.kb.shopsystem.verifactu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * URL del código QR tributario (modo VERI*FACTU).
 * Spec: AEAT "Detalle de las especificaciones técnicas del código QR de la factura y de la URL
 * del servicio de cotejo" v0.5.0 (10/12/2025).
 *
 * Solo 4 parámetros, en este orden: nif, numserie, fecha (DD-MM-AAAA), importe (punto decimal).
 * Los valores van con URL encoding UTF-8. El parámetro "formato" NUNCA debe ir en el QR.
 * Impresión: QR de 30x30 a 40x40 mm, ISO/IEC 18004, corrección de errores nivel M, margen
 * blanco mínimo 2 mm (recomendado 6 mm), texto "QR tributario:" encima y la frase
 * "Factura verificable en la sede electrónica de la AEAT" o "VERI*FACTU" debajo.
 */
public final class VerifactuQr {

    public static final String BASE_PRUEBAS = "https://prewww2.aeat.es/wlpl/TIKE-CONT/ValidarQR";
    public static final String BASE_PRODUCCION = "https://www2.agenciatributaria.gob.es/wlpl/TIKE-CONT/ValidarQR";

    public static final String TEXTO_ENCIMA = "QR tributario:";
    public static final String TEXTO_DEBAJO = "Factura verificable en la sede electrónica de la AEAT";

    private VerifactuQr() {}

    /** @param baseUrl BASE_PRUEBAS o BASE_PRODUCCION (sin el "?") */
    public static String url(String baseUrl, String nif, String numSerie, String fechaDdMmAaaa,
                             BigDecimal importeTotal) {
        return baseUrl
                + "?nif=" + enc(nif)
                + "&numserie=" + enc(numSerie)
                + "&fecha=" + enc(fechaDdMmAaaa)
                + "&importe=" + importeTotal.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String enc(String v) {
        return URLEncoder.encode(v.trim(), StandardCharsets.UTF_8);
    }
}
