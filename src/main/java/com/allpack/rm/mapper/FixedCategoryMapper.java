package com.allpack.rm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.allpack.rm.dto.FixedCategoryDto;

@Mapper
public interface FixedCategoryMapper {

    List<FixedCategoryDto> getListByStore(String store);

    int insertCategory(FixedCategoryDto dto);

    int deleteByStore(String store);

    int updateLocation(FixedCategoryDto dto);

    int checkDuplicateLocation(@Param("store") String store,
                               @Param("location") String location,
                               @Param("mainBarcode") String mainBarcode);
}
