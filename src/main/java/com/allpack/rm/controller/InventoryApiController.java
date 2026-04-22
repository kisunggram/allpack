package com.allpack.rm.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.allpack.rm.dto.ApiResultDto;
import com.allpack.rm.dto.InventoryStockDto;
import com.allpack.rm.mapper.InventoryMapper;

@Slf4j
@RequiredArgsConstructor
@RestController
public class InventoryApiController {

    private final InventoryMapper inventoryMapper;

    @GetMapping("/api/inventory")
    public List<InventoryStockDto> findByBarcode(@RequestParam("barcode") String barcode) {
        return inventoryMapper.findStockByBarcode(barcode);
    }

    @PostMapping("/api/inventory/in")
    public ApiResultDto stockIn(@RequestParam("id") Long id,
                                @RequestParam("qty") int qty) {
        try {
            int affected = inventoryMapper.incrementIn(id, qty);
            if (affected == 0) {
                log.warn("### [Inventory.in] id: {} 재고가 존재하지 않습니다.", id);
                return ApiResultDto.fail("해당 재고가 존재하지 않습니다.");
            }
            log.info("### [Inventory.in] id: {}, qty: {}", id, qty);
            return ApiResultDto.success("ok");
        } catch (Exception ex) {
            log.error("### [Inventory.in] id: {}, qty: {}, Ex: {}", id, qty, ex.getMessage());
            return ApiResultDto.fail(ex.getMessage());
        }
    }

    @PostMapping("/api/inventory/out")
    public ApiResultDto stockOut(@RequestParam("id") Long id,
                                 @RequestParam("qty") int qty) {
        try {
            int affected = inventoryMapper.incrementOut(id, qty);
            if (affected == 0) {
                log.warn("### [Inventory.out] id: {} 재고가 존재하지 않습니다.", id);
                return ApiResultDto.fail("해당 재고가 존재하지 않습니다.");
            }
            log.info("### [Inventory.out] id: {}, qty: {}", id, qty);
            return ApiResultDto.success("ok");
        } catch (Exception ex) {
            log.error("### [Inventory.out] id: {}, qty: {}, Ex: {}", id, qty, ex.getMessage());
            return ApiResultDto.fail(ex.getMessage());
        }
    }
}
