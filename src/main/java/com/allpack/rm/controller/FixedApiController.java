package com.allpack.rm.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.allpack.rm.dto.ApiResultDto;
import com.allpack.rm.mapper.FixedReturnMapper;
import com.allpack.rm.mapper.FixedScanMapper;
import com.allpack.rm.store.StoreParser;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@RestController
public class FixedApiController {

    private final FixedScanMapper fixedScanMapper;
    private final FixedReturnMapper fixedReturnMapper;
    private final StoreRegistry storeRegistry;

    @PostMapping("/api/fixed/scan")
    public ApiResultDto scan(
            @RequestParam("store") String store,
            @RequestParam("code") String code) {

        StoreParser parser = storeRegistry.getParser(store);
        String mainBarcode = parser.extractMainBarcode(code.trim());

        String uploadDate = fixedReturnMapper.getLatestUploadDate(store);
        if (uploadDate == null) {
            return ApiResultDto.fail("반품등록 데이터가 없습니다.");
        }

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            fixedScanMapper.upsertScan(store, mainBarcode, uploadDate, today);
            log.info("### [FixedScan] store: {}, barcode: {}, uploadDate: {}, scanDate: {}", store, mainBarcode, uploadDate, today);
            return ApiResultDto.success("ok");
        } catch (Exception ex) {
            log.error("### [FixedScan] store: {}, barcode: {}, Ex: {}", store, mainBarcode, ex.getMessage());
            return ApiResultDto.fail(ex.getMessage());
        }
    }
}
