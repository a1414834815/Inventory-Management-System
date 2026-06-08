package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.dto.PageResult;
import com.ims.entity.Product;
import com.ims.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResult<Product>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Product> result = productService.search(keyword, page, size);
        return ApiResponse.ok(PageResult.of(result, result.getContent()));
    }

    @GetMapping("/all")
    public ApiResponse<List<Product>> listAll() {
        return ApiResponse.ok(productService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getById(@PathVariable Long id) {
        return productService.getById(id)
                .map(ApiResponse::ok)
                .orElse(ApiResponse.fail("货品不存在"));
    }

    @PostMapping
    public ApiResponse<Product> create(@RequestBody Product product) {
        try {
            return ApiResponse.ok("货品添加成功", productService.create(product));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @RequestBody Product product) {
        try {
            return ApiResponse.ok("货品更新成功", productService.update(id, product));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ApiResponse.ok("货品删除成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
