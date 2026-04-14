package com.allpack.rm.store;

import java.util.Map;

import org.apache.poi.ss.usermodel.*;

import com.allpack.rm.dto.BarcodeDto;

public class EtcParser implements StoreParser {

    @Override
    public String getId() { return "etc"; }

    @Override
    public String getName() { return "기타"; }

    @Override
    public String extractMainBarcode(String code) {
        // 기타: 전체 바코드 사용
        return code;
    }

    @Override
    public String truncateScanBarcode(String code) {
        // 기타: 바코드 전체 사용 (패리티 제거 안함)
        return code;
    }

    @Override
    public CategoryParseResult parseCategoryRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // 기타: 바코드 칼럼 전체 사용, 품목명 별도
        Integer barcodeIdx = colIdx.get("바코드");
        if (barcodeIdx == null) {
            // 품목명에서 파싱: 메인바코드-서브바코드-품목명
            Integer prdIdx = colIdx.get("품목명");
            if (prdIdx == null) return null;
            Cell cell = row.getCell(prdIdx);
            if (cell == null) return null;
            String prdVal = formatter.formatCellValue(cell).trim();
            if (prdVal.isEmpty()) return null;

            String[] parts = prdVal.split("-", 3);
            if (parts.length >= 2) {
                return new CategoryParseResult(parts[0].trim(), parts[1].trim(),
                        parts.length >= 3 ? parts[2].trim() : "");
            }
            return new CategoryParseResult(prdVal, null, prdVal);
        }

        Cell barcodeCell = row.getCell(barcodeIdx);
        if (barcodeCell == null) return null;
        String barcodeVal = formatter.formatCellValue(barcodeCell).trim();
        if (barcodeVal.isEmpty()) return null;

        Integer prdIdx = colIdx.get("품목명");
        String product = "";
        if (prdIdx != null) {
            Cell prdCell = row.getCell(prdIdx);
            if (prdCell != null) product = formatter.formatCellValue(prdCell).trim();
        }

        return new CategoryParseResult(barcodeVal, null, product);
    }

    @Override
    public ReturnParseResult parseReturnRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // 기타: 바코드 전체 사용
        Integer barcodeIdx = colIdx.get("바코드");
        if (barcodeIdx == null) {
            Integer prdIdx = colIdx.get("품목명");
            if (prdIdx == null) return null;
            Cell cell = row.getCell(prdIdx);
            if (cell == null) return null;
            String prdVal = formatter.formatCellValue(cell).trim();
            if (prdVal.isEmpty()) return null;
            String[] parts = prdVal.split("-", 3);
            return new ReturnParseResult(parts[0].trim(), 1);
        }

        Cell barcodeCell = row.getCell(barcodeIdx);
        if (barcodeCell == null) return null;
        String barcodeVal = formatter.formatCellValue(barcodeCell).trim();
        if (barcodeVal.isEmpty()) return null;

        return new ReturnParseResult(barcodeVal, 1);
    }

    @Override
    public BarcodeDto parseGeneralRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // 기타: 바코드 or 품목명에서 파싱
        Integer barcodeIdx = colIdx.get("바코드");
        Integer prdIdx = colIdx.get("품목명");

        if (barcodeIdx != null) {
            Cell cell = row.getCell(barcodeIdx);
            if (cell == null) return null;
            String val = formatter.formatCellValue(cell).trim();
            if (val.isEmpty()) return null;

            BarcodeDto dto = new BarcodeDto();
            dto.PrdCode = val;
            dto.ItemCode = "";
            dto.setBarcode(val);
            dto.setProduct(prdIdx != null && row.getCell(prdIdx) != null
                    ? formatter.formatCellValue(row.getCell(prdIdx)).trim() : "");
            dto.setQty(1);
            return dto;
        }

        if (prdIdx != null) {
            Cell cell = row.getCell(prdIdx);
            if (cell == null) return null;
            String prdVal = formatter.formatCellValue(cell).trim();
            if (prdVal.isEmpty()) return null;

            String[] parts = prdVal.split("-", 3);
            BarcodeDto dto = new BarcodeDto();
            dto.PrdCode = parts[0].trim();
            dto.ItemCode = parts.length >= 2 ? parts[1].trim() : "";
            dto.setBarcode(dto.PrdCode + dto.ItemCode);
            dto.setProduct(parts.length >= 3 ? parts[2].trim() : prdVal);
            dto.setQty(1);
            return dto;
        }

        return null;
    }
}
