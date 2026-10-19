package es.jonay.kb.shopsystem.verifactu;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Cliente SOAP 1.1 del servicio RegFactuSistemaFacturacion de la AEAT.
 * Autenticación con certificado cualificado (PKCS12). Sin dependencias extra: usa java.net.http.
 */
@Component
public class AeatClient {

    private VerifactuProperties props;
    private volatile HttpClient client;

    @Autowired
    public void setProps(VerifactuProperties props) { this.props = props; }

    /** Envía un sobre RegFactuSistemaFacturacion y devuelve la respuesta interpretada. */
    public AeatResponse send(String envelopeXml) throws Exception {
        String soap = RecordXmlBuilder.soap(envelopeXml);
        HttpRequest request = HttpRequest.newBuilder(URI.create(props.soapEndpoint()))
                .timeout(Duration.ofSeconds(props.getSender().getRequestTimeoutSeconds()))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(soap, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = response.body();
        // Un SOAP Fault llega con HTTP 500 y cuerpo XML: se interpreta igual.
        if (body != null && body.contains("<") ) {
            return AeatResponse.parse(body);
        }
        throw new IllegalStateException("Respuesta HTTP " + response.statusCode() + " sin cuerpo XML");
    }

    private HttpClient client() throws Exception {
        HttpClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    client = buildClient();
                }
                c = client;
            }
        }
        return c;
    }

    private HttpClient buildClient() throws Exception {
        VerifactuProperties.Certificate cert = props.getCertificate();
        if (cert.getPath() == null || cert.getPath().isBlank()) {
            throw new IllegalStateException("Falta verifactu.certificate.path (certificado .p12 de la AEAT)");
        }
        char[] pass = cert.getPassword() == null ? new char[0] : cert.getPassword().toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(Path.of(cert.getPath()))) {
            ks.load(in, pass);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pass);
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), null, null); // truststore por defecto de la JVM
        return HttpClient.newBuilder()
                .sslContext(ssl)
                .connectTimeout(Duration.ofSeconds(props.getSender().getConnectTimeoutSeconds()))
                .build();
    }

    /** Fuerza recargar el certificado (p. ej. tras renovarlo). */
    public void reset() {
        client = null;
    }
}
