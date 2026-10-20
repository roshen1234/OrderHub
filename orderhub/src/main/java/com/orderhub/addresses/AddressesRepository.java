package com.orderhub.addresses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressesRepository extends JpaRepository<Addresses,Long> {

    List<Addresses> findByUserEmailOrderByDefaultAddressDescCreatedAtDesc(String email);

    Optional<Addresses> findByIdAndUserEmail(Long id, String email);   // the owner check lives in the query

    long countByUserEmail(String email);

    @Modifying
    @Query("UPDATE Addresses a SET a.defaultAddress = false WHERE a.user.id = :userId AND a.defaultAddress = true")
    int clearDefault(@Param("userId") Long userId);
}
