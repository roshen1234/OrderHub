package com.orderhub;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.mysql.MySQLContainer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    // One MySQL container for the whole test run, shared by every test class.
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("orderhub_test");

    static {
        MYSQL.start();      // stopped automatically when the JVM exits
    }

    @Value("${orderhub.payment.webhook-secret}")
    protected String webhookSecret;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    protected static final String PASSWORD = "Passw0rd!123";

    protected record Customer(String email, String token, Long addressId) {}

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    protected RestClient client;

    protected record Stock(int available, int reserved) {}

    @BeforeEach
    void setUpClient() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> { })   // assert on 4xx/5xx instead of throwing
                .build();
    }

    // ---------- users ----------

    protected String register() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<Map> response = post("/register", null, Map.of("email", email, "password", PASSWORD));
        assertThat(response.getStatusCode().value()).as("register: %s", response.getBody()).isEqualTo(201);
        return email;
    }

    protected String login(String email) {
        ResponseEntity<Map> response = post("/login", null, Map.of("email", email, "password", PASSWORD));
        assertThat(response.getStatusCode().value()).as("login: %s", response.getBody()).isEqualTo(200);
        return (String) response.getBody().get("accessToken");
    }

    protected String adminToken() {
        String email = register();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);
        return login(email);                       // log in after the change: the role is inside the token
    }

    // ---------- catalog ----------

    protected long createProduct(String adminToken, int stock) {
        ResponseEntity<Map> category = post("/categories", adminToken, Map.of("name", "Cat-" + UUID.randomUUID()));
        assertThat(category.getStatusCode().value()).as("category: %s", category.getBody()).isEqualTo(201);
        long categoryId = ((Number) category.getBody().get("id")).longValue();

        ResponseEntity<Map> product = post("/products", adminToken, Map.of(
                "name", "Product-" + UUID.randomUUID(),
                "description", "Test product",
                "price", new BigDecimal("100.00"),
                "categoryId", categoryId,
                "initialStock", stock));
        assertThat(product.getStatusCode().value()).as("product: %s", product.getBody()).isEqualTo(201);
        return ((Number) product.getBody().get("id")).longValue();
    }

    // ---------- customers, cart, checkout ----------

    protected Customer customerWithAddress() {
        String email = register();
        String token = login(email);
        ResponseEntity<Map> address = post("/addresses", token, Map.of(
                "recipientName", "Test Buyer",
                "phone", "+91 98765 43210",
                "line1", "1 Test Street",
                "city", "Kochi",
                "state", "Kerala",
                "postalCode", "682001",
                "country", "IN"));
        assertThat(address.getStatusCode().value()).as("address: %s", address.getBody()).isEqualTo(201);
        return new Customer(email, token, ((Number) address.getBody().get("id")).longValue());
    }

    protected void addToCart(String token, long productId, int quantity) {
        ResponseEntity<Map> response = post("/cart/items", token, Map.of("productId", productId, "quantity", quantity));
        assertThat(response.getStatusCode().value()).as("add to cart: %s", response.getBody()).isEqualTo(201);
    }

    protected ResponseEntity<Map> checkout(Customer customer) {
        return post("/orders", customer.token(), Map.of("addressId", customer.addressId()));
    }

    // ---------- HTTP ----------

    protected ResponseEntity<Map> post(String path, String token, Object body) {
        RestClient.RequestBodySpec spec = client.post().uri(path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return spec.body(body).retrieve().toEntity(Map.class);
    }

    protected ResponseEntity<Map> get(String path, String token) {
        RestClient.RequestHeadersSpec<?> spec = client.get().uri(path);
        if (token != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return spec.retrieve().toEntity(Map.class);
    }


    protected <T> List<T> runInParallel(int threads, IntFunction<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();                       // everyone waits at the starting line
                return task.apply(index);
            }));
        }
        ready.await();
        go.countDown();                           // fire
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }

    protected ResponseEntity<Map> pay(String token, long orderId, String idempotencyKey) {
        return client.post().uri("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .body(Map.of("orderId", orderId))
                .retrieve().toEntity(Map.class);
    }


    /** Plays the gateway: sends a signed webhook to the real endpoint and returns the HTTP status. */
    protected int sendWebhook(String eventId, String type, String providerRef) {
        String body = "{\"eventId\":\"" + eventId + "\",\"type\":\"" + type
                + "\",\"providerRef\":\"" + providerRef + "\"}";
        return client.post().uri("/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", sign(body))
                .body(body)                                   // a String is sent as it is, so the signature matches
                .retrieve().toBodilessEntity().getStatusCode().value();
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** For endpoints that reply with 204 and no body. Returns only the status code. */
    protected int postStatus(String path, String token) {
        return client.post().uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().toBodilessEntity().getStatusCode().value();
    }

    protected String orderStatus(long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    /** Adds the product to the customer's cart, checks out, and returns the new order id. */
    protected long placeOrder(Customer customer, long productId, int quantity) {
        addToCart(customer.token(), productId, quantity);
        ResponseEntity<Map> order = checkout(customer);
        assertThat(order.getStatusCode().value()).as("checkout: %s", order.getBody()).isEqualTo(201);
        return ((Number) order.getBody().get("id")).longValue();
    }

    /** Starts a payment (it stays PENDING) and returns the gateway reference. */
    protected String startPayment(Customer customer, long orderId) {
        ResponseEntity<Map> payment = pay(customer.token(), orderId, "key-" + UUID.randomUUID());
        assertThat(payment.getStatusCode().value()).as("pay: %s", payment.getBody()).isEqualTo(201);
        return (String) payment.getBody().get("providerRef");
    }

    /** Starts a payment and settles it as successful through a signed webhook. */
    protected String payAndSettle(Customer customer, long orderId) {
        String providerRef = startPayment(customer, orderId);
        assertThat(sendWebhook("evt-" + UUID.randomUUID(), "payment.succeeded", providerRef)).isEqualTo(200);
        return providerRef;
    }

    protected Stock stockOf(long productId) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT available_qty, reserved_qty FROM inventory WHERE product_id = ?", productId);
        return new Stock(((Number) row.get("available_qty")).intValue(), ((Number) row.get("reserved_qty")).intValue());
    }

    protected String paymentStatus(long orderId) {
        return jdbc.queryForObject(
                "SELECT status FROM payments WHERE order_id = ? ORDER BY id DESC LIMIT 1", String.class, orderId);
    }

    protected int paymentCount(long orderId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE order_id = ?", Integer.class, orderId);
    }

    /** How many history rows moved the order INTO the given status. */
    protected int countHistory(long orderId, String toStatus) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_status_history WHERE order_id = ? AND to_status = ?",
                Integer.class, orderId, toStatus);
    }

    /** Logs in and returns the whole response body, which has accessToken and refreshToken. */
    protected Map<String, Object> loginResponse(String email) {
        ResponseEntity<Map> r = post("/login", null, Map.of("email", email, "password", PASSWORD));
        assertThat(r.getStatusCode().value()).as("login: %s", r.getBody()).isEqualTo(200);
        return r.getBody();
    }

}