package com.allpack.rm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.allpack.rm.dto.ApiResultDto;
import com.allpack.rm.mapper.InventoryMapper;

@Slf4j
@RequiredArgsConstructor
@RestController
public class InventoryApiController {

    private final InventoryMapper inventoryMapper;

    @PostMapping("/api/inventory/in")
    public ApiResultDto stockIn(@RequestParam("barcode") Long barcode) {
        try {
            inventoryMapper.incrementIn(barcode);
            log.info("### [Inventory.in] barcode: {}", barcode);
            return ApiResultDto.success("ok");
        } catch (Exception ex) {
            log.error("### [Inventory.in] barcode: {}, Ex: {}", barcode, ex.getMessage());
            return ApiResultDto.fail(ex.getMessage());
        }
    }

    @PostMapping("/api/inventory/out")
    public ApiResultDto stockOut(@RequestParam("barcode") Long barcode) {
        try {
            inventoryMapper.incrementOut(barcode);
            log.info("### [Inventory.out] barcode: {}", barcode);
            return ApiResultDto.success("ok");
        } catch (Exception ex) {
            log.error("### [Inventory.out] barcode: {}, Ex: {}", barcode, ex.getMessage());
            return ApiResultDto.fail(ex.getMessage());
        }
    }
}
