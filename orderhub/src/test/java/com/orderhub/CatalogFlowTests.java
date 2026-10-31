package com.orderhub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class CatalogFlowTests extends IntegrationTestBase {

    @Test
    void adminCreatesProduct_customerReadsIt_customerCannotCreate() {
        // an admin: register, promote in the database, log in again so the token carries the new role
        String adminEmail = register();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", adminEmail);
        String adminToken = login(adminEmail);

        ResponseEntity<Map> category = post("/categories", adminToken, Map.of("name", "Phones"));
        assertThat(category.getStatusCode().value()).as("create category: %s", category.getBody()).isEqualTo(201);
        long categoryId = ((Number) category.getBody().get("id")).longValue();

        ResponseEntity<Map> product = post("/products", adminToken, Map.of(
                "name", "iPhone 15",
                "description", "Test phone",
                "price", new BigDecimal("799.00"),
                "categoryId", categoryId,
                "initialStock", 25));
        assertThat(product.getStatusCode().value()).as("create product: %s", product.getBody()).isEqualTo(201);
        long productId = ((Number) product.getBody().get("id")).longValue();

        // anyone can read it, with no token
        ResponseEntity<Map> read = get("/products/" + productId, null);
        assertThat(read.getStatusCode().value()).isEqualTo(200);
        assertThat(read.getBody().get("name")).isEqualTo("iPhone 15");
        assertThat(new BigDecimal(read.getBody().get("price").toString())).isEqualByComparingTo("799.00");

        // creating the product also created its stock row
        Integer stock = jdbc.queryForObject(
                "SELECT available_qty FROM inventory WHERE product_id = ?", Integer.class, productId);
        assertThat(stock).isEqualTo(25);

        // a normal customer must not be able to create products
        String customerToken = login(register());
        ResponseEntity<Map> forbidden = post("/products", customerToken, Map.of(
                "name", "Hacked", "price", new BigDecimal("1.00"), "categoryId", categoryId, "initialStock", 1));
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);
    }

}