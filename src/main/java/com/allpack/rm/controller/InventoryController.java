package com.allpack.rm.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletResponse;

import com.allpack.rm.dto.ApiResultDto;
import com.allpack.rm.dto.InventoryDto;
import com.allpack.rm.service.InventoryService;

@Slf4j
@RequiredArgsConstructor
@Controller
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/inventory")
    public ModelAndView inventory() {
        List<InventoryDto> list = inventoryService.getList();

        ModelAndView mv = new ModelAndView("inventory/main");
        mv.addObject("list", list);
        return mv;
    }

    @PostMapping("/inventory/upload")
    public ModelAndView inventoryUpload(@RequestParam("excel") MultipartFile excelFile) {
        String errMsg = inventoryService.uploadInventory(excelFile);

        ModelAndView mv = new ModelAndView();
        if ("".equals(errMsg)) {
            mv.setViewName("redirect:/inventory");
        } else {
            mv.setViewName("barcode/error");
            mv.addObject("errMsg", errMsg);
        }
        return mv;
    }

    @PostMapping("/inventory/update")
    @ResponseBody
    public ApiResultDto inventoryUpdate(
            @RequestParam("id") Long id,
            @RequestParam("area") String area,
            @RequestParam("place") String place,
            @RequestParam("baseQty") int baseQty) {

        String errMsg = inventoryService.updateFields(id, area, place, baseQty);
        if (!errMsg.isEmpty()) {
            return ApiResultDto.fail(errMsg);
        }
        return ApiResultDto.success("ok");
    }

    @PostMapping("/inventory/download")
    public void inventoryDownload(HttpServletResponse response) {
        String filename = "재고관리_" + new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date()) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        try {
            String errMsg = inventoryService.getExcel(response.getOutputStream());
            if (!errMsg.isEmpty()) {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write(errMsg);
            }
        } catch (Exception ex) {
            log.error("### [inventory/download] Ex: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
