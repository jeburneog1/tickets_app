package com.nequi.tickets.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
public abstract class BaseIntegrationTest extends AbstractTestContainers {
}