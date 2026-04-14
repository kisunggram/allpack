package com.allpack.rm.dto;

import lombok.Data;

@Data
public class ApiResultDto {
    private Boolean result;
    private String message;
    private Object data;

    public static ApiResultDto success(String message) {
        ApiResultDto dto = new ApiResultDto();
        dto.setResult(true);
        dto.setMessage(message);
        return dto;
    }

    public static ApiResultDto fail(String message) {
        ApiResultDto dto = new ApiResultDto();
        dto.setResult(false);
        dto.setMessage(message);
        return dto;
    }
}
