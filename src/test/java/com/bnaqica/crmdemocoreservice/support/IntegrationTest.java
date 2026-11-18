package com.bnaqica.crmdemocoreservice.support;

import com.bnaqica.crmdemocoreservice.client.UmsClient;
import com.bnaqica.crmdemocoreservice.client.UmsPromotionClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base for full-application tests with real PostgreSQL, Kafka and Keycloak. The UMS is mocked (it is a separate
 * service). Every subclass shares one Spring context, so the containers start once per test run; don't add
 * {@code @MockitoBean}s or properties in subclasses, or they get a context (and containers) of their own.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
public abstract class IntegrationTest {

    @MockitoBean
    protected UmsClient ums;

    @MockitoBean
    protected UmsPromotionClient umsPromotion;
}
