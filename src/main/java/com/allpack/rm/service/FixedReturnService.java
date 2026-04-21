package com.allpack.rm.service;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
import com.allpack.rm.dto.FixedReturnDto;
import com.allpack.rm.mapper.FixedCategoryMapper;
import com.allpack.rm.mapper.FixedReturnMapper;
import com.allpack.rm.store.StoreParser;
import com.allpack.rm.store.StoreParser.ReturnParseResult;
import com.allpack.rm.store.StoreRegistry;
import com.allpack.rm.util.ExcelHeaderUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class FixedReturnService {

    private static final Set<String> RETURN_HEADERS = Set.of(
            "바코드", "상품코드", "품목명", "수량");

    private final FixedReturnMapper fixedReturnMapper;
    private final FixedCategoryMapper fixedCategoryMapper;
    private final StoreRegistry storeRegistry;

    public List<FixedReturnDto> getList(String store, String uploadDate) {
        if (uploadDate == null || uploadDate.isEmpty()) {
            uploadDate = fixedReturnMapper.getLatestUploadDate(store);
        }
        if (uploadDate == null) {
            return Collections.emptyList();
        }
        return fixedReturnMapper.getListByStoreAndDate(store, uploadDate);
    }

    public String getLatestUploadDate(String store) {
        return fixedReturnMapper.getLatestUploadDate(store);
    }

    @Transactional
    public String uploadReturn(String store, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        log.info("### [FixedReturn.upload] store: {}, file: {}, size: {}", store, originalName, file.getSize());

        StoreParser parser = storeRegistry.getParser(store);

        try (Workbook workbook = originalName.endsWith(".xls")
                ? new HSSFWorkbook(file.getInputStream())
                : new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            int headerRowIdx = ExcelHeaderUtils.findHeaderRowIndex(sheet, formatter, RETURN_HEADERS, 1);
            if (headerRowIdx < 0) {
                return "헤더 행이 없습니다.";
            }

            Row headerRow = sheet.getRow(headerRowIdx);
            Map<String, Integer> colIdx = ExcelHeaderUtils.buildColumnIndex(headerRow, formatter);

            Set<String> validBarcodes = new HashSet<>();
            for (FixedCategoryDto cat : fixedCategoryMapper.getListByStore(store)) {
                validBarcodes.add(cat.getMainBarcode());
            }

            Map<String, Integer> qtyMap = new LinkedHashMap<>();
            int rowSize = sheet.getPhysicalNumberOfRows();
            for (int i = headerRowIdx + 1; i < rowSize; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                ReturnParseResult result = parser.parseReturnRow(row, formatter, colIdx);
                if (result == null) {
                    continue;
                }

                if (!validBarcodes.contains(result.mainBarcode)) {
                    continue;
                }

                qtyMap.merge(result.mainBarcode, result.qty, Integer::sum);
            }

            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            fixedReturnMapper.deleteByStoreAndDate(store, today);

            for (Map.Entry<String, Integer> entry : qtyMap.entrySet()) {
                FixedReturnDto dto = new FixedReturnDto();
                dto.setStore(store);
                dto.setMainBarcode(entry.getKey());
                dto.setQty(entry.getValue());
                dto.setVip(false);
                dto.setUploadDate(today);
                fixedReturnMapper.insertReturn(dto);
            }

            log.info("### [FixedReturn.upload] store: {}, date: {}, inserted: {}", store, today, qtyMap.size());
        } catch (Exception ex) {
            log.error("### [FixedReturn.upload] store: {}, Ex: {}", store, ex.getMessage());
            throw new RuntimeException(ex);
        }

        return "";
    }

    public String updateReturn(Long id, int qty, boolean vip, String vipLocation) {
        FixedReturnDto dto = new FixedReturnDto();
        dto.setId(id);
        dto.setQty(qty);
        dto.setVip(vip);
        dto.setVipLocation(vipLocation);
        fixedReturnMapper.updateReturn(dto);
        return "";
    }

    public List<String> getDistinctDates(String store, String startDate, String endDate) {
        return fixedReturnMapper.getDistinctDates(store, startDate, endDate);
    }

    public String getExcel(String store, String storeName, String uploadDate, OutputStream ost) {
        if (uploadDate == null) {
            uploadDate = fixedReturnMapper.getLatestUploadDate(store);
        }
        if (uploadDate == null) {
            return "반품등록 데이터가 없습니다.";
        }

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(storeName + "_반품등록");
            List<FixedReturnDto> list = fixedReturnMapper.getListByStoreAndDate(store, uploadDate);

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("vip");
            headerRow.createCell(1).setCellValue("vip-위치");
            headerRow.createCell(2).setCellValue("위치");
            headerRow.createCell(3).setCellValue("바코드");
            headerRow.createCell(4).setCellValue("품목명");
            headerRow.createCell(5).setCellValue("수량");
            headerRow.setHeight((short) 600);

            for (FixedReturnDto dto : list) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(dto.isVip() ? "Y" : "");
                row.createCell(1).setCellValue(dto.getVipLocation() != null ? dto.getVipLocation() : "");
                row.createCell(2).setCellValue(dto.getLocation());
                row.createCell(3).setCellValue(dto.getMainBarcode());
                row.createCell(4).setCellValue(dto.getProduct());
                row.createCell(5).setCellValue(dto.getQty());
            }
            sheet.setColumnWidth(2, 2500);
            sheet.setColumnWidth(3, 3500);
            sheet.setColumnWidth(4, 10000);

            workbook.write(ost);
        } catch (Exception ex) {
            log.error("### [FixedReturn.getExcel] store: {}, Ex: {}", store, ex.getMessage());
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
