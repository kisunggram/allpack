package com.allpack.rm.dto;

import lombok.Data;

@Data
public class ReturnDto {
    private Boolean result;
    private String message;
    private BarcodeDto data;
}
