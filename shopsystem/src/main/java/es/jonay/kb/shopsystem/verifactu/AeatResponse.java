package es.jonay.kb.shopsystem.verifactu;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Respuesta del servicio RegFactuSistemaFacturacion (RespuestaSuministro.xsd).
 * Estado del envío: Correcto / ParcialmenteCorrecto / Incorrecto.
 * Estado de cada registro: Correcto / AceptadoConErrores / Incorrecto.
 */
public final class AeatResponse {

    public record Line(String numSerie, String fecha, String operacion, String estadoRegistro,
                       String errorCode, String errorDescription, String duplicateState) {}

    private String csv;
    private String estadoEnvio;
    private int waitSeconds = 60;
    private final List<Line> lines = new ArrayList<>();

    public String getCsv() { return csv; }
    public String getEstadoEnvio() { return estadoEnvio; }
    public int getWaitSeconds() { return waitSeconds; }
    public List<Line> getLines() { return lines; }

    /** Lanza AeatFaultException si la respuesta es un SOAP Fault. */
    public static AeatResponse parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document d = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element fault = first(d.getDocumentElement(), "Fault");
            if (fault != null) {
                String code = text(first(fault, "faultcode"));
                String msg = text(first(fault, "faultstring"));
                throw new AeatFaultException(code, msg);
            }

            Element root = first(d.getDocumentElement(), "RespuestaRegFactuSistemaFacturacion");
            if (root == null) {
                throw new IllegalStateException("Respuesta inesperada de la AEAT (sin RespuestaRegFactuSistemaFacturacion)");
            }
            AeatResponse r = new AeatResponse();
            r.csv = text(child(root, "CSV"));
            r.estadoEnvio = text(child(root, "EstadoEnvio"));
            String wait = text(child(root, "TiempoEsperaEnvio"));
            if (wait != null && wait.matches("\\d{1,4}")) {
                r.waitSeconds = Integer.parseInt(wait);
            }
            NodeList kids = root.getChildNodes();
            for (int i = 0; i < kids.getLength(); i++) {
                Node n = kids.item(i);
                if (n instanceof Element e && "RespuestaLinea".equals(e.getLocalName())) {
                    Element id = child(e, "IDFactura");
                    Element op = child(e, "Operacion");
                    Element dup = child(e, "RegistroDuplicado");
                    r.lines.add(new Line(
                            text(child(id, "NumSerieFactura")),
                            text(child(id, "FechaExpedicionFactura")),
                            text(child(op, "TipoOperacion")),
                            text(child(e, "EstadoRegistro")),
                            text(child(e, "CodigoErrorRegistro")),
                            text(child(e, "DescripcionErrorRegistro")),
                            dup == null ? null : text(child(dup, "EstadoRegistroDuplicado"))));
                }
            }
            return r;
        } catch (AeatFaultException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo interpretar la respuesta de la AEAT: " + e.getMessage(), e);
        }
    }

    public static final class AeatFaultException extends RuntimeException {
        private final String code;
        public AeatFaultException(String code, String message) {
            super("SOAP Fault " + code + ": " + message);
            this.code = code;
        }
        public String getCode() { return code; }
    }

    private static Element child(Element parent, String local) {
        if (parent == null) return null;
        NodeList kids = parent.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            if (kids.item(i) instanceof Element e && local.equals(e.getLocalName())) return e;
        }
        return null;
    }

    private static Element first(Element parent, String local) {
        NodeList l = parent.getElementsByTagNameNS("*", local);
        return l.getLength() == 0 ? null : (Element) l.item(0);
    }

    private static String text(Element e) {
        return e == null ? null : e.getTextContent().trim();
    }
}
