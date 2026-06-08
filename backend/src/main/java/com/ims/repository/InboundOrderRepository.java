package com.ims.repository;

import com.ims.entity.InboundOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {

    @Query("SELECT i FROM InboundOrder i JOIN i.product p WHERE " +
           "(:startDate IS NULL OR i.inboundDate >= :startDate) AND " +
           "(:endDate IS NULL OR i.inboundDate <= :endDate) AND " +
           "(:productId IS NULL OR p.id = :productId) AND " +
           "(:source IS NULL OR i.source LIKE %:source%) " +
           "ORDER BY i.inboundDate DESC, i.id DESC")
    Page<InboundOrder> search(@Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               @Param("productId") Long productId,
                               @Param("source") String source,
                               Pageable pageable);

    boolean existsByProductId(Long productId);

    @Query("SELECT MAX(i.orderNo) FROM InboundOrder i WHERE i.orderNo LIKE :prefix%")
    String findMaxOrderNoByPrefix(@Param("prefix") String prefix);
}
