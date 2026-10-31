package com.orderhub;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DoubleClickTests extends IntegrationTestBase {

    @Test
    void doubleClickOnCheckout_createsOnlyOneOrder() throws Exception {
        long productId = createProduct(adminToken(), 10);
        Customer buyer = customerWithAddress();
        addToCart(buyer.token(), productId, 2);

        List<Integer> statuses = runInParallel(5, i -> checkout(buyer).getStatusCode().value());

        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(s -> s == 400).count()).isEqualTo(4);

        Integer orders = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders o JOIN users u ON u.id = o.user_id WHERE u.email = ?",
                Integer.class, buyer.email());
        assertThat(orders).isEqualTo(1);

        Integer reserved = jdbc.queryForObject(
                "SELECT reserved_qty FROM inventory WHERE product_id = ?", Integer.class, productId);
        assertThat(reserved).isEqualTo(2);              // reserved once, not five times
    }

    @Test
    void sameIdempotencyKeySentTwentyTimes_createsOnePayment() throws Exception {
        long orderId = orderReadyToPay();
        String buyerToken = lastBuyer.token();
        String key = "same-key-" + UUID.randomUUID();

        List<ResponseEntity<Map>> responses = runInParallel(20, i -> pay(buyerToken, orderId, key));

        assertThat(responses).allSatisfy(r -> assertThat(r.getStatusCode().value()).isEqualTo(201));
        Set<Object> paymentIds = responses.stream().map(r -> r.getBody().get("id")).collect(Collectors.toSet());
        assertThat(paymentIds).hasSize(1);              // all 20 answers describe the same payment

        assertThat(paymentsFor(orderId)).isEqualTo(1);
    }

    @Test
    void differentKeysAtTheSameTime_onlyOnePaymentIsStarted() throws Exception {
        long orderId = orderReadyToPay();
        String buyerToken = lastBuyer.token();

        List<Integer> statuses = runInParallel(10,
                i -> pay(buyerToken, orderId, "key-" + i + "-" + UUID.randomUUID()).getStatusCode().value());

        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(9);
        assertThat(paymentsFor(orderId)).isEqualTo(1);
    }

    // ---------- helpers for this class ----------

    private Customer lastBuyer;

    /** A customer with a CREATED order, ready to be paid. */
    private long orderReadyToPay() {
        long productId = createProduct(adminToken(), 10);
        lastBuyer = customerWithAddress();
        addToCart(lastBuyer.token(), productId, 1);
        ResponseEntity<Map> order = checkout(lastBuyer);
        assertThat(order.getStatusCode().value()).as("checkout: %s", order.getBody()).isEqualTo(201);
        return ((Number) order.getBody().get("id")).longValue();
    }

    private Integer paymentsFor(long orderId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE order_id = ?", Integer.class, orderId);
    }
}
