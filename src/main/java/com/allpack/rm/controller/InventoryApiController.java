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
    public ApiResultDto stockIn(@RequestParam("id") Long id) {
        try {
            inventoryMapper.incrementIn(id);
            log.info("### [Inventory.in] id: {}", id);
            return ApiResultDto.success("ok");
        } catch (Exception ex) {
            log.error("### [Inventory.in] id: {}, Ex: {}", id, ex.getMessage());
            return ApiResultDto.fail(ex.getMessage());
        }
    }

    @PostMapping("/api/inventory/out")
    public ApiResultDto stockOut(@RequestParam("id") Long id) {
        try {
            inventoryMapper.incrementOut(id);
            log.info("### [Inventory.out] id: {}", id);
            return ApiResultDto.success("ok");
        } catch (Exception ex) {
            log.error("### [Inventory.out] id: {}, Ex: {}", id, ex.getMessage());
            return ApiResultDto.fail(ex.getMessage());
        }
    }
}
