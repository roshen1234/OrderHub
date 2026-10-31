package com.orderhub;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutConcurrencyTests extends IntegrationTestBase {

    private static final int STOCK = 10;
    private static final int BUYERS = 50;

    @Test
    void fiftyBuyersCompeteForTenUnits_exactlyTenSucceed() throws Exception {
        // Arrange: one product with 10 units, and 50 customers who each have 1 unit in their cart
        String admin = adminToken();
        long productId = createProduct(admin, STOCK);

        List<Customer> buyers = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            Customer buyer = customerWithAddress();
            addToCart(buyer.token(), productId, 1);
            buyers.add(buyer);
        }

        // Act: 50 threads wait at a starting line, then all call checkout at the same moment
        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        CountDownLatch ready = new CountDownLatch(BUYERS);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>();
        for (Customer buyer : buyers) {
            futures.add(pool.submit(() -> {
                ready.countDown();          // "I'm at the starting line"
                go.await();                 // wait for the starting gun
                return checkout(buyer).getStatusCode().value();
            }));
        }
        ready.await();                      // wait until all 50 threads are at the line
        go.countDown();                     // fire

        Map<Integer, Long> results = new HashMap<>();
        for (Future<Integer> future : futures) {
            results.merge(future.get(60, TimeUnit.SECONDS), 1L, Long::sum);
        }
        pool.shutdown();

        // Assert: exactly 10 orders, 40 refusals, and the stock numbers add up
        assertThat(results).isEqualTo(Map.of(201, 10L, 409, 40L));

        Map<String, Object> stock = jdbc.queryForMap(
                "SELECT available_qty, reserved_qty FROM inventory WHERE product_id = ?", productId);
        assertThat(((Number) stock.get("available_qty")).intValue()).isZero();
        assertThat(((Number) stock.get("reserved_qty")).intValue()).isEqualTo(STOCK);

        Integer orders = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT order_id) FROM order_items WHERE product_id = ?", Integer.class, productId);
        assertThat(orders).isEqualTo(STOCK);
    }
}
