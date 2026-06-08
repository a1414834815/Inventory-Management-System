package com.ims.repository;

import com.ims.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.product p " +
           "WHERE (:keyword IS NULL OR p.name LIKE %:keyword% OR p.code LIKE %:keyword%) " +
           "ORDER BY p.code")
    List<Inventory> searchWithProduct(@Param("keyword") String keyword);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.product p WHERE p.id = :productId")
    Optional<Inventory> findByProductIdWithProduct(@Param("productId") Long productId);
}
