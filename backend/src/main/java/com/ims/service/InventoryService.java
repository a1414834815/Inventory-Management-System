package com.ims.service;

import com.ims.dto.InventoryVO;
import com.ims.entity.Inventory;
import com.ims.entity.Product;
import com.ims.repository.InventoryRepository;
import com.ims.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    public List<InventoryVO> list(String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        List<Inventory> list = inventoryRepository.searchWithProduct(kw);

        // 确保所有产品都有库存记录（懒初始化）
        if (kw == null) {
            List<Product> products = productRepository.findAll();
            for (Product p : products) {
                boolean exists = list.stream().anyMatch(i -> i.getProduct().getId().equals(p.getId()));
                if (!exists) {
                    Inventory inv = new Inventory();
                    inv.setProduct(p);
                    inv.setQuantity(0);
                    inv.setTotalValue(BigDecimal.ZERO);
                    list.add(inv);
                }
            }
        }

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    public InventoryVO getByProductId(Long productId) {
        Inventory inv = inventoryRepository.findByProductIdWithProduct(productId)
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new IllegalArgumentException("货品不存在"));
                    Inventory newInv = new Inventory();
                    newInv.setProduct(product);
                    newInv.setQuantity(0);
                    newInv.setTotalValue(BigDecimal.ZERO);
                    return newInv;
                });
        return toVO(inv);
    }

    public void updateSafetyStock(Long productId, Integer safetyStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("货品不存在"));
        product.setSafetyStock(safetyStock);
        productRepository.save(product);
    }

    private InventoryVO toVO(Inventory inv) {
        Product p = inv.getProduct();
        InventoryVO vo = new InventoryVO();
        vo.setId(inv.getId());
        vo.setProductId(p.getId());
        vo.setProductCode(p.getCode());
        vo.setProductName(p.getName());
        vo.setProductSpec(p.getSpec());
        vo.setProductUnit(p.getUnit());
        vo.setProductCategory(p.getCategory());
        vo.setQuantity(inv.getQuantity());
        vo.setTotalValue(inv.getTotalValue());

        // 计算均价
        if (inv.getQuantity() > 0) {
            vo.setAvgPrice(inv.getTotalValue()
                    .divide(BigDecimal.valueOf(inv.getQuantity()), 4, RoundingMode.HALF_UP));
        } else {
            vo.setAvgPrice(BigDecimal.ZERO);
        }

        vo.setSafetyStock(p.getSafetyStock());
        // 库存预警：当前库存 <= 安全库存
        vo.setAlert(p.getSafetyStock() != null && p.getSafetyStock() > 0
                && inv.getQuantity() <= p.getSafetyStock());
        vo.setUpdatedAt(inv.getUpdatedAt());
        return vo;
    }
}
