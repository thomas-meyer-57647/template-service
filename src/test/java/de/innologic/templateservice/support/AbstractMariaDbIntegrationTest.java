package de.innologic.templateservice.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractMariaDbIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.4")
        .withDatabaseName("template")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.flyway.url", MARIADB::getJdbcUrl);
        registry.add("spring.flyway.user", MARIADB::getUsername);
        registry.add("spring.flyway.password", MARIADB::getPassword);
    }
}
