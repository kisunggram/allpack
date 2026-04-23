package com.allpack.rm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.allpack.rm.dto.FixedHistoryDto;

@Mapper
public interface FixedScanMapper {

    int upsertScan(@Param("store") String store, @Param("mainBarcode") String mainBarcode,
                   @Param("uploadDate") String uploadDate, @Param("scanDate") String scanDate);

    List<FixedHistoryDto> getHistoryByDate(@Param("store") String store,
                                           @Param("uploadDate") String uploadDate,
                                           @Param("scanDate") String scanDate);

    List<String> getScanDates(@Param("store") String store,
                              @Param("startDate") String startDate,
                              @Param("endDate") String endDate);

    List<FixedHistoryDto> getCategoryHistoryByDate(@Param("store") String store,
                                                   @Param("uploadDate") String uploadDate,
                                                   @Param("scanDate") String scanDate);

    java.util.Map<String, Object> getScanDetail(@Param("store") String store,
                                                @Param("mainBarcode") String mainBarcode,
                                                @Param("uploadDate") String uploadDate,
                                                @Param("scanDate") String scanDate);
}
