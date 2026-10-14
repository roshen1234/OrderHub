package com.orderhub.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    @Modifying
    @Query("UPDATE Inventory i SET i.availableQty = i.availableQty + :delta " +
            "WHERE i.productId = :id AND i.availableQty + :delta >= 0")
    int adjust(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Inventory i SET i.availableQty = i.availableQty - :qty, i.reservedQty = i.reservedQty + :qty " +
            "WHERE i.productId = :id AND i.availableQty >= :qty")
    int reserve(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Inventory i SET i.availableQty = i.availableQty + :qty, i.reservedQty = i.reservedQty - :qty " +
            "WHERE i.productId = :id AND i.reservedQty >= :qty")
    int release(@Param("id") Long id, @Param("qty") int qty);

}
