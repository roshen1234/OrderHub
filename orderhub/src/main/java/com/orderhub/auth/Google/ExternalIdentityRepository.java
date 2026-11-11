package com.orderhub.auth.Google;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<ExternalIdentity> findByProviderAndSubject(String provider, String subject);
}
