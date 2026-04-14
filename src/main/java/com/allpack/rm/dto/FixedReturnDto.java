package com.allpack.rm.dto;

import lombok.Data;

@Data
public class FixedReturnDto {
    private Long id;
    private String store;
    private String mainBarcode;
    private int qty;
    private boolean vip;
    private String vipLocation;
    private String uploadDate;
    private String updateDate;
    // JOIN from FIXED_CATEGORY
    private String product;
    private String location;
}
