package es.jonay.kb.shopsystem.verifactu;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración VERI*FACTU (application.properties, prefijo "verifactu").
 * Por defecto está DESACTIVADO: mientras enabled=false la venta funciona como siempre
 * y no se genera ningún registro.
 */
@ConfigurationProperties(prefix = "verifactu")
public class VerifactuProperties {

    private boolean enabled = false;

    /** "test" (prewww1/prewww2.aeat.es) o "production". */
    private String environment = "test";

    /** NIF del obligado a expedir (9 caracteres) y su nombre o razón social. */
    private String nif;
    private String businessName;

    private String description = "Venta de productos";

    /** ClaveRegimen (lista L8B para IGIC). "01" = régimen general. */
    private String claveRegimen = "01";

    /** Anula la URL del servicio si se informa (por defecto se elige por environment y certificate.seal). */
    private String endpoint;

    private final Certificate certificate = new Certificate();
    private final Software software = new Software();
    private final Sender sender = new Sender();
    private final Qr qr = new Qr();

    public static class Certificate {
        /** Ruta al .p12/.pfx del certificado cualificado (fuera del repositorio). */
        private String path;
        private String password;
        /** true si es un certificado de sello electrónico (usa los puertos "Sello"). */
        private boolean seal = false;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isSeal() { return seal; }
        public void setSeal(boolean seal) { this.seal = seal; }
    }

    /** Bloque SistemaInformatico del registro (tu software). */
    public static class Software {
        private String producerName;
        private String producerNif;
        /** Máx. 30 caracteres. */
        private String name = "KioscoBotanicoPOS";
        /** Máx. 2 caracteres. */
        private String id = "KB";
        private String version = "1.0";
        private String installationNumber = "1";

        public String getProducerName() { return producerName; }
        public void setProducerName(String producerName) { this.producerName = producerName; }
        public String getProducerNif() { return producerNif; }
        public void setProducerNif(String producerNif) { this.producerNif = producerNif; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getInstallationNumber() { return installationNumber; }
        public void setInstallationNumber(String installationNumber) { this.installationNumber = installationNumber; }
    }

    public static class Sender {
        /** Cada cuánto mira la cola de envío. */
        private long intervalMs = 60000;
        private int connectTimeoutSeconds = 15;
        private int requestTimeoutSeconds = 60;

        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int s) { this.connectTimeoutSeconds = s; }
        public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
        public void setRequestTimeoutSeconds(int s) { this.requestTimeoutSeconds = s; }
    }

    public static class Qr {
        /** Puntos por módulo del QR en la impresora (6 ≈ 36 mm a 203 dpi). Mide el resultado: 30–40 mm. */
        private int moduleSize = 6;

        public int getModuleSize() { return moduleSize; }
        public void setModuleSize(int moduleSize) { this.moduleSize = moduleSize; }
    }

    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }

    public String qrBaseUrl() {
        return isProduction() ? VerifactuQr.BASE_PRODUCCION : VerifactuQr.BASE_PRUEBAS;
    }

    /** URL del servicio SOAP según el WSDL oficial (puertos normal/sello, pruebas/producción). */
    public String soapEndpoint() {
        if (endpoint != null && !endpoint.isBlank()) return endpoint;
        String path = "/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP";
        if (isProduction()) {
            return certificate.isSeal() ? "https://www10.agenciatributaria.gob.es" + path
                    : "https://www1.agenciatributaria.gob.es" + path;
        }
        return certificate.isSeal() ? "https://prewww10.aeat.es" + path : "https://prewww1.aeat.es" + path;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getClaveRegimen() { return claveRegimen; }
    public void setClaveRegimen(String claveRegimen) { this.claveRegimen = claveRegimen; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public Certificate getCertificate() { return certificate; }
    public Software getSoftware() { return software; }
    public Sender getSender() { return sender; }
    public Qr getQr() { return qr; }
}
