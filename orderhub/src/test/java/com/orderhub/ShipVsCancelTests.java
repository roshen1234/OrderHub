package com.orderhub;

import org.junit.jupiter.api.RepeatedTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShipVsCancelTests extends IntegrationTestBase {

    @RepeatedTest(5)
    void shipAndCancelAtTheSameTime_exactlyOneWins() throws Exception {
        // Arrange: a PAID order for 2 units out of 10
        String admin = adminToken();
        long productId = createProduct(admin, 10);
        Customer buyer = customerWithAddress();
        addToCart(buyer.token(), productId, 2);
        long orderId = ((Number) checkout(buyer).getBody().get("id")).longValue();
        payAndSettle(buyer, orderId);
        assertThat(orderStatus(orderId)).isEqualTo("PAID");

        // Act: the admin ships and the customer cancels, at the same moment
        List<Integer> results = runInParallel(2, i -> i == 0
                ? postStatus("/admin/orders/" + orderId + "/ship", admin)
                : postStatus("/orders/" + orderId + "/cancel", buyer.token()));
        int shipStatus = results.get(0);
        int cancelStatus = results.get(1);

        // Assert: exactly one of them won
        assertThat(List.of(shipStatus, cancelStatus))
                .as("shipStatus %s and cancelStatus %s", shipStatus, cancelStatus)
                .containsExactlyInAnyOrder(204, 409);

        Map<String, Object> stock = jdbc.queryForMap(
                "SELECT available_qty, reserved_qty FROM inventory WHERE product_id = ?", productId);
        int available = ((Number) stock.get("available_qty")).intValue();
        int reserved = ((Number) stock.get("reserved_qty")).intValue();
        String paymentStatus = jdbc.queryForObject(
                "SELECT status FROM payments WHERE order_id = ?", String.class, orderId);

        if (shipStatus == 204) {
            assertThat(orderStatus(orderId)).isEqualTo("SHIPPED");
            assertThat(available).isEqualTo(8);          // the 2 units left the warehouse
            assertThat(reserved).isZero();
            assertThat(paymentStatus).isEqualTo("SUCCESS");
        } else {
            assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
            assertThat(available).isEqualTo(10);         // the 2 units went back on sale
            assertThat(reserved).isZero();
            assertThat(paymentStatus).isIn("REFUND_PENDING", "REFUNDED");   // the refund job may already have run
        }

        Integer historyRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_status_history WHERE order_id = ? AND from_status = 'PAID'",
                Integer.class, orderId);
        assertThat(historyRows).isEqualTo(1);            // only one change out of PAID was recorded
    }
}