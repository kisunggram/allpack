package com.allpack.rm.dto;

import lombok.Data;

@Data
public class FixedHistoryDto {
    private String store;
    private String mainBarcode;
    private String product;
    private String location;
    private int qty;
    private boolean vip;
    private String vipLocation;
    private String uploadDate;
    private int scanCount;
}
