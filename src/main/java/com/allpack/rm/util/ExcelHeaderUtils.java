package com.allpack.rm.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ExcelHeaderUtils {

    private ExcelHeaderUtils() {
    }

    public static int findHeaderRowIndex(Sheet sheet, DataFormatter formatter, Set<String> expectedHeaders, int minMatchCount) {
        int lastRowNum = Math.min(sheet.getLastRowNum(), 9);
        int bestRowIdx = -1;
        int bestMatchCount = 0;

        for (int rowIdx = 0; rowIdx <= lastRowNum; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) {
                continue;
            }

            int matchCount = countMatchingHeaders(row, formatter, expectedHeaders);
            if (matchCount > bestMatchCount) {
                bestMatchCount = matchCount;
                bestRowIdx = rowIdx;
            }
        }

        return bestMatchCount >= minMatchCount ? bestRowIdx : -1;
    }
    private static int countMatchingHeaders(Row row, DataFormatter formatter, Set<String> expectedHeaders) {
        int lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return 0;
        }

        int matchCount = 0;
        for (int cellIdx = 0; cellIdx < lastCellNum; cellIdx++) {
            String headerName = normalizeHeader(formatter.formatCellValue(row.getCell(cellIdx)));
            if (expectedHeaders.contains(headerName)) {
                matchCount++;
            }
        }

        return matchCount;
    }

    public static Map<String, Integer> buildColumnIndex(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> colIdx = new HashMap<>();
        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) {
                continue;
            }

            String value = normalizeHeader(formatter.formatCellValue(cell));
            if (!value.isEmpty()) {
                colIdx.put(value, i);
            }
        }
        return colIdx;
    }

    public static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\n", "").replace("\r", "").replace(" ", "").trim();
    }

    public static String getCellString(Row row, DataFormatter formatter, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String val = formatter.formatCellValue(cell);
        if (val == null) return null;
        return val.trim();
    }
}
