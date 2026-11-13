package com.ecommerce.procurement.ucp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Legacy procurement (/api/procurement) tenant isolation (B-02). Its SQL uses INSERT ... RETURNING, which H2 does not
 * support, so it runs only against PostgreSQL: UCP_TEST_PROCUREMENT_POSTGRES_URL must name a "ucp_scratch_" database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "UCP_TEST_PROCUREMENT_POSTGRES_URL", matches = "jdbc:postgresql://.+/ucp_scratch_[A-Za-z0-9_]+")
class LegacyProcurementTenantPostgresTest {
    static final UUID A = UUID.randomUUID();
    static final UUID B = UUID.randomUUID();

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("UCP_TEST_PROCUREMENT_POSTGRES_URL"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("UCP_TEST_POSTGRES_USER", "postgres"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("UCP_TEST_POSTGRES_PASSWORD", ""));
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired MockMvc mvc;
    @MockBean InventoryClient inventory;
    @MockBean AccountingClient accounting;
    @MockBean UcpAuthorizationClient authorization;
    @Value("${jwt.secret}") String secret;

    String token(UUID tenant, String email, Set<String> perms, String app) {
        Date now = new Date();
        return "Bearer " + Jwts.builder().setClaims(Map.of("id", 3L, "tenant_role", "STAFF", "tenant_id", tenant.toString(), "application_code", app, "permissions", perms))
                .setSubject(email).setId(UUID.randomUUID().toString()).setIssuer("universal-commerce").setAudience("commerce-api").setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 600_000)).signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256).compact();
    }

    @Test void legacyProcurementIsTenantScoped() throws Exception {
        String managerA = token(A, "a-mgr@x.test", Set.of("procurement:read", "procurement:manage"), "ECOMMERCE_ADMIN");
        String managerB = token(B, "b-mgr@x.test", Set.of("procurement:read", "procurement:manage"), "ECOMMERCE_ADMIN");
        String code = "LEG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String created = mvc.perform(MockMvcRequestBuilders.post("/api/procurement/suppliers").header("Authorization", managerA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"supplierCode\":\"" + code + "\",\"name\":\"Legacy A\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long supplier = new ObjectMapper().readTree(created).get("id").asLong();

        String suppliersB = mvc.perform(MockMvcRequestBuilders.get("/api/procurement/suppliers").header("Authorization", managerB))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(suppliersB.contains(code), "tenant B must not list tenant A's suppliers");
        mvc.perform(MockMvcRequestBuilders.put("/api/procurement/suppliers/" + supplier).header("Authorization", managerB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"hijacked\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.delete("/api/procurement/suppliers/" + supplier).header("Authorization", managerB))
                .andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.get("/api/procurement/suppliers/" + supplier + "/products").header("Authorization", managerB))
                .andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.post("/api/procurement/purchase-orders").header("Authorization", managerB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"supplierId\":" + supplier + ",\"lines\":[{\"supplierProductId\":1,\"quantity\":1}]}"))
                .andExpect(status().isNotFound());

        String suppliersA = mvc.perform(MockMvcRequestBuilders.get("/api/procurement/suppliers").header("Authorization", managerA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(suppliersA.contains(code));
        assertTrue(suppliersA.contains("Legacy A"), "tenant B's update attempt must not have changed the record");
    }

    @Test void supplierCodesAreUniquePerTenant() throws Exception {
        String code = "DUP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        for (UUID tenant : List.of(A, B)) {
            mvc.perform(MockMvcRequestBuilders.post("/api/procurement/suppliers")
                            .header("Authorization", token(tenant, "m@x.test", Set.of("procurement:manage"), "ECOMMERCE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"supplierCode\":\"" + code + "\",\"name\":\"Same\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(MockMvcRequestBuilders.post("/api/procurement/suppliers")
                        .header("Authorization", token(A, "m@x.test", Set.of("procurement:manage"), "ECOMMERCE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"supplierCode\":\"" + code + "\",\"name\":\"Again\"}"))
                .andExpect(status().isConflict());
    }
}
