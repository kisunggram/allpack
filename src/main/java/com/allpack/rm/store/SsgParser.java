package com.allpack.rm.store;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.allpack.rm.util.ExcelHeaderUtils;
import org.apache.poi.ss.usermodel.*;

import com.allpack.rm.dto.BarcodeDto;

public class SsgParser implements StoreParser {

    @Override
    public String getId() { return "ssg"; }

    @Override
    public String getName() { return "신세계"; }

    @Override
    public String extractMainBarcode(String code) {
        // SSG: 첫 '-' 이전이 메인바코드 (가변 길이)
        int idx = code.indexOf("-");
        return idx > 0 ? code.substring(0, idx) : code;
    }

    @Override
    public String truncateScanBarcode(String code) {
        // 12번째 자리는 패리티로 11자리까지 사용
        return code.length() > 11 ? code.substring(0, 11) : code;
    }

    Pattern barcodePattern = Pattern.compile("^\\d{8}-\\d{3}-.*$");
    @Override
    public CategoryParseResult parseCategoryRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // SSG: 품목명 = PrdCode-ItemCode-제품명
        Integer prdIdx = colIdx.get("품목명");
        if (prdIdx == null) return null;
        String prdVal = ExcelHeaderUtils.getCellString(row, formatter, prdIdx);
        if (prdVal == null) return null;

        String mainBarcode = "", subBarcode = "", product = "";

        Matcher matcher = barcodePattern.matcher(prdVal);
        if (matcher.find()) {
            String barcode = matcher.group(1);
            mainBarcode = barcode.substring(0, 8);
            subBarcode = barcode.substring(9, 12);
            product = prdVal.substring(13);
        }
        else {
            Integer codeIdx = colIdx.get("바코드");
            if (codeIdx == null) return null;
            String barcode = ExcelHeaderUtils.getCellString(row, formatter, codeIdx);
            if (barcode == null) return null;

            if (barcode.length() >= 8)
                mainBarcode = barcode.substring(0, 8);
            if (barcode.length() >= 12) {
                subBarcode = barcode.substring(8);
                if (subBarcode.startsWith("-"))
                    subBarcode = barcode.substring(9);
            }
            product = prdVal;
        }

        return new CategoryParseResult(mainBarcode, subBarcode, product);
    }

    @Override
    public ReturnParseResult parseReturnRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // SSG: 품목명에서 첫 '-' 이전이 PrdCode, 수량은 행당 1
        Integer prdIdx = colIdx.get("품목명");
        if (prdIdx == null) return null;

        Cell cell = row.getCell(prdIdx);
        if (cell == null) return null;
        String prdVal = formatter.formatCellValue(cell).trim();
        if (prdVal.isEmpty()) return null;

        String prdCode = nextToken(prdVal, 0);
        if (prdCode == null) return null;

        return new ReturnParseResult(prdCode, 1);
    }

    @Override
    public BarcodeDto parseGeneralRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // SSG 일반관리: 품목명 = PrdCode-ItemCode-제품명
        Integer prdIdx = colIdx.get("품목명");
        if (prdIdx == null) return null;

        Cell cell = row.getCell(prdIdx);
        if (cell == null) return null;
        String prdVal = formatter.formatCellValue(cell).trim();
        if (prdVal.isEmpty()) return null;

        String prdCode = nextToken(prdVal, 0);
        if (prdCode == null) return null;
        String itemCode = nextToken(prdVal, prdCode.length() + 1);

        BarcodeDto dto = new BarcodeDto();
        dto.PrdCode = prdCode;
        dto.ItemCode = itemCode;
        dto.setBarcode(prdCode + (itemCode != null ? itemCode : ""));
        int nameStart = prdCode.length() + 1 + (itemCode != null ? itemCode.length() + 1 : 0);
        dto.setProduct(nameStart < prdVal.length() ? prdVal.substring(nameStart).trim() : prdVal);
        dto.setQty(1);
        return dto;
    }

    private String nextToken(String str, int startpos) {
        if (startpos >= str.length()) return null;
        str = str.substring(startpos);
        int idx = str.indexOf("-");
        return idx == -1 ? null : str.substring(0, idx);
    }
}
