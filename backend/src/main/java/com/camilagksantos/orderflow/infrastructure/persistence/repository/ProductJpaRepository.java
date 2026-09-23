package com.camilagksantos.orderflow.infrastructure.persistence.repository;

import com.camilagksantos.orderflow.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findBySku(String sku);
    List<ProductEntity> findByCategoryId(Long categoryId);
    @Query("select p from ProductEntity p join fetch p.category where p.id = :id")
    Optional<ProductEntity> findByIdWithCategory(Long id);
}