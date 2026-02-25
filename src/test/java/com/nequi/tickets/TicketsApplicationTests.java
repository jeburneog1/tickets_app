package com.nequi.tickets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires infrastructure setup - will be enabled with TestContainers in integration tests")
class TicketsApplicationTests {
	@Test
	void contextLoads() {
	}
}