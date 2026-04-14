package com.allpack.rm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.allpack.rm.dto.ReturnDto;
import com.allpack.rm.service.BarcodeService;
import com.allpack.rm.store.StoreParser;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@RestController
public class BarcodeApiController {

    private final BarcodeService barcodeService;
    private final StoreRegistry storeRegistry;

    @PostMapping("/api/return")
    public ReturnDto scan(
        @RequestParam(value = "store", required = true) String store,
        @RequestParam(value = "code", required = true) String code ) {

        StoreParser parser = storeRegistry.getParser(store);
        String barcode = parser.truncateScanBarcode(code.trim());

        ReturnDto result = barcodeService.ScanBarcode(store, barcode);

        return result;
    }
}
