package com.orderhub;

import com.orderhub.order.OrderService;
import com.orderhub.payment.PaymentResultService;
import com.orderhub.payment.paymentGateway.GatewayStatus;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RaceConditionTests extends IntegrationTestBase{

    @Autowired
    OrderService orderService;

    @Autowired
    PaymentResultService paymentResultService;

    //Checkout sorts items by product ID before reserving stock, so two customers who buy the same products in opposite order don’t lock each other in a circle.
    @RepeatedTest(5)
    void oppositeCartOrder_neverDeadlocks() throws Exception {
        String admin = adminToken();
        long p1 = createProduct(admin, 100);          // created first, so p1 < p2
        long p2 = createProduct(admin, 100);

        int buyers = 20;
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            Customer c = customerWithAddress();
            if (i % 2 == 0) {                         // even customers: p1 then p2
                addToCart(c.token(), p1, 1);
                addToCart(c.token(), p2, 1);
            } else {                                  // odd customers: p2 then p1
                addToCart(c.token(), p2, 1);
                addToCart(c.token(), p1, 1);
            }
            customers.add(c);
        }

        List<Integer> statuses = runInParallel(buyers, i -> checkout(customers.get(i)).getStatusCode().value());

        assertThat(statuses).containsOnly(201);       // stock is plentiful, so nobody should be refused
        assertThat(stockOf(p1)).isEqualTo(new Stock(80, 20));
        assertThat(stockOf(p2)).isEqualTo(new Stock(80, 20));
    }

    //A payment starts, then the customer cancels at the same moment the gateway reports success. Whichever happens first, the stock comes back once, the order ends up CANCELLED, and the money is queued for a refund.
    @RepeatedTest(5)
    void cancelWhilePaymentSucceeds_orderCancelled_stockReleasedOnce_paymentRefunded() throws Exception {
        long productId = createProduct(adminToken(), 10);
        Customer buyer = customerWithAddress();
        long orderId = placeOrder(buyer, productId, 2);
        String providerRef = startPayment(buyer, orderId);            // payment PENDING, order CREATED

        List<Integer> results = runInParallel(2, i -> i == 0
                ? postStatus("/orders/" + orderId + "/cancel", buyer.token())
                : sendWebhook("evt-" + UUID.randomUUID(), "payment.succeeded", providerRef));

        assertThat(results.get(0)).isEqualTo(204);       // the cancel works whether the order was CREATED or already PAID
        assertThat(results.get(1)).isEqualTo(200);       // the webhook is always acknowledged

        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(stockOf(productId)).isEqualTo(new Stock(10, 0));       // released exactly once, not twice
        assertThat(paymentStatus(orderId)).isIn("REFUND_PENDING", "REFUNDED");   // the refund job may already have run
        assertThat(countHistory(orderId, "CANCELLED")).isEqualTo(1);
    }

    //The deadline check inside the status update. A payment that arrives after expiry can’t make the order PAID, and the money goes back.
    @RepeatedTest(5)
    void paymentSucceedsAfterExpiry_orderIsCancelled_andPaymentIsRefunded() throws Exception {
        long productId = createProduct(adminToken(), 10);
        Customer buyer = customerWithAddress();
        long orderId = placeOrder(buyer, productId, 2);
        String providerRef = startPayment(buyer, orderId);            // payment PENDING while the order is still valid

        // the payment window closes: move the deadline into the past
        jdbc.update("UPDATE orders SET expires_at = ? WHERE id = ?", LocalDateTime.now().minusDays(1), orderId);

        // the expiry job and the gateway's success webhook arrive at the same moment
        List<Integer> results = runInParallel(2, i -> {
            if (i == 0) {
                orderService.cancelExpired(orderId);      // what the scheduled job does for an expired order
                return 0;
            }
            return sendWebhook("evt-" + UUID.randomUUID(), "payment.succeeded", providerRef);
        });
        assertThat(results.get(1)).isEqualTo(200);

        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(stockOf(productId)).isEqualTo(new Stock(10, 0));
        assertThat(paymentStatus(orderId)).isIn("REFUND_PENDING", "REFUNDED");
        assertThat(countHistory(orderId, "CANCELLED")).isEqualTo(1);  // cancelled once
        assertThat(countHistory(orderId, "PAID")).isZero();           // never marked paid
    }

    //The processed_events table. A gateway that resends the same event many times, even simultaneously, causes one change.
    @RepeatedTest(5)
    void sameWebhookEventSentTwentyTimes_isProcessedOnce() throws Exception {
        long productId = createProduct(adminToken(), 10);
        Customer buyer = customerWithAddress();
        long orderId = placeOrder(buyer, productId, 1);
        String providerRef = startPayment(buyer, orderId);
        String eventId = "evt-" + UUID.randomUUID();

        List<Integer> statuses = runInParallel(20, i -> sendWebhook(eventId, "payment.succeeded", providerRef));

        assertThat(statuses).containsOnly(200);          // duplicates are acknowledged, not rejected
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
        assertThat(paymentStatus(orderId)).isEqualTo("SUCCESS");
        assertThat(paymentCount(orderId)).isEqualTo(1);
        assertThat(countHistory(orderId, "PAID")).isEqualTo(1);       // one status change recorded, not twenty

        Integer processed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ?", Integer.class, eventId);
        assertThat(processed).isEqualTo(1);
    }

    //The two ways a payment gets settled, the webhook and the reconciliation job, can arrive together without applying the result twice.
    @RepeatedTest(5)
    void webhookAndReconciliationForTheSamePayment_applyTheResultOnce() throws Exception {
        long productId = createProduct(adminToken(), 10);
        Customer buyer = customerWithAddress();
        long orderId = placeOrder(buyer, productId, 1);
        String providerRef = startPayment(buyer, orderId);
        long paymentId = jdbc.queryForObject("SELECT id FROM payments WHERE order_id = ?", Long.class, orderId);

        List<Integer> results = runInParallel(2, i -> {
            if (i == 0) {
                return sendWebhook("evt-" + UUID.randomUUID(), "payment.succeeded", providerRef);
            }
            paymentResultService.reconcile(paymentId, GatewayStatus.PAID);    // what the job calls for a pending payment
            return 200;
        });

        assertThat(results).containsOnly(200);           // an exception in reconcile would fail the test first
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
        assertThat(paymentStatus(orderId)).isEqualTo("SUCCESS");
        assertThat(countHistory(orderId, "PAID")).isEqualTo(1);
    }

    //Token rotation. A refresh token works once, and parallel reuse can’t produce several new tokens.
    @RepeatedTest(5)
    void sameRefreshTokenUsedInParallel_oneSucceeds() throws Exception {
        String email = register();
        String refreshToken = (String) loginResponse(email).get("refreshToken");

        List<Integer> statuses = runInParallel(10, i ->
                post("/refresh", null, Map.of("refreshToken", refreshToken)).getStatusCode().value());

        System.out.print("testing returned data"+statuses.toString());

        assertThat(statuses.stream().filter(s -> s == 200).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(s -> s == 401).count()).isEqualTo(9);

        // rows for this user: the one from login, plus exactly one created by the winning refresh
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens rt JOIN users u ON u.id = rt.user_id WHERE u.email = ?",
                Integer.class, email);
        assertThat(total).isEqualTo(2);

        // the original token is revoked, and at most one token is still usable
        Integer active = jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens rt JOIN users u ON u.id = rt.user_id " +
                        "WHERE u.email = ? AND rt.revoked = false", Integer.class, email);
        assertThat(active).isLessThanOrEqualTo(1);
    }

    //Stock is released once, and the refund is queued once, however many times the cancel arrives.
    @RepeatedTest(5)
    void cancelSentFiveTimesAtOnce_releasesStockOnce_andQueuesOneRefund() throws Exception {
        long productId = createProduct(adminToken(), 10);
        Customer buyer = customerWithAddress();
        long orderId = placeOrder(buyer, productId, 2);
        payAndSettle(buyer, orderId);

        List<Integer> statuses = runInParallel(5, i -> postStatus("/orders/" + orderId + "/cancel", buyer.token()));

        assertThat(statuses).containsExactlyInAnyOrder(204, 409, 409, 409, 409);
        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(stockOf(productId)).isEqualTo(new Stock(10, 0));       // 10, not 18
        assertThat(paymentCount(orderId)).isEqualTo(1);
        assertThat(paymentStatus(orderId)).isIn("REFUND_PENDING", "REFUNDED");
        assertThat(countHistory(orderId, "CANCELLED")).isEqualTo(1);
    }
}
