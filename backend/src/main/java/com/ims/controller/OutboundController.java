package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.dto.PageResult;
import com.ims.entity.OutboundOrder;
import com.ims.service.OutboundService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/outbound")
public class OutboundController {

    private final OutboundService outboundService;

    public OutboundController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @GetMapping
    public ApiResponse<PageResult<OutboundOrder>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OutboundOrder> result = outboundService.search(startDate, endDate, productId, storeId, page, size);
        return ApiResponse.ok(PageResult.of(result, result.getContent()));
    }

    @GetMapping("/{id}")
    public ApiResponse<OutboundOrder> getById(@PathVariable Long id) {
        try {
            return ApiResponse.ok(outboundService.getById(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<OutboundOrder> create(@RequestBody OutboundOrder order) {
        try {
            return ApiResponse.ok("出库成功", outboundService.create(order));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            outboundService.delete(id);
            return ApiResponse.ok("出库单已删除，库存已恢复", null);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
