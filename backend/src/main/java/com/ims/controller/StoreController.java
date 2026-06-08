package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.entity.Store;
import com.ims.service.StoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ApiResponse<List<Store>> listAll() {
        return ApiResponse.ok(storeService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Store> getById(@PathVariable Long id) {
        return storeService.getById(id)
                .map(ApiResponse::ok)
                .orElse(ApiResponse.fail("店铺不存在"));
    }

    @PostMapping
    public ApiResponse<Store> create(@RequestBody Store store) {
        return ApiResponse.ok("店铺添加成功", storeService.create(store));
    }

    @PutMapping("/{id}")
    public ApiResponse<Store> update(@PathVariable Long id, @RequestBody Store store) {
        try {
            return ApiResponse.ok("店铺更新成功", storeService.update(id, store));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            storeService.delete(id);
            return ApiResponse.ok("店铺删除成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
