package es.jonay.kb.shopsystem;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** MySQL 8 en Docker para los tests de integración (el mismo motor que producción). */
@TestConfiguration(proxyBeanMethods = false)
public class TestDatabase {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysql() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0")).withDatabaseName("shopdb")
                .withCommand("--default-time-zone=+00:00");
    }
}
