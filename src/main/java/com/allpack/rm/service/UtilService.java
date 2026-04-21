package com.allpack.rm.service;

import com.allpack.rm.dto.UtilCategoryDto;
import com.allpack.rm.mapper.UtilCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.allpack.rm.util.ExcelHeaderUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class UtilService {

    private static final Set<String> UTIL_HEADERS = Set.of(
            "품목명", "단품코드", "상품코드", "명칭", "상품명", "수량", "수량합계");

    private static final Pattern PRODUCT_BARCODE_PATTERN = Pattern.compile("^(\\d{8})-\\d{3}-(.*)$");

    private final UtilCategoryMapper utilCategoryMapper;

    public List<UtilCategoryDto> getList() {
        return utilCategoryMapper.getList();
    }

    @Transactional
    public String uploadExcel(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        log.info("### [UtilService.uploadExcel] file: {}, size: {}", originalName, file.getSize());

        try (Workbook workbook = originalName != null && originalName.endsWith(".xls")
                ? new HSSFWorkbook(file.getInputStream())
                : new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            int headerRowIdx = ExcelHeaderUtils.findHeaderRowIndex(sheet, formatter, UTIL_HEADERS, 2);
            if (headerRowIdx < 0) {
                return "헤더 행이 없습니다.";
            }

            Row headerRow = sheet.getRow(headerRowIdx);
            Map<String, Integer> colIdx = ExcelHeaderUtils.buildColumnIndex(headerRow, formatter);
            int productDescIdx = findColumnIndex(colIdx, -1, "품목명");
            int barcodeColIdx = findColumnIndex(colIdx, -1, "단품코드", "상품코드");
            int nameColIdx = findColumnIndex(colIdx, -1, "명칭", "상품명");
            int qtyIdx = findColumnIndex(colIdx, -1, "수량", "수량합계");
            log.info("### [util.excel.upload] 품목명: {}, 단품코드: {}, 명칭: {}, 수량: {}",
                    productDescIdx, barcodeColIdx, nameColIdx, qtyIdx);

            if (productDescIdx < 0 && barcodeColIdx < 0) {
                return "바코드 컬럼을 찾을 수 없습니다.";
            }

            Map<String, UtilCategoryDto> barcodeMap = new HashMap<>();
            int rowSize = sheet.getPhysicalNumberOfRows();
            for (int i = headerRowIdx + 1; i < rowSize; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                String barcode = null;
                String product = null;

                // 1. 품목명 패턴 체크 (숫자8-숫자3-문자)
                if (productDescIdx >= 0) {
                    String prdVal = getCellValue(formatter, row, productDescIdx);
                    Matcher matcher = PRODUCT_BARCODE_PATTERN.matcher(prdVal);
                    if (matcher.matches()) {
                        barcode = matcher.group(1);
                        product = matcher.group(2);
                    }
                }

                // 2. 단품코드/상품코드 컬럼값이 8자리 숫자로 시작
                if (barcode == null && barcodeColIdx >= 0) {
                    String val = getCellValue(formatter, row, barcodeColIdx);
                    if (val.length() >= 8 && val.substring(0, 8).matches("\\d{8}")) {
                        barcode = val.substring(0, 8);
                    }
                }

                // 3. 명칭/상품명 컬럼값을 품목명으로
                if (product == null && nameColIdx >= 0) {
                    product = getCellValue(formatter, row, nameColIdx);
                }

                if (barcode == null || barcode.isEmpty()) {
                    continue;
                }
                if (product == null) {
                    product = "";
                }

                int qty = parseQty(getCellValue(formatter, row, qtyIdx));
                if (qty <= 0) {
                    qty = 1;
                }

                UtilCategoryDto dto = barcodeMap.get(barcode);
                if (dto == null) {
                    dto = new UtilCategoryDto();
                    dto.setBarcode(barcode);
                    dto.setProduct(product);
                    dto.setQty(qty);
                    barcodeMap.put(barcode, dto);
                } else {
                    dto.setQty(dto.getQty() + qty);
                    if ((dto.getProduct() == null || dto.getProduct().isEmpty()) && !product.isEmpty()) {
                        dto.setProduct(product);
                    }
                }
            }

            List<UtilCategoryDto> categoryList = new ArrayList<>(barcodeMap.values());
            categoryList.sort(Comparator
                    .comparingInt(UtilCategoryDto::getQty).reversed()
                    .thenComparing(UtilCategoryDto::getBarcode, Comparator.nullsLast(String::compareTo)));

            utilCategoryMapper.deleteCategoryAll();

            int location = 1;
            for (UtilCategoryDto dto : categoryList) {
                dto.setLocation(Integer.toString(location++));
                utilCategoryMapper.insertCategory(dto);
            }

            log.info("### [UtilService.uploadExcel] inserted: {}", categoryList.size());
        } catch (Exception ex) {
            log.error("### [UtilService.uploadExcel] Ex: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }

        return "";
    }

    public String getExcel(OutputStream ost) {
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("분류고정");
            List<UtilCategoryDto> list = utilCategoryMapper.getList();

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("장소");
            headerRow.createCell(1).setCellValue("바코드");
            headerRow.createCell(2).setCellValue("품목명");
            headerRow.createCell(3).setCellValue("수량");
            headerRow.setHeight((short) 600);

            for (UtilCategoryDto dto : list) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(dto.getLocation() != null ? dto.getLocation() : "");
                row.createCell(1).setCellValue(dto.getBarcode() != null ? dto.getBarcode() : "");
                row.createCell(2).setCellValue(dto.getProduct() != null ? dto.getProduct() : "");
                row.createCell(3).setCellValue(dto.getQty());
            }
            workbook.write(ost);
        } catch (Exception ex) {
            log.error("### [UtilService.getExcel] Ex: {}", ex.getMessage(), ex);
            return ex.getMessage();
        } finally {
            workbook.dispose();
            try {
                workbook.close();
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private int findColumnIndex(Map<String, Integer> colIdx, int fallbackIdx, String... headerNames) {
        for (String headerName : headerNames) {
            Integer idx = colIdx.get(ExcelHeaderUtils.normalizeHeader(headerName));
            if (idx != null) {
                return idx;
            }
        }
        return fallbackIdx;
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        int lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return true;
        }

        for (int cellIdx = 0; cellIdx < lastCellNum; cellIdx++) {
            if (!formatter.formatCellValue(row.getCell(cellIdx)).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(DataFormatter formatter, Row row, int idx) {
        if (idx < 0) {
            return "";
        }
        Cell cell = row.getCell(idx);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private int parseQty(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
