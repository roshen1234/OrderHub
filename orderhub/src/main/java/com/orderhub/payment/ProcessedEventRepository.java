package com.orderhub.payment;

import com.orderhub.payment.entity.Payment;
import com.orderhub.payment.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    // 1 = new event, 0 = already seen. No save() involved, so a duplicate can never slip through.
    @Modifying
    @Query(value = "INSERT IGNORE INTO processed_events (event_id) VALUES (:eventId)", nativeQuery = true)
    int insertIfAbsent(@Param("eventId") String eventId);
}
