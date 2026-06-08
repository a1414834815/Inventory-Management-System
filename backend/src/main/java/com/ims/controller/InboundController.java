package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.dto.PageResult;
import com.ims.entity.InboundOrder;
import com.ims.service.InboundService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/inbound")
public class InboundController {

    private final InboundService inboundService;

    public InboundController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    @GetMapping
    public ApiResponse<PageResult<InboundOrder>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InboundOrder> result = inboundService.search(startDate, endDate, productId, source, page, size);
        return ApiResponse.ok(PageResult.of(result, result.getContent()));
    }

    @GetMapping("/{id}")
    public ApiResponse<InboundOrder> getById(@PathVariable Long id) {
        try {
            return ApiResponse.ok(inboundService.getById(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<InboundOrder> create(@RequestBody InboundOrder order) {
        try {
            return ApiResponse.ok("入库成功", inboundService.create(order));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            inboundService.delete(id);
            return ApiResponse.ok("入库单已删除，库存已回退", null);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
