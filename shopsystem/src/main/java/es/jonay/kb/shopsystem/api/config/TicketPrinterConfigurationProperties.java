package es.jonay.kb.shopsystem.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ConfigurationProperties(prefix = "ticket-printer")
public class TicketPrinterConfigurationProperties {
    @NotBlank
    @Pattern(regexp = "[\\d+\\\\.]+")
    private String ip;

    @NotBlank
    private Integer port;

    @NotBlank
    private String street;

    @NotBlank
    private String businessName;

    @NotBlank
    private String nif;

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return this.port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getStreet() {
        return this.street;
    }

    public String getBusinessName() {
        return this.businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getNif() {
        return this.nif;
    }
    public void setNif(String nif) {
        this.nif = nif;
    }
}