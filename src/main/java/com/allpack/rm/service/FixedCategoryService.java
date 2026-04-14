package com.allpack.rm.service;

import java.io.OutputStream;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.allpack.rm.dto.FixedCategoryDto;
import com.allpack.rm.dto.UploadResultDto;
import com.allpack.rm.mapper.FixedCategoryMapper;
import com.allpack.rm.store.StoreParser;
import com.allpack.rm.store.StoreParser.CategoryParseResult;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@Service
public class FixedCategoryService {

    private final FixedCategoryMapper fixedCategoryMapper;
    private final StoreRegistry storeRegistry;

    public List<FixedCategoryDto> getList(String store) {
        return fixedCategoryMapper.getListByStore(store);
    }

    @Transactional
    public UploadResultDto uploadCategory(String store, MultipartFile file) {
        UploadResultDto uploadResult = new UploadResultDto();
        String originalName = file.getOriginalFilename();
        log.info("### [FixedCategory.upload] store: {}, file: {}, size: {}", store, originalName, file.getSize());

        StoreParser parser = storeRegistry.getParser(store);

        try (Workbook workbook = originalName.endsWith(".xls")
                ? new HSSFWorkbook(file.getInputStream())
                : new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                uploadResult.setError("헤더 행이 없습니다.");
                return uploadResult;
            }

            Map<String, Integer> colIdx = buildColumnIndex(headerRow, formatter);

            fixedCategoryMapper.deleteByStore(store);

            Map<String, FixedCategoryDto> barcodeMap = new HashMap<>();
            List<FixedCategoryDto> resultList = new ArrayList<>();
            Map<String, Integer> dupCount = new LinkedHashMap<>();

            int locCol = colIdx.getOrDefault("장소", -1);
            int totalRows = 0;
            int rowSize = sheet.getPhysicalNumberOfRows();
            for (int i = 1; i < rowSize; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String location = locCol >= 0 ? formatter.formatCellValue(row.getCell(locCol)).trim() : "";

                CategoryParseResult result = parser.parseCategoryRow(row, formatter, colIdx);
                if (result == null) continue;

                totalRows++;
                if (location.isEmpty()) location = "시즌아웃";

                if (!barcodeMap.containsKey(result.mainBarcode)) {
                    FixedCategoryDto dto = new FixedCategoryDto();
                    dto.setStore(store);
                    dto.setMainBarcode(result.mainBarcode);
                    dto.setSubBarcode(result.subBarcode);
                    dto.setProduct(result.product);
                    dto.setLocation(location);
                    barcodeMap.put(result.mainBarcode, dto);
                    resultList.add(dto);
                } else {
                    dupCount.merge(result.mainBarcode, 1, Integer::sum);
                }
            }

            for (FixedCategoryDto dto : resultList) {
                fixedCategoryMapper.insertCategory(dto);
            }

            uploadResult.setTotalRows(totalRows);
            uploadResult.setInsertedCount(resultList.size());
            uploadResult.setDuplicates(dupCount);

            log.info("### [FixedCategory.upload] store: {}, total: {}, inserted: {}, duplicates: {}",
                    store, totalRows, resultList.size(), dupCount.size());
        } catch (Exception ex) {
            log.error("### [FixedCategory.upload] store: {}, Ex: {}", store, ex.getMessage());
            throw new RuntimeException(ex);
        }

        return uploadResult;
    }

    public String updateLocation(String store, String mainBarcode, String newLocation) {
        int dupCount = fixedCategoryMapper.checkDuplicateLocation(store, newLocation, mainBarcode);
        if (dupCount > 0) {
            return "중복되는 장소가 존재합니다.";
        }

        FixedCategoryDto dto = new FixedCategoryDto();
        dto.setStore(store);
        dto.setMainBarcode(mainBarcode);
        dto.setLocation(newLocation);
        fixedCategoryMapper.updateLocation(dto);
        return "";
    }

    public String getExcel(String store, String storeName, OutputStream ost) {
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(storeName + "_분류등록");
            List<FixedCategoryDto> list = fixedCategoryMapper.getListByStore(store);

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("장소");
            headerRow.createCell(1).setCellValue("바코드");
            headerRow.createCell(2).setCellValue("품목명");
            headerRow.setHeight((short) 600);

            for (FixedCategoryDto dto : list) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(dto.getLocation());
                row.createCell(1).setCellValue(dto.getMainBarcode());
                row.createCell(2).setCellValue(dto.getProduct());
            }
            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 3500);
            sheet.setColumnWidth(2, 10000);

            workbook.write(ost);
        } catch (Exception ex) {
            log.error("### [FixedCategory.getExcel] store: {}, Ex: {}", store, ex.getMessage());
            return ex.getMessage();
        } finally {
            workbook.dispose();
            try { workbook.close(); } catch (Exception ignored) {}
        }
        return "";
    }

    private Map<String, Integer> buildColumnIndex(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> colIdx = new HashMap<>();
        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String val = formatter.formatCellValue(cell).trim();
            if (!val.isEmpty()) colIdx.put(val, i);
        }
        return colIdx;
    }
}
