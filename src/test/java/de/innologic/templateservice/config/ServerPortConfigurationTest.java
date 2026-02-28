package de.innologic.templateservice.config;

import de.innologic.templateservice.support.AbstractMariaDbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ServerPortConfigurationTest extends AbstractMariaDbIntegrationTest {

    @Value("${server.port}")
    private String serverPort;

    @Test
    void defaultsTo8104() {
        assertEquals("8104", serverPort);
    }
}
