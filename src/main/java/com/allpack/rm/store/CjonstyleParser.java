package com.allpack.rm.store;

import java.util.Map;

import org.apache.poi.ss.usermodel.*;

import com.allpack.rm.dto.BarcodeDto;

public class CjonstyleParser implements StoreParser {

    @Override
    public String getId() { return "cjonstyle"; }

    @Override
    public String getName() { return "온스타일"; }

    @Override
    public String extractMainBarcode(String code) {
        // CJ 온스타일: 앞 8자리
        return code.length() > 8 ? code.substring(0, 8) : code;
    }

    @Override
    public String truncateScanBarcode(String code) {
        // 12번째 자리는 패리티로 11자리까지 사용
        return code.length() > 11 ? code.substring(0, 11) : code;
    }

    @Override
    public CategoryParseResult parseCategoryRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // CJ: 단품코드 앞 8자리 = PrdCode, 명칭 = 제품명
        Integer itemIdx = colIdx.get("단품코드");
        if (itemIdx == null) {
            // 바코드 칼럼 fallback
            itemIdx = colIdx.get("바코드");
        }
        if (itemIdx == null) return null;

        Cell itemCell = row.getCell(itemIdx);
        if (itemCell == null) return null;
        String itemVal = formatter.formatCellValue(itemCell).trim();
        if (itemVal.isEmpty()) return null;

        String mainBarcode = itemVal.length() > 8 ? itemVal.substring(0, 8) : itemVal;
        String subBarcode = itemVal.length() > 8 ? itemVal.substring(8) : null;

        Integer nameIdx = colIdx.get("명칭");
        if (nameIdx == null) nameIdx = colIdx.get("품목명");
        String product = "";
        if (nameIdx != null) {
            Cell nameCell = row.getCell(nameIdx);
            if (nameCell != null) product = formatter.formatCellValue(nameCell).trim();
        }

        return new CategoryParseResult(mainBarcode, subBarcode, product);
    }

    @Override
    public ReturnParseResult parseReturnRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // CJ: 단품코드 앞 8자리, 수량 칼럼 사용
        Integer itemIdx = colIdx.get("단품코드");
        if (itemIdx == null) itemIdx = colIdx.get("바코드");
        if (itemIdx == null) return null;

        Cell itemCell = row.getCell(itemIdx);
        if (itemCell == null) return null;
        String itemVal = formatter.formatCellValue(itemCell).trim();
        if (itemVal.isEmpty()) return null;

        String mainBarcode = itemVal.length() > 8 ? itemVal.substring(0, 8) : itemVal;

        int qty = 1;
        Integer qtyIdx = colIdx.get("수량");
        if (qtyIdx != null) {
            Cell qtyCell = row.getCell(qtyIdx);
            if (qtyCell != null) {
                try {
                    qty = Integer.parseInt(formatter.formatCellValue(qtyCell).trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        return new ReturnParseResult(mainBarcode, qty);
    }

    @Override
    public BarcodeDto parseGeneralRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx) {
        // CJ 일반관리: 단품코드/명칭/수량
        Integer itemIdx = colIdx.get("단품코드");
        Integer nameIdx = colIdx.get("명칭");
        Integer qtyIdx = colIdx.get("수량");
        if (itemIdx == null || nameIdx == null || qtyIdx == null) return null;

        Cell itemCell = row.getCell(itemIdx);
        Cell nameCell = row.getCell(nameIdx);
        Cell qtyCell = row.getCell(qtyIdx);
        if (itemCell == null || nameCell == null || qtyCell == null) return null;

        String item = formatter.formatCellValue(itemCell).trim();
        if (item.isEmpty()) return null;

        BarcodeDto dto = new BarcodeDto();
        dto.PrdCode = item.substring(0, Math.min(8, item.length()));
        dto.ItemCode = item.length() > 8 ? item.substring(8) : "";
        dto.setBarcode(item);
        dto.setProduct(formatter.formatCellValue(nameCell).trim());
        try {
            dto.setQty(Integer.parseInt(formatter.formatCellValue(qtyCell).trim()));
        } catch (NumberFormatException e) {
            dto.setQty(1);
        }
        return dto;
    }
}
