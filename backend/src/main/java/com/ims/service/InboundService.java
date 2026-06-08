package com.ims.service;

import com.ims.entity.InboundOrder;
import com.ims.entity.Inventory;
import com.ims.entity.Product;
import com.ims.repository.InboundOrderRepository;
import com.ims.repository.InventoryRepository;
import com.ims.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class InboundService {

    private final InboundOrderRepository inboundOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InboundService(InboundOrderRepository inboundOrderRepository,
                          InventoryRepository inventoryRepository,
                          ProductRepository productRepository) {
        this.inboundOrderRepository = inboundOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    public Page<InboundOrder> search(LocalDate startDate, LocalDate endDate,
                                      Long productId, String source, int page, int size) {
        String src = (source == null || source.isBlank()) ? null : source.trim();
        return inboundOrderRepository.search(startDate, endDate, productId, src,
                PageRequest.of(page - 1, size));
    }

    public InboundOrder getById(Long id) {
        return inboundOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("入库单不存在"));
    }

    /**
     * 新增入库单并更新库存（移动加权平均法）
     */
    @Transactional
    public InboundOrder create(InboundOrder order) {
        // 校验货品
        Product product = productRepository.findById(order.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("货品不存在"));
        order.setProduct(product);

        // 校验数量
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new IllegalArgumentException("入库数量必须大于0");
        }

        // 校验日期
        if (order.getInboundDate() == null) {
            order.setInboundDate(LocalDate.now());
        }

        // 计算总价
        if (order.getUnitPrice() == null) {
            order.setUnitPrice(BigDecimal.ZERO);
        }
        order.setTotalPrice(order.getUnitPrice()
                .multiply(BigDecimal.valueOf(order.getQuantity())));

        // 自动生成单号
        order.setOrderNo(generateOrderNo());

        // 保存入库单
        InboundOrder saved = inboundOrderRepository.save(order);

        // 更新库存（移动加权平均）
        updateInventoryForInbound(product, order.getQuantity(), order.getTotalPrice());

        return saved;
    }

    /**
     * 删除入库单并回退库存
     */
    @Transactional
    public void delete(Long id) {
        InboundOrder order = inboundOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("入库单不存在"));

        // 回退库存
        rollbackInventoryForInbound(order.getProduct(), order.getQuantity(), order.getTotalPrice());

        inboundOrderRepository.delete(order);
    }

    /**
     * 入库更新库存：移动加权平均法
     */
    private void updateInventoryForInbound(Product product, int inboundQty, BigDecimal inboundValue) {
        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseGet(() -> {
                    Inventory inv = new Inventory();
                    inv.setProduct(product);
                    inv.setQuantity(0);
                    inv.setTotalValue(BigDecimal.ZERO);
                    return inv;
                });

        int newQty = inventory.getQuantity() + inboundQty;
        BigDecimal newValue = inventory.getTotalValue().add(inboundValue);

        inventory.setQuantity(newQty);
        inventory.setTotalValue(newValue);
        inventoryRepository.save(inventory);
    }

    /**
     * 删除入库单时回退库存
     */
    private void rollbackInventoryForInbound(Product product, int inboundQty, BigDecimal inboundValue) {
        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new IllegalStateException("库存记录不存在，无法回退"));

        int newQty = inventory.getQuantity() - inboundQty;
        if (newQty < 0) {
            throw new IllegalStateException("库存不足，无法回退入库（可能存在关联出库）");
        }
        BigDecimal newValue = inventory.getTotalValue().subtract(inboundValue);

        inventory.setQuantity(newQty);
        inventory.setTotalValue(newValue.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newValue);
        inventoryRepository.save(inventory);
    }

    /**
     * 生成入库单号：RK-年月日-3位序号
     */
    private String generateOrderNo() {
        String prefix = "RK-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String maxNo = inboundOrderRepository.findMaxOrderNoByPrefix(prefix + "%");
        int seq = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            try {
                seq = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {}
        }
        return prefix + String.format("%03d", seq);
    }
}
