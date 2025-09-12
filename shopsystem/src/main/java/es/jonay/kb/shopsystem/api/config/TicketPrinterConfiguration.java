package es.jonay.kb.shopsystem.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TicketPrinterConfigurationProperties.class)
class TicketPrinterConfiguration {
}