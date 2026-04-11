package com.allpack.rm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

import com.allpack.rm.dto.BarcodeDto;

@Mapper
public interface BarcodeMapper {

    List<BarcodeDto> getProductList(String store);
    int insProduct(BarcodeDto barcode);
    int setLocation(BarcodeDto barcode);
    int clearProduct(String store);

    List<BarcodeDto> getBarcode(BarcodeDto barcode);
    int scanBarcode(BarcodeDto barcode);

}
