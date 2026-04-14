package com.allpack.rm.dto;

import lombok.Data;

@Data
public class InventoryDto {
    private Long id;
    private String area;
    private String place;
    private String location;
    private String barcode;
    private String product;
    private String itemCode;
    private String itemName;
    private int baseQty;
    private int inQty;
    private int outQty;
    private String regDate;

    public int getRealQty() {
        return baseQty + inQty - outQty;
    }
}
