package com.allpack.rm.dto;

import lombok.Data;

@Data
public class InventoryStockDto {
    private Long id;
    private String area;
    private String place;
    private String product;
    private int qty;
}
