package com.allpack.rm.service;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import com.allpack.rm.dto.InventoryDto;
import com.allpack.rm.mapper.InventoryMapper;
import com.allpack.rm.util.ExcelHeaderUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryService {

    private static final Set<String> INVENTORY_HEADERS = Set.of(
            "구역", "장소", "로케이션", "바코드", "상품코드", "품명", "상품명",
            "단품코드", "단품명", "기준수량", "재고합계");

    private final InventoryMapper inventoryMapper;

    public List<InventoryDto> getList() {
        return inventoryMapper.getList();
    }

    @Transactional
    public String uploadInventory(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        log.info("### [Inventory.upload] file: {}, size: {}", originalName, file.getSize());

        try (Workbook workbook = originalName.endsWith(".xls")
                ? new HSSFWorkbook(file.getInputStream())
                : new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            int headerRowIdx = ExcelHeaderUtils.findHeaderRowIndex(sheet, formatter, INVENTORY_HEADERS, 3);
            if (headerRowIdx < 0) {
                return "헤더 행을 찾을 수 없습니다.";
            }

            Row headerRow = sheet.getRow(headerRowIdx);

            int areaIdx = -1;
            int placeIdx = -1;
            int locIdx = -1;
            int barcodeIdx = -1;
            int productIdx = -1;
            int itemCodeIdx = -1;
            int itemNameIdx = -1;
            int qtyIdx = -1;

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String val = ExcelHeaderUtils.normalizeHeader(formatter.formatCellValue(headerRow.getCell(i)));
                switch (val) {
                    case "구역":
                        areaIdx = i;
                        break;
                    case "장소":
                        placeIdx = i;
                        break;
                    case "로케이션":
                        locIdx = i;
                        break;
                    case "바코드":
                    case "상품코드":
                        barcodeIdx = i;
                        break;
                    case "품명":
                    case "상품명":
                        productIdx = i;
                        break;
                    case "단품코드":
                        itemCodeIdx = i;
                        break;
                    case "단품명":
                        itemNameIdx = i;
                        break;
                    case "기준수량":
                    case "재고합계":
                        qtyIdx = i;
                        break;
                    default:
                        break;
                }
            }

            log.info("### [Inventory.upload] 헤더 행: {}", headerRowIdx);
            log.info("### [Inventory.upload] 구역:{}, 장소:{}, 로케이션:{}, 바코드:{}", areaIdx, placeIdx, locIdx, barcodeIdx);
            log.info("### [Inventory.upload] 상품명:{}, 단품코드:{}, 단품명:{}", productIdx, itemCodeIdx, itemNameIdx);
            log.info("### [Inventory.upload] 기준수량:{}", qtyIdx);

            inventoryMapper.deleteAll();

            int rowSize = sheet.getPhysicalNumberOfRows();
            int insertCount = 0;
            for (int i = headerRowIdx + 1; i < rowSize; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                InventoryDto dto = new InventoryDto();
                dto.setArea(getCellValue(formatter, row, areaIdx));
                dto.setPlace(getCellValue(formatter, row, placeIdx));
                dto.setLocation(getCellValue(formatter, row, locIdx));
                dto.setBarcode(getCellValue(formatter, row, barcodeIdx));
                dto.setProduct(getCellValue(formatter, row, productIdx));
                dto.setItemCode(getCellValue(formatter, row, itemCodeIdx));
                dto.setItemName(getCellValue(formatter, row, itemNameIdx));

                String qtyVal = getCellValue(formatter, row, qtyIdx);
                try {
                    dto.setBaseQty(qtyVal.isEmpty() ? 0 : Integer.parseInt(qtyVal));
                } catch (NumberFormatException e) {
                    dto.setBaseQty(0);
                }

                dto.setInQty(0);
                dto.setOutQty(0);

                inventoryMapper.insertInventory(dto);
                insertCount++;
            }

            log.info("### [Inventory.upload] inserted: {}", insertCount);
        } catch (Exception ex) {
            log.error("### [Inventory.upload] Ex: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }

        return "";
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

    public String getExcel(OutputStream ost) {
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("재고관리");
            List<InventoryDto> list = inventoryMapper.getList();

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("구역");
            headerRow.createCell(1).setCellValue("장소");
            headerRow.createCell(2).setCellValue("로케이션");
            headerRow.createCell(3).setCellValue("바코드");
            headerRow.createCell(4).setCellValue("상품명");
            headerRow.createCell(5).setCellValue("단품코드");
            headerRow.createCell(6).setCellValue("단품명");
            headerRow.createCell(7).setCellValue("기준수량");
            headerRow.createCell(8).setCellValue("입고수량");
            headerRow.createCell(9).setCellValue("출고수량");
            headerRow.createCell(10).setCellValue("실수량");
            headerRow.setHeight((short) 600);

            for (InventoryDto dto : list) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(dto.getArea() != null ? dto.getArea() : "");
                row.createCell(1).setCellValue(dto.getPlace() != null ? dto.getPlace() : "");
                row.createCell(2).setCellValue(dto.getLocation() != null ? dto.getLocation() : "");
                row.createCell(3).setCellValue(dto.getBarcode() != null ? dto.getBarcode() : "");
                row.createCell(4).setCellValue(dto.getProduct() != null ? dto.getProduct() : "");
                row.createCell(5).setCellValue(dto.getItemCode() != null ? dto.getItemCode() : "");
                row.createCell(6).setCellValue(dto.getItemName() != null ? dto.getItemName() : "");
                row.createCell(7).setCellValue(dto.getBaseQty());
                row.createCell(8).setCellValue(dto.getInQty());
                row.createCell(9).setCellValue(dto.getOutQty());
                row.createCell(10).setCellValue(dto.getRealQty());
            }

            workbook.write(ost);
        } catch (Exception ex) {
            log.error("### [Inventory.getExcel] Ex: {}", ex.getMessage(), ex);
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

    public String updateFields(Long id, String area, String place, int baseQty) {
        InventoryDto dto = new InventoryDto();
        dto.setId(id);
        dto.setArea(area);
        dto.setPlace(place);
        dto.setBaseQty(baseQty);
        inventoryMapper.updateFields(dto);
        return "";
    }
}
