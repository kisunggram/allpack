package com.allpack.rm.mapper;

import com.allpack.rm.dto.UtilCategoryDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UtilCategoryMapper {
    List<UtilCategoryDto> getList();

    int insertCategory(UtilCategoryDto dto);

    int deleteCategoryAll();
}
