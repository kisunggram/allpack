package com.allpack.rm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.allpack.rm.dto.FixedReturnDto;

@Mapper
public interface FixedReturnMapper {

    List<FixedReturnDto> getListByStoreAndDate(@Param("store") String store, @Param("uploadDate") String uploadDate);

    String getLatestUploadDate(String store);

    int insertReturn(FixedReturnDto dto);

    int deleteByStoreAndDate(@Param("store") String store, @Param("uploadDate") String uploadDate);

    int updateReturn(FixedReturnDto dto);

    List<String> getDistinctDates(@Param("store") String store, @Param("startDate") String startDate, @Param("endDate") String endDate);

    int existsReturn(@Param("store") String store,
                     @Param("mainBarcode") String mainBarcode,
                     @Param("uploadDate") String uploadDate);
}
