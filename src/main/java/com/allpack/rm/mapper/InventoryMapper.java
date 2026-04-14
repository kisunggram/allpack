package com.allpack.rm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

import com.allpack.rm.dto.InventoryDto;

@Mapper
public interface InventoryMapper {

    List<InventoryDto> getList();

    int insertInventory(InventoryDto dto);

    int deleteAll();

    int updateFields(InventoryDto dto);

    int incrementIn(Long id);

    int incrementOut(Long id);
}
