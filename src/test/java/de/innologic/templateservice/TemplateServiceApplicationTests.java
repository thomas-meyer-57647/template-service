package de.innologic.templateservice;

import de.innologic.templateservice.support.AbstractMariaDbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TemplateServiceApplicationTests extends AbstractMariaDbIntegrationTest {

	@Test
	void contextLoads() {
	}

}
