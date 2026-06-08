package com.ims.service;

import com.ims.entity.Product;
import com.ims.repository.InboundOrderRepository;
import com.ims.repository.InventoryRepository;
import com.ims.repository.OutboundOrderRepository;
import com.ims.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(ProductRepository productRepository,
                          InboundOrderRepository inboundOrderRepository,
                          OutboundOrderRepository outboundOrderRepository,
                          InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inboundOrderRepository = inboundOrderRepository;
        this.outboundOrderRepository = outboundOrderRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public Page<Product> search(String keyword, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return productRepository.search(kw, PageRequest.of(page - 1, size));
    }

    public List<Product> listAll() {
        return productRepository.findAll();
    }

    public Optional<Product> getById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> getByCode(String code) {
        return productRepository.findByCode(code);
    }

    public Product create(Product product) {
        if (productRepository.existsByCode(product.getCode())) {
            throw new IllegalArgumentException("货品编码已存在: " + product.getCode());
        }
        return productRepository.save(product);
    }

    public Product update(Long id, Product updated) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("货品不存在"));
        if (!existing.getCode().equals(updated.getCode())
                && productRepository.existsByCode(updated.getCode())) {
            throw new IllegalArgumentException("货品编码已存在: " + updated.getCode());
        }
        existing.setCode(updated.getCode());
        existing.setName(updated.getName());
        existing.setSpec(updated.getSpec());
        existing.setUnit(updated.getUnit());
        existing.setCategory(updated.getCategory());
        existing.setSafetyStock(updated.getSafetyStock());
        existing.setRemark(updated.getRemark());
        return productRepository.save(existing);
    }

    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("货品不存在"));
        if (inboundOrderRepository.existsByProductId(id)
                || outboundOrderRepository.existsByProductId(id)) {
            throw new IllegalArgumentException("该货品存在出入库记录，无法删除");
        }
        // 同时删除库存记录
        inventoryRepository.findByProductId(id).ifPresent(inventoryRepository::delete);
        productRepository.delete(product);
    }
}
