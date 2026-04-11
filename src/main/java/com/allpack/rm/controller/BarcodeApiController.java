package com.allpack.rm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.allpack.rm.dto.ReturnDto;
import com.allpack.rm.service.BarcodeService;

@Slf4j
@RequiredArgsConstructor
@RestController
public class BarcodeApiController {
    
    private final BarcodeService barcodeService;

    @PostMapping("/api/return")
    public ReturnDto sacan(
        @RequestParam(value = "store", required = true) String store,
		@RequestParam(value = "code", required = true) String code ) {
        
        // 바코드 유효성
        // 12번째 자리는 패리티로 11자리의 + 1 값. xxxx3 -> xxxx34, xxxx9 -> xxxx9a
        String barcode = code.trim();
        if (("etc".equals(store) == false) && (barcode.length() > 11))
            barcode = barcode.substring(0, 11);

        ReturnDto result = barcodeService.ScanBarcode(store, barcode);
        
        return result;
    }
}
