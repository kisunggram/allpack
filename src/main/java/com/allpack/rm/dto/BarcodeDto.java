package com.allpack.rm.dto;

import lombok.Data;

@Data
public class BarcodeDto implements Comparable<BarcodeDto> {
    private String store;
    private String barcode;
    private int qty;
    private String product; 
    private String location; 
    private String regDate; 
    private String scanDate; 

    // 집계를 위한 임시 변수들
    public String PrdCode;
    public String ItemCode;
    public int locNo;
    public void AddQty(int qty) {
        this.qty += qty;
    }
    
    @Override
    public int compareTo(BarcodeDto o) {
        if (o.getQty() < qty)
            return 1;
        else if (o.getQty() > qty)
            return -1;
        else 
            return 0;
    }
}
