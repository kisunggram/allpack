package com.allpack.rm.service;

import java.io.OutputStream;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.allpack.rm.dto.InventoryDto;
import com.allpack.rm.mapper.InventoryMapper;

@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryService {

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

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return "헤더 행이 없습니다.";

            // 칼럼 인덱스 찾기
            int areaIdx = -1, placeIdx = -1, locIdx = -1, barcodeIdx = -1;
            int productIdx = -1, itemCodeIdx = -1, itemNameIdx = -1, qtyIdx = -1;

            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                String val = formatter.formatCellValue(headerRow.getCell(i)).trim();
                switch (val) {
                    case "구역": areaIdx = i; break;
                    case "장소": placeIdx = i; break;
                    case "로케이션": locIdx = i; break;
                    case "바코드": barcodeIdx = i; break;
                    case "품명": productIdx = i; break;
                    case "단품코드": itemCodeIdx = i; break;
                    case "단품명": itemNameIdx = i; break;
                    case "수량": qtyIdx = i; break;
                }
            }

            // 전체 삭제 후 삽입
            inventoryMapper.deleteAll();

            int rowSize = sheet.getPhysicalNumberOfRows();
            int insertCount = 0;
            for (int i = 1; i < rowSize; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

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

                inventoryMapper.insertInventory(dto);
                insertCount++;
            }

            log.info("### [Inventory.upload] inserted: {}", insertCount);
        } catch (Exception ex) {
            log.error("### [Inventory.upload] Ex: {}", ex.getMessage());
            throw new RuntimeException(ex);
        }

        return "";
    }

    private String getCellValue(DataFormatter formatter, Row row, int idx) {
        if (idx < 0) return "";
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
            headerRow.createCell(4).setCellValue("품명");
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
            log.error("### [Inventory.getExcel] Ex: {}", ex.getMessage());
            return ex.getMessage();
        } finally {
            workbook.dispose();
            try { workbook.close(); } catch (Exception ignored) {}
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
