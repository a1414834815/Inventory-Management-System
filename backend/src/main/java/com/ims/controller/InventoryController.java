package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.dto.InventoryVO;
import com.ims.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<List<InventoryVO>> list(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(inventoryService.list(keyword));
    }

    @GetMapping("/{productId}")
    public ApiResponse<InventoryVO> getByProductId(@PathVariable Long productId) {
        try {
            return ApiResponse.ok(inventoryService.getByProductId(productId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{productId}/safety-stock")
    public ApiResponse<Void> updateSafetyStock(@PathVariable Long productId,
                                                @RequestParam Integer safetyStock) {
        try {
            inventoryService.updateSafetyStock(productId, safetyStock);
            return ApiResponse.ok("安全库存设置成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
