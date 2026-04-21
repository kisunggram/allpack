package com.allpack.rm.controller;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletResponse;

import com.allpack.rm.dto.ApiResultDto;
import com.allpack.rm.dto.FixedReturnDto;
import com.allpack.rm.service.FixedReturnService;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@Controller
public class FixedReturnController {

    private final FixedReturnService fixedReturnService;
    private final StoreRegistry storeRegistry;

    @GetMapping("/fixed/return")
    public ModelAndView fixedReturn(
            @RequestParam(value = "store", required = false) String store,
            @RequestParam(value = "date", required = false) String date) {
        if (store == null || store.isEmpty()) store = storeRegistry.getStoreIds().get(0);

        String latestDate = fixedReturnService.getLatestUploadDate(store);
        if (date == null || date.isEmpty()) date = latestDate;

        List<FixedReturnDto> list = (date != null) ? fixedReturnService.getList(store, date) : Collections.emptyList();

        ModelAndView mv = new ModelAndView("fixed/return");
        mv.addObject("StoreId", storeRegistry.getStoreIds());
        mv.addObject("StoreName", storeRegistry.getStoreNames());
        mv.addObject("store", store);
        mv.addObject("uploadDate", date);
        mv.addObject("list", list);
        return mv;
    }

    @PostMapping("/fixed/return/upload")
    public ModelAndView returnUpload(
            @RequestParam("store") String store,
            @RequestParam("excel") MultipartFile excelFile) {

        String errMsg = fixedReturnService.uploadReturn(store, excelFile);

        ModelAndView mv = new ModelAndView();
        if ("".equals(errMsg)) {
            mv.setViewName("redirect:/fixed/return?store=" + store);
        } else {
            mv.setViewName("barcode/error");
            mv.addObject("errMsg", errMsg);
        }
        return mv;
    }

    @PostMapping("/fixed/return/update")
    @ResponseBody
    public ApiResultDto returnUpdate(
            @RequestParam("id") Long id,
            @RequestParam("qty") int qty,
            @RequestParam("vip") boolean vip,
            @RequestParam(value = "vipLocation", required = false) String vipLocation) {

        String errMsg = fixedReturnService.updateReturn(id, qty, vip, vipLocation);
        if (!errMsg.isEmpty()) {
            return ApiResultDto.fail(errMsg);
        }
        return ApiResultDto.success("ok");
    }

    @PostMapping("/fixed/return/download")
    public void returnDownload(
            @RequestParam("store") String store,
            @RequestParam(value = "date", required = false) String date,
            HttpServletResponse response) {

        String storeName = storeRegistry.getStoreNames().get(store);
        String filename = storeName + "_반품등록_" + new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date()) + ".xlsx";
        ExcelResponseUtil.setExcelResponse(response, filename);

        try {
            String errMsg = fixedReturnService.getExcel(store, storeName, date, response.getOutputStream());
            if (!errMsg.isEmpty()) ExcelResponseUtil.writeError(response, errMsg);
        } catch (Exception ex) {
            log.error("### [return/download] Ex: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
