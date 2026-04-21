package com.allpack.rm.service;

import java.io.OutputStream;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import com.allpack.rm.dto.FixedHistoryDto;
import com.allpack.rm.mapper.FixedScanMapper;

@Slf4j
@RequiredArgsConstructor
@Service
public class FixedHistoryService {

    private final FixedScanMapper fixedScanMapper;

    public String getCategoryHistoryExcel(String store, String storeName, String uploadDate, String scanDate, OutputStream ost) {
        if (uploadDate == null || uploadDate.isEmpty()) {
            return "반품등록 기준일이 없습니다.";
        }
        if (scanDate == null || scanDate.isEmpty()) {
            return "조회 일자가 없습니다.";
        }

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(storeName + "_분류고정내역");
            List<FixedHistoryDto> list = fixedScanMapper.getHistoryByDate(store, uploadDate, scanDate);

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("장소");
            headerRow.createCell(1).setCellValue("바코드");
            headerRow.createCell(2).setCellValue("품목명");
            headerRow.createCell(3).setCellValue("수량");
            headerRow.setHeight((short) 600);

            for (FixedHistoryDto dto : list) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(dto.getLocation() != null ? dto.getLocation() : "");
                row.createCell(1).setCellValue(dto.getMainBarcode() != null ? dto.getMainBarcode() : "");
                row.createCell(2).setCellValue(dto.getProduct() != null ? dto.getProduct() : "");
                row.createCell(3).setCellValue(dto.getQty());
            }

            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 4500);
            sheet.setColumnWidth(2, 12000);
            sheet.setColumnWidth(3, 2500);

            workbook.write(ost);
        } catch (Exception ex) {
            log.error("### [FixedHistory.getCategoryHistoryExcel] store: {}, Ex: {}", store, ex.getMessage(), ex);
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
