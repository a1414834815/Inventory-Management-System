package com.ims.service;

import com.ims.entity.*;
import com.ims.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class OutboundService {

    private final OutboundOrderRepository outboundOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public OutboundService(OutboundOrderRepository outboundOrderRepository,
                           InventoryRepository inventoryRepository,
                           ProductRepository productRepository,
                           StoreRepository storeRepository) {
        this.outboundOrderRepository = outboundOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    public Page<OutboundOrder> search(LocalDate startDate, LocalDate endDate,
                                       Long productId, Long storeId, int page, int size) {
        return outboundOrderRepository.search(startDate, endDate, productId, storeId,
                PageRequest.of(page - 1, size));
    }

    public OutboundOrder getById(Long id) {
        return outboundOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("出库单不存在"));
    }

    /**
     * 新增出库单：校验库存 → 扣减库存
     */
    @Transactional
    public OutboundOrder create(OutboundOrder order) {
        // 校验货品
        Product product = productRepository.findById(order.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("货品不存在"));
        order.setProduct(product);

        // 校验店铺
        Store store = storeRepository.findById(order.getStore().getId())
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
        order.setStore(store);

        // 校验数量
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new IllegalArgumentException("出库数量必须大于0");
        }

        // 校验库存
        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new IllegalStateException("该货品暂无库存，无法出库"));
        if (inventory.getQuantity() < order.getQuantity()) {
            throw new IllegalStateException(
                    String.format("库存不足！当前库存: %d，出库数量: %d",
                            inventory.getQuantity(), order.getQuantity()));
        }

        // 校验日期
        if (order.getOutboundDate() == null) {
            order.setOutboundDate(LocalDate.now());
        }

        // 计算总价（基于库存均价）
        if (order.getUnitPrice() == null || order.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            // 如果未指定单价，使用库存均价
            BigDecimal avgPrice = inventory.getTotalValue()
                    .divide(BigDecimal.valueOf(inventory.getQuantity()), 4, RoundingMode.HALF_UP);
            order.setUnitPrice(avgPrice);
        }
        order.setTotalPrice(order.getUnitPrice()
                .multiply(BigDecimal.valueOf(order.getQuantity())));

        // 自动生成单号
        order.setOrderNo(generateOrderNo());

        // 保存出库单
        OutboundOrder saved = outboundOrderRepository.save(order);

        // 扣减库存
        deductInventory(inventory, order.getQuantity(), order.getTotalPrice());

        return saved;
    }

    /**
     * 删除出库单并恢复库存
     */
    @Transactional
    public void delete(Long id) {
        OutboundOrder order = outboundOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("出库单不存在"));

        // 恢复库存
        restoreInventory(order.getProduct(), order.getQuantity(), order.getTotalPrice());

        outboundOrderRepository.delete(order);
    }

    /**
     * 扣减库存
     */
    private void deductInventory(Inventory inventory, int outQty, BigDecimal outValue) {
        int newQty = inventory.getQuantity() - outQty;
        BigDecimal newValue = inventory.getTotalValue().subtract(outValue);
        if (newValue.compareTo(BigDecimal.ZERO) < 0) {
            newValue = BigDecimal.ZERO;
        }
        inventory.setQuantity(newQty);
        inventory.setTotalValue(newValue);
        inventoryRepository.save(inventory);
    }

    /**
     * 恢复库存（删除出库单时）
     */
    private void restoreInventory(Product product, int outQty, BigDecimal outValue) {
        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseGet(() -> {
                    Inventory inv = new Inventory();
                    inv.setProduct(product);
                    inv.setQuantity(0);
                    inv.setTotalValue(BigDecimal.ZERO);
                    return inv;
                });

        inventory.setQuantity(inventory.getQuantity() + outQty);
        inventory.setTotalValue(inventory.getTotalValue().add(outValue));
        inventoryRepository.save(inventory);
    }

    /**
     * 生成出库单号：CK-年月日-3位序号
     */
    private String generateOrderNo() {
        String prefix = "CK-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String maxNo = outboundOrderRepository.findMaxOrderNoByPrefix(prefix + "%");
        int seq = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            try {
                seq = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {}
        }
        return prefix + String.format("%03d", seq);
    }
}
