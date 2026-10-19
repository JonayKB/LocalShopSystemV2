package es.jonay.kb.shopsystem.verifactu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Genera el XML de los registros de facturación (alta y anulación) y el sobre de envío,
 * siguiendo el orden exacto de los esquemas SuministroInformacion.xsd y SuministroLR.xsd.
 *
 * Los registros se devuelven como fragmentos con prefijo "sf:" (los namespaces se declaran
 * en el sobre). Se guardan tal cual en base de datos: lo que se envía es siempre lo mismo
 * que se usó para calcular la huella.
 */
public final class RecordXmlBuilder {

    private static final String NS_BASE =
            "https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/";
    public static final String NS_SF = NS_BASE + "SuministroInformacion.xsd";
    public static final String NS_LR = NS_BASE + "SuministroLR.xsd";

    private RecordXmlBuilder() {}

    public record Issuer(String name, String nif) {}

    public record SystemInfo(String producerName, String producerNif, String systemName, String systemId,
                             String version, String installationNumber,
                             boolean onlyVerifactu, boolean multiOt, boolean multipleOt) {}

    /** Una línea de desglose de IGIC (un tipo impositivo). */
    public record TaxLine(BigDecimal rate, BigDecimal base, BigDecimal tax) {}

    /** Registro anterior de la cadena. Null en el primer registro. */
    public record Previous(String nif, String numSerie, String fecha, String hash) {}

    public record Alta(Issuer issuer, String numSerie, String fecha, String tipoFactura, String description,
                       String claveRegimen, List<TaxLine> lines, BigDecimal cuotaTotal, BigDecimal importeTotal,
                       Previous previous, SystemInfo system, String timestamp, String hash) {}

    public record Anulacion(String nif, String numSerie, String fecha, Previous previous, SystemInfo system,
                            String timestamp, String hash) {}

    public static String alta(Alta a) {
        StringBuilder x = new StringBuilder(1500);
        x.append("<sf:RegistroAlta>");
        el(x, "IDVersion", "1.0");
        x.append("<sf:IDFactura>");
        el(x, "IDEmisorFactura", a.issuer().nif());
        el(x, "NumSerieFactura", a.numSerie());
        el(x, "FechaExpedicionFactura", a.fecha());
        x.append("</sf:IDFactura>");
        el(x, "NombreRazonEmisor", a.issuer().name());
        el(x, "TipoFactura", a.tipoFactura());
        el(x, "DescripcionOperacion", a.description());
        x.append("<sf:Desglose>");
        for (TaxLine l : a.lines()) {
            x.append("<sf:DetalleDesglose>");
            el(x, "Impuesto", "03"); // IGIC
            el(x, "ClaveRegimen", a.claveRegimen());
            el(x, "CalificacionOperacion", "S1");
            el(x, "TipoImpositivo", money(l.rate()));
            el(x, "BaseImponibleOimporteNoSujeto", money(l.base()));
            el(x, "CuotaRepercutida", money(l.tax()));
            x.append("</sf:DetalleDesglose>");
        }
        x.append("</sf:Desglose>");
        el(x, "CuotaTotal", money(a.cuotaTotal()));
        el(x, "ImporteTotal", money(a.importeTotal()));
        chain(x, a.previous());
        system(x, a.system());
        el(x, "FechaHoraHusoGenRegistro", a.timestamp());
        el(x, "TipoHuella", "01");
        el(x, "Huella", a.hash());
        x.append("</sf:RegistroAlta>");
        return x.toString();
    }

    public static String anulacion(Anulacion a) {
        StringBuilder x = new StringBuilder(1000);
        x.append("<sf:RegistroAnulacion>");
        el(x, "IDVersion", "1.0");
        x.append("<sf:IDFactura>");
        el(x, "IDEmisorFacturaAnulada", a.nif());
        el(x, "NumSerieFacturaAnulada", a.numSerie());
        el(x, "FechaExpedicionFacturaAnulada", a.fecha());
        x.append("</sf:IDFactura>");
        chain(x, a.previous());
        system(x, a.system());
        el(x, "FechaHoraHusoGenRegistro", a.timestamp());
        el(x, "TipoHuella", "01");
        el(x, "Huella", a.hash());
        x.append("</sf:RegistroAnulacion>");
        return x.toString();
    }

    /** Sobre RegFactuSistemaFacturacion con 1 a 1000 registros ya generados. */
    public static String envelope(Issuer issuer, List<String> recordFragments) {
        if (recordFragments.isEmpty() || recordFragments.size() > 1000) {
            throw new IllegalArgumentException("Un envío lleva entre 1 y 1000 registros");
        }
        StringBuilder x = new StringBuilder(2000 + recordFragments.size() * 1500);
        x.append("<sfLR:RegFactuSistemaFacturacion xmlns:sfLR=\"").append(NS_LR)
                .append("\" xmlns:sf=\"").append(NS_SF).append("\">");
        x.append("<sfLR:Cabecera><sf:ObligadoEmision>");
        el(x, "NombreRazon", issuer.name());
        el(x, "NIF", issuer.nif());
        x.append("</sf:ObligadoEmision></sfLR:Cabecera>");
        for (String r : recordFragments) {
            x.append("<sfLR:RegistroFactura>").append(r).append("</sfLR:RegistroFactura>");
        }
        x.append("</sfLR:RegFactuSistemaFacturacion>");
        return x.toString();
    }

    public static String soap(String envelope) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Header/><soapenv:Body>" + envelope + "</soapenv:Body></soapenv:Envelope>";
    }

    private static void chain(StringBuilder x, Previous p) {
        x.append("<sf:Encadenamiento>");
        if (p == null) {
            el(x, "PrimerRegistro", "S");
        } else {
            x.append("<sf:RegistroAnterior>");
            el(x, "IDEmisorFactura", p.nif());
            el(x, "NumSerieFactura", p.numSerie());
            el(x, "FechaExpedicionFactura", p.fecha());
            el(x, "Huella", p.hash());
            x.append("</sf:RegistroAnterior>");
        }
        x.append("</sf:Encadenamiento>");
    }

    private static void system(StringBuilder x, SystemInfo s) {
        x.append("<sf:SistemaInformatico>");
        el(x, "NombreRazon", s.producerName());
        el(x, "NIF", s.producerNif());
        el(x, "NombreSistemaInformatico", s.systemName());
        el(x, "IdSistemaInformatico", s.systemId());
        el(x, "Version", s.version());
        el(x, "NumeroInstalacion", s.installationNumber());
        el(x, "TipoUsoPosibleSoloVerifactu", yn(s.onlyVerifactu()));
        el(x, "TipoUsoPosibleMultiOT", yn(s.multiOt()));
        el(x, "IndicadorMultiplesOT", yn(s.multipleOt()));
        x.append("</sf:SistemaInformatico>");
    }

    private static String yn(boolean b) { return b ? "S" : "N"; }

    private static String money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static void el(StringBuilder x, String name, String value) {
        x.append("<sf:").append(name).append('>').append(escape(value)).append("</sf:").append(name).append('>');
    }

    static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&apos;");
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
