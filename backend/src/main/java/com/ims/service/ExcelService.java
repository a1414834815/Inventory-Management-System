package com.ims.service;

import com.ims.entity.*;
import com.ims.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelService {

    private final ProductRepository productRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;

    public ExcelService(ProductRepository productRepository,
                        InboundOrderRepository inboundOrderRepository,
                        OutboundOrderRepository outboundOrderRepository) {
        this.productRepository = productRepository;
        this.inboundOrderRepository = inboundOrderRepository;
        this.outboundOrderRepository = outboundOrderRepository;
    }

    // ==================== 模板下载 ====================

    public byte[] generateTemplate(String type) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet;
            switch (type) {
                case "product":
                    sheet = workbook.createSheet("货品信息");
                    createProductTemplate(sheet, workbook);
                    break;
                case "inbound":
                    sheet = workbook.createSheet("入库统计");
                    createInboundTemplate(sheet, workbook);
                    break;
                case "outbound":
                    sheet = workbook.createSheet("出库统计");
                    createOutboundTemplate(sheet, workbook);
                    break;
                default:
                    throw new IllegalArgumentException("未知模板类型: " + type);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private void createProductTemplate(Sheet sheet, XSSFWorkbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle requiredStyle = createRequiredStyle(workbook);

        Row header = sheet.createRow(0);
        String[] headers = {"货品编码*", "货品名称*", "规格", "单位", "分类", "安全库存", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headers[i].contains("*") ? requiredStyle : headerStyle);
        }

        // 示例数据
        Row example = sheet.createRow(1);
        example.createCell(0).setCellValue("P001");
        example.createCell(1).setCellValue("示例货品");
        example.createCell(2).setCellValue("A款-大号");
        example.createCell(3).setCellValue("个");
        example.createCell(4).setCellValue("日用品");
        example.createCell(5).setCellValue("50");
        example.createCell(6).setCellValue("示例备注");

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createInboundTemplate(Sheet sheet, XSSFWorkbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle requiredStyle = createRequiredStyle(workbook);

        Row header = sheet.createRow(0);
        String[] headers = {"货品编码*", "入库数量*", "进货单价*", "进货源", "入库日期*", "操作员", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headers[i].contains("*") ? requiredStyle : headerStyle);
        }

        Row example = sheet.createRow(1);
        example.createCell(0).setCellValue("P001");
        example.createCell(1).setCellValue("100");
        example.createCell(2).setCellValue("25.50");
        example.createCell(3).setCellValue("XX供应商");
        example.createCell(4).setCellValue("2026-06-09");
        example.createCell(5).setCellValue("张三");
        example.createCell(6).setCellValue("示例备注");

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createOutboundTemplate(Sheet sheet, XSSFWorkbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle requiredStyle = createRequiredStyle(workbook);

        Row header = sheet.createRow(0);
        String[] headers = {"货品编码*", "店铺名称*", "出库数量*", "出货单价", "出库日期*", "操作员", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headers[i].contains("*") ? requiredStyle : headerStyle);
        }

        Row example = sheet.createRow(1);
        example.createCell(0).setCellValue("P001");
        example.createCell(1).setCellValue("XX店铺");
        example.createCell(2).setCellValue("20");
        example.createCell(3).setCellValue("30.00");
        example.createCell(4).setCellValue("2026-06-09");
        example.createCell(5).setCellValue("李四");
        example.createCell(6).setCellValue("示例备注");

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ==================== 导入解析 ====================

    public Map<String, Object> importProducts(MultipartFile file) throws IOException {
        List<Product> products = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String code = getCellString(row, 0);
                    String name = getCellString(row, 1);
                    if (code == null || code.isBlank()) {
                        errors.add("第" + (i + 1) + "行: 货品编码不能为空");
                        continue;
                    }
                    if (name == null || name.isBlank()) {
                        errors.add("第" + (i + 1) + "行: 货品名称不能为空");
                        continue;
                    }

                    Product p = new Product();
                    p.setCode(code.trim());
                    p.setName(name.trim());
                    p.setSpec(getCellString(row, 2));
                    p.setUnit(getCellString(row, 3));
                    p.setCategory(getCellString(row, 4));
                    String safetyStock = getCellString(row, 5);
                    p.setSafetyStock(safetyStock != null && !safetyStock.isBlank()
                            ? Integer.parseInt(safetyStock.trim()) : 0);
                    p.setRemark(getCellString(row, 6));
                    products.add(p);
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", products);
        result.put("total", products.size());
        result.put("errors", errors);
        return result;
    }

    public Map<String, Object> importInbounds(MultipartFile file) throws IOException {
        List<Map<String, Object>> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String code = getCellString(row, 0);
                    String qtyStr = getCellString(row, 1);
                    String priceStr = getCellString(row, 2);
                    String source = getCellString(row, 3);
                    String dateStr = getCellString(row, 4);
                    String operator = getCellString(row, 5);
                    String remark = getCellString(row, 6);

                    if (code == null || code.isBlank()) {
                        errors.add("第" + (i + 1) + "行: 货品编码不能为空");
                        continue;
                    }

                    Product product = productRepository.findByCode(code.trim()).orElse(null);
                    if (product == null) {
                        errors.add("第" + (i + 1) + "行: 货品编码 " + code + " 不存在");
                        continue;
                    }

                    Map<String, Object> record = new HashMap<>();
                    record.put("productId", product.getId());
                    record.put("productCode", product.getCode());
                    record.put("productName", product.getName());
                    record.put("quantity", Integer.parseInt(qtyStr));
                    record.put("unitPrice", new BigDecimal(priceStr));
                    record.put("source", source);
                    record.put("inboundDate", LocalDate.parse(dateStr));
                    record.put("operator", operator);
                    record.put("remark", remark);
                    records.add(record);
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", records);
        result.put("total", records.size());
        result.put("errors", errors);
        return result;
    }

    public Map<String, Object> importOutbounds(MultipartFile file) throws IOException {
        List<Map<String, Object>> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String code = getCellString(row, 0);
                    String storeName = getCellString(row, 1);
                    String qtyStr = getCellString(row, 2);
                    String priceStr = getCellString(row, 3);
                    String dateStr = getCellString(row, 4);
                    String operator = getCellString(row, 5);
                    String remark = getCellString(row, 6);

                    if (code == null || code.isBlank()) {
                        errors.add("第" + (i + 1) + "行: 货品编码不能为空");
                        continue;
                    }
                    if (storeName == null || storeName.isBlank()) {
                        errors.add("第" + (i + 1) + "行: 店铺名称不能为空");
                        continue;
                    }

                    Product product = productRepository.findByCode(code.trim()).orElse(null);
                    if (product == null) {
                        errors.add("第" + (i + 1) + "行: 货品编码 " + code + " 不存在");
                        continue;
                    }

                    Map<String, Object> record = new HashMap<>();
                    record.put("productId", product.getId());
                    record.put("productCode", product.getCode());
                    record.put("productName", product.getName());
                    record.put("storeName", storeName.trim());
                    record.put("quantity", Integer.parseInt(qtyStr));
                    record.put("unitPrice", priceStr != null && !priceStr.isBlank()
                            ? new BigDecimal(priceStr) : null);
                    record.put("outboundDate", LocalDate.parse(dateStr));
                    record.put("operator", operator);
                    record.put("remark", remark);
                    records.add(record);
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", records);
        result.put("total", records.size());
        result.put("errors", errors);
        return result;
    }

    // ==================== 导出 ====================

    public byte[] exportProducts(String keyword) throws IOException {
        List<Product> products;
        if (keyword != null && !keyword.isBlank()) {
            products = productRepository.search(keyword.trim(), null).getContent();
        } else {
            products = productRepository.findAll();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("货品信息");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row header = sheet.createRow(0);
            String[] headers = {"货品编码", "货品名称", "规格", "单位", "分类", "安全库存", "备注", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getCode());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getSpec() != null ? p.getSpec() : "");
                row.createCell(3).setCellValue(p.getUnit() != null ? p.getUnit() : "");
                row.createCell(4).setCellValue(p.getCategory() != null ? p.getCategory() : "");
                row.createCell(5).setCellValue(p.getSafetyStock() != null ? p.getSafetyStock() : 0);
                row.createCell(6).setCellValue(p.getRemark() != null ? p.getRemark() : "");
                row.createCell(7).setCellValue(p.getCreatedAt() != null
                        ? p.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    public byte[] exportInbounds(String keyword, LocalDate startDate, LocalDate endDate) throws IOException {
        List<InboundOrder> orders = inboundOrderRepository.search(startDate, endDate,
                null, keyword, null).getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("入库统计");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row header = sheet.createRow(0);
            String[] headers = {"入库单号", "货品编码", "货品名称", "规格", "入库数量", "进货单价", "进货总价", "进货源", "入库日期", "操作员", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (InboundOrder o : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(o.getOrderNo());
                row.createCell(1).setCellValue(o.getProduct().getCode());
                row.createCell(2).setCellValue(o.getProduct().getName());
                row.createCell(3).setCellValue(o.getProduct().getSpec() != null ? o.getProduct().getSpec() : "");
                row.createCell(4).setCellValue(o.getQuantity());
                row.createCell(5).setCellValue(o.getUnitPrice().doubleValue());
                row.createCell(6).setCellValue(o.getTotalPrice().doubleValue());
                row.createCell(7).setCellValue(o.getSource() != null ? o.getSource() : "");
                row.createCell(8).setCellValue(o.getInboundDate().toString());
                row.createCell(9).setCellValue(o.getOperator() != null ? o.getOperator() : "");
                row.createCell(10).setCellValue(o.getRemark() != null ? o.getRemark() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    public byte[] exportOutbounds(String keyword, LocalDate startDate, LocalDate endDate) throws IOException {
        List<OutboundOrder> orders = outboundOrderRepository.search(startDate, endDate,
                null, null, null).getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("出库统计");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row header = sheet.createRow(0);
            String[] headers = {"出库单号", "货品编码", "货品名称", "规格", "店铺名称", "出库数量", "出货单价", "出货总价", "出库日期", "操作员", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (OutboundOrder o : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(o.getOrderNo());
                row.createCell(1).setCellValue(o.getProduct().getCode());
                row.createCell(2).setCellValue(o.getProduct().getName());
                row.createCell(3).setCellValue(o.getProduct().getSpec() != null ? o.getProduct().getSpec() : "");
                row.createCell(4).setCellValue(o.getStore().getName());
                row.createCell(5).setCellValue(o.getQuantity());
                row.createCell(6).setCellValue(o.getUnitPrice().doubleValue());
                row.createCell(7).setCellValue(o.getTotalPrice().doubleValue());
                row.createCell(8).setCellValue(o.getOutboundDate().toString());
                row.createCell(9).setCellValue(o.getOperator() != null ? o.getOperator() : "");
                row.createCell(10).setCellValue(o.getRemark() != null ? o.getRemark() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    // ==================== 辅助方法 ====================

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createRequiredStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                String val = cell.getStringCellValue().trim();
                return val.isEmpty() ? null : val;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellString(row, i);
                if (val != null && !val.isBlank()) return false;
            }
        }
        return true;
    }
}
