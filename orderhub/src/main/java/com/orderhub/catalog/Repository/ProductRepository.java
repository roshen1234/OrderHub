package com.orderhub.catalog.Repository;

import com.orderhub.catalog.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

    @EntityGraph(attributePaths = "category")
    List<Product> findByActiveTrue();

    @EntityGraph(attributePaths = "category")
    Optional<Product> findByIdAndActiveTrue(Long id);

//same as above

//    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.active = true")
//    List<Product> findAllActiveWithCategory();
//
//    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id AND p.active = true")
//    Optional<Product> findActiveByIdWithCategory(@Param("id") Long id);

}
