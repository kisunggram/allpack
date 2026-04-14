package com.allpack.rm.service;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.allpack.rm.dto.FixedReturnDto;
import com.allpack.rm.mapper.FixedCategoryMapper;
import com.allpack.rm.mapper.FixedReturnMapper;
import com.allpack.rm.store.StoreParser;
import com.allpack.rm.store.StoreParser.ReturnParseResult;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@Service
public class FixedReturnService {

    private final FixedReturnMapper fixedReturnMapper;
    private final FixedCategoryMapper fixedCategoryMapper;
    private final StoreRegistry storeRegistry;

    public List<FixedReturnDto> getList(String store, String uploadDate) {
        if (uploadDate == null || uploadDate.isEmpty()) {
            uploadDate = fixedReturnMapper.getLatestUploadDate(store);
        }
        if (uploadDate == null) return Collections.emptyList();
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

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return "헤더 행이 없습니다.";

            Map<String, Integer> colIdx = buildColumnIndex(headerRow, formatter);

            // 분류등록 데이터 로드 (매칭용)
            Set<String> validBarcodes = new HashSet<>();
            for (FixedCategoryDto cat : fixedCategoryMapper.getListByStore(store)) {
                validBarcodes.add(cat.getMainBarcode());
            }

            // 엑셀 파싱 → mainBarcode별 수량 집계
            Map<String, Integer> qtyMap = new LinkedHashMap<>();
            int rowSize = sheet.getPhysicalNumberOfRows();
            for (int i = 1; i < rowSize; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ReturnParseResult result = parser.parseReturnRow(row, formatter, colIdx);
                if (result == null) continue;

                if (!validBarcodes.contains(result.mainBarcode)) continue;

                qtyMap.merge(result.mainBarcode, result.qty, Integer::sum);
            }

            // 오늘 날짜로 저장
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
        if (uploadDate == null) uploadDate = fixedReturnMapper.getLatestUploadDate(store);
        if (uploadDate == null) return "반품등록 데이터가 없습니다.";

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(storeName + "_반품등록");
            List<FixedReturnDto> list = fixedReturnMapper.getListByStoreAndDate(store, uploadDate);

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("vip");
            headerRow.createCell(1).setCellValue("vip-위치");
            headerRow.createCell(2).setCellValue("장소");
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
