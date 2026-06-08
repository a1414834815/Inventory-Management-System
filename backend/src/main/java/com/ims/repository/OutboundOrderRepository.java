package com.ims.repository;

import com.ims.entity.OutboundOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {

    @Query("SELECT o FROM OutboundOrder o JOIN o.product p JOIN o.store s WHERE " +
           "(:startDate IS NULL OR o.outboundDate >= :startDate) AND " +
           "(:endDate IS NULL OR o.outboundDate <= :endDate) AND " +
           "(:productId IS NULL OR p.id = :productId) AND " +
           "(:storeId IS NULL OR s.id = :storeId) " +
           "ORDER BY o.outboundDate DESC, o.id DESC")
    Page<OutboundOrder> search(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate,
                                @Param("productId") Long productId,
                                @Param("storeId") Long storeId,
                                Pageable pageable);

    boolean existsByProductId(Long productId);
    boolean existsByStoreId(Long storeId);

    @Query("SELECT MAX(o.orderNo) FROM OutboundOrder o WHERE o.orderNo LIKE :prefix%")
    String findMaxOrderNoByPrefix(@Param("prefix") String prefix);
}
