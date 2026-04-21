package com.allpack.rm.service;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.allpack.rm.dto.FixedCategoryDto;
import com.allpack.rm.dto.UploadResultDto;
import com.allpack.rm.mapper.FixedCategoryMapper;
import com.allpack.rm.store.StoreParser;
import com.allpack.rm.store.StoreParser.CategoryParseResult;
import com.allpack.rm.store.StoreRegistry;
import com.allpack.rm.util.ExcelHeaderUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class FixedCategoryService {

    private static final Set<String> CATEGORY_HEADERS = Set.of(
            "장소", "바코드", "품목명");

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

            int headerRowIdx = ExcelHeaderUtils.findHeaderRowIndex(sheet, formatter, CATEGORY_HEADERS, 2);
            if (headerRowIdx < 0) {
                uploadResult.setError("헤더 행이 없습니다.");
                return uploadResult;
            }

            Row headerRow = sheet.getRow(headerRowIdx);
            Map<String, Integer> colIdx = ExcelHeaderUtils.buildColumnIndex(headerRow, formatter);

            fixedCategoryMapper.deleteByStore(store);

            Map<String, FixedCategoryDto> barcodeMap = new HashMap<>();
            List<FixedCategoryDto> resultList = new ArrayList<>();
            Map<String, Integer> dupCount = new LinkedHashMap<>();

            int locCol = colIdx.getOrDefault("장소", -1);
            int totalRows = 0;
            int rowSize = sheet.getPhysicalNumberOfRows();
            for (int i = headerRowIdx + 1; i < rowSize; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String location = locCol >= 0 ? formatter.formatCellValue(row.getCell(locCol)).trim() : "";

                CategoryParseResult result = parser.parseCategoryRow(row, formatter, colIdx);
                if (result == null) {
                    continue;
                }

                totalRows++;
                if (location.isEmpty()) {
                    location = "시즌아웃";
                }

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

    public String insertRow(String store, String mainBarcode, String product, String location) {
        if (mainBarcode == null || mainBarcode.trim().isEmpty()) {
            return "바코드를 입력해주세요.";
        }
        String loc = (location == null || location.trim().isEmpty()) ? "시즌아웃" : location.trim();
        if (!"시즌아웃".equals(loc) && fixedCategoryMapper.checkDuplicateLocation(store, loc, mainBarcode.trim()) > 0) {
            return "중복되는 위치가 존재합니다. [" + loc + "]";
        }
        try {
            FixedCategoryDto dto = new FixedCategoryDto();
            dto.setStore(store);
            dto.setMainBarcode(mainBarcode.trim());
            dto.setProduct(product == null ? "" : product.trim());
            dto.setLocation(loc);
            fixedCategoryMapper.insertCategory(dto);
        } catch (Exception ex) {
            log.error("### [FixedCategory.insertRow] barcode: {}, Ex: {}", mainBarcode, ex.getMessage());
            return "[" + mainBarcode + "] 등록 실패: 중복된 바코드일 수 있습니다.";
        }
        return "";
    }

    public String updateLocation(String store, String mainBarcode, String newLocation) {
        String loc = (newLocation == null || newLocation.trim().isEmpty()) ? "시즌아웃" : newLocation.trim();
        if (!"시즌아웃".equals(loc)
                && fixedCategoryMapper.checkDuplicateLocation(store, loc, mainBarcode) > 0) {
            return "중복되는 위치가 존재합니다.";
        }

        FixedCategoryDto dto = new FixedCategoryDto();
        dto.setStore(store);
        dto.setMainBarcode(mainBarcode);
        dto.setLocation(loc);
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
            try {
                workbook.close();
            } catch (Exception ignored) {
            }
        }
        return "";
    }
}
