package com.allpack.rm.dto;

import lombok.Data;

@Data
public class FixedCategoryDto {
    private String store;
    private String mainBarcode;
    private String subBarcode;
    private String product;
    private String location;
    private String regDate;
}
