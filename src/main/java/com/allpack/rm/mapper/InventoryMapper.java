package com.allpack.rm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.allpack.rm.dto.InventoryDto;
import com.allpack.rm.dto.InventoryStockDto;

@Mapper
public interface InventoryMapper {

    List<InventoryDto> getList();

    int insertInventory(InventoryDto dto);

    int deleteAll();

    int updateFields(InventoryDto dto);

    int incrementIn(@Param("id") Long id, @Param("qty") int qty);

    int incrementOut(@Param("id") Long id, @Param("qty") int qty);

    List<InventoryStockDto> findStockByBarcode(String barcode);
}
