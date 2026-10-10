package com.orderhub.catalog.Repository;

import com.orderhub.catalog.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long>, JpaSpecificationExecutor<Product> {

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

    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

}
