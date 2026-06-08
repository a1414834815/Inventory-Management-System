package com.ims.controller;

import com.ims.dto.ApiResponse;
import com.ims.entity.*;
import com.ims.repository.StoreRepository;
import com.ims.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    private final ExcelService excelService;
    private final InboundService inboundService;
    private final OutboundService outboundService;
    private final ProductService productService;
    private final StoreRepository storeRepository;

    public ExcelController(ExcelService excelService,
                           InboundService inboundService,
                           OutboundService outboundService,
                           ProductService productService,
                           StoreRepository storeRepository) {
        this.excelService = excelService;
        this.inboundService = inboundService;
        this.outboundService = outboundService;
        this.productService = productService;
        this.storeRepository = storeRepository;
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam String type) {
        try {
            byte[] data = excelService.generateTemplate(type);
            String filename = switch (type) {
                case "product" -> "货品导入模板.xlsx";
                case "inbound" -> "入库导入模板.xlsx";
                case "outbound" -> "出库导入模板.xlsx";
                default -> "template.xlsx";
            };
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导入预览（不写入数据库）
     */
    @PostMapping("/import/preview")
    public ApiResponse<Map<String, Object>> importPreview(@RequestParam String type,
                                                           @RequestParam MultipartFile file) {
        try {
            Map<String, Object> result = switch (type) {
                case "product" -> excelService.importProducts(file);
                case "inbound" -> excelService.importInbounds(file);
                case "outbound" -> excelService.importOutbounds(file);
                default -> throw new IllegalArgumentException("未知导入类型");
            };
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 确认导入（写入数据库）
     */
    @PostMapping("/import/confirm")
    public ApiResponse<Map<String, Object>> importConfirm(@RequestParam String type,
                                                           @RequestParam MultipartFile file) {
        try {
            Map<String, Object> result = switch (type) {
                case "product" -> {
                    Map<String, Object> r = excelService.importProducts(file);
                    @SuppressWarnings("unchecked")
                    List<Product> products = (List<Product>) r.get("data");
                    List<Product> saved = new ArrayList<>();
                    List<String> saveErrors = new ArrayList<>();
                    for (int i = 0; i < products.size(); i++) {
                        try {
                            saved.add(productService.create(products.get(i)));
                        } catch (Exception e) {
                            saveErrors.add("第" + (i + 2) + "行: " + e.getMessage());
                        }
                    }
                    r.put("saved", saved.size());
                    r.put("saveErrors", saveErrors);
                    yield r;
                }
                case "inbound" -> {
                    Map<String, Object> r = excelService.importInbounds(file);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> records = (List<Map<String, Object>>) r.get("data");
                    int savedCount = 0;
                    List<String> saveErrors = new ArrayList<>();
                    for (int i = 0; i < records.size(); i++) {
                        try {
                            Map<String, Object> rec = records.get(i);
                            InboundOrder order = new InboundOrder();
                            Product p = new Product();
                            p.setId(((Number) rec.get("productId")).longValue());
                            order.setProduct(p);
                            order.setQuantity(((Number) rec.get("quantity")).intValue());
                            order.setUnitPrice(new BigDecimal(rec.get("unitPrice").toString()));
                            order.setSource((String) rec.get("source"));
                            order.setInboundDate(LocalDate.parse(rec.get("inboundDate").toString()));
                            order.setOperator((String) rec.get("operator"));
                            order.setRemark((String) rec.get("remark"));
                            inboundService.create(order);
                            savedCount++;
                        } catch (Exception e) {
                            saveErrors.add("第" + (i + 2) + "行: " + e.getMessage());
                        }
                    }
                    r.put("saved", savedCount);
                    r.put("saveErrors", saveErrors);
                    yield r;
                }
                case "outbound" -> {
                    Map<String, Object> r = excelService.importOutbounds(file);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> records = (List<Map<String, Object>>) r.get("data");
                    int savedCount = 0;
                    List<String> saveErrors = new ArrayList<>();
                    for (int i = 0; i < records.size(); i++) {
                        try {
                            Map<String, Object> rec = records.get(i);
                            String storeName = (String) rec.get("storeName");
                            // 查找或创建店铺
                            Store store = storeRepository.findAll().stream()
                                    .filter(s -> s.getName().equals(storeName))
                                    .findFirst()
                                    .orElseGet(() -> {
                                        Store s = new Store();
                                        s.setName(storeName);
                                        return storeRepository.save(s);
                                    });

                            OutboundOrder order = new OutboundOrder();
                            Product p = new Product();
                            p.setId(((Number) rec.get("productId")).longValue());
                            order.setProduct(p);
                            order.setStore(store);
                            order.setQuantity(((Number) rec.get("quantity")).intValue());
                            Object priceObj = rec.get("unitPrice");
                            if (priceObj != null) {
                                order.setUnitPrice(new BigDecimal(priceObj.toString()));
                            }
                            order.setOutboundDate(LocalDate.parse(rec.get("outboundDate").toString()));
                            order.setOperator((String) rec.get("operator"));
                            order.setRemark((String) rec.get("remark"));
                            outboundService.create(order);
                            savedCount++;
                        } catch (Exception e) {
                            saveErrors.add("第" + (i + 2) + "行: " + e.getMessage());
                        }
                    }
                    r.put("saved", savedCount);
                    r.put("saveErrors", saveErrors);
                    yield r;
                }
                default -> throw new IllegalArgumentException("未知导入类型");
            };
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 导出数据
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String type,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] data;
            String filename;
            switch (type) {
                case "product" -> {
                    data = excelService.exportProducts(keyword);
                    filename = "货品信息_" + today() + ".xlsx";
                }
                case "inbound" -> {
                    data = excelService.exportInbounds(keyword, startDate, endDate);
                    filename = "入库统计_" + today() + ".xlsx";
                }
                case "outbound" -> {
                    data = excelService.exportOutbounds(keyword, startDate, endDate);
                    filename = "出库统计_" + today() + ".xlsx";
                }
                default -> throw new IllegalArgumentException("未知导出类型");
            }
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
