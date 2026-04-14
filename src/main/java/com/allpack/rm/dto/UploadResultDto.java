package com.allpack.rm.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

@Data
public class UploadResultDto {
    private String error = "";
    private int totalRows;
    private int insertedCount;
    /** 중복 바코드 → 중복 횟수 (skip된 횟수) */
    private Map<String, Integer> duplicates = new LinkedHashMap<>();

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public boolean hasDuplicates() {
        return !duplicates.isEmpty();
    }

    public int getDuplicateTotalCount() {
        int sum = 0;
        for (int v : duplicates.values()) sum += v;
        return sum;
    }
}
