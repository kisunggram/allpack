package com.allpack.rm.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;

import com.allpack.rm.dto.ApiResultDto;
import com.allpack.rm.dto.FixedCategoryDto;
import com.allpack.rm.dto.FixedHistoryDto;
import com.allpack.rm.dto.FixedReturnDto;
import com.allpack.rm.dto.UploadResultDto;
import com.allpack.rm.mapper.FixedScanMapper;
import com.allpack.rm.service.FixedCategoryService;
import com.allpack.rm.service.FixedReturnService;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@Controller
public class FixedController {

    private final FixedCategoryService fixedCategoryService;
    private final FixedReturnService fixedReturnService;
    private final FixedScanMapper fixedScanMapper;
    private final StoreRegistry storeRegistry;

    // ========== 분류등록 ==========

    @GetMapping("/fixed/category")
    public ModelAndView category(@RequestParam(value = "store", required = false) String store) {
        if (store == null || store.isEmpty()) store = storeRegistry.getStoreIds().get(0);

        List<FixedCategoryDto> list = fixedCategoryService.getList(store);

        ModelAndView mv = new ModelAndView("fixed/category");
        mv.addObject("StoreId", storeRegistry.getStoreIds());
        mv.addObject("StoreName", storeRegistry.getStoreNames());
        mv.addObject("store", store);
        mv.addObject("list", list);
        return mv;
    }

    @PostMapping("/fixed/category/upload")
    public ModelAndView categoryUpload(
            @RequestParam("store") String store,
            @RequestParam("excel") MultipartFile excelFile,
            RedirectAttributes redirectAttributes) {

        UploadResultDto result = fixedCategoryService.uploadCategory(store, excelFile);

        ModelAndView mv = new ModelAndView();
        if (result.hasError()) {
            mv.setViewName("barcode/error");
            mv.addObject("errMsg", result.getError());
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", result);
            mv.setViewName("redirect:/fixed/category?store=" + store);
        }
        return mv;
    }

    @PostMapping("/fixed/category/relocate")
    @ResponseBody
    public ApiResultDto categoryRelocate(
            @RequestParam("store") String store,
            @RequestParam("mainBarcode") String mainBarcode,
            @RequestParam("location") String location) {

        String errMsg = fixedCategoryService.updateLocation(store, mainBarcode, location);
        if (!errMsg.isEmpty()) {
            return ApiResultDto.fail(errMsg);
        }
        return ApiResultDto.success("ok");
    }

    // ========== 반품등록 ==========

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

    // ========== 일자별내역 ==========

    @GetMapping("/fixed/history")
    public ModelAndView history(
            @RequestParam(value = "store", required = false) String store,
            @RequestParam(value = "date", required = false) String date) {

        if (store == null || store.isEmpty()) store = storeRegistry.getStoreIds().get(0);

        // 반품등록 업로드 일자
        String latestUploadDate = fixedReturnService.getLatestUploadDate(store);

        // 선택된 날짜의 상세 데이터
        List<FixedHistoryDto> historyList = Collections.emptyList();
        if (date != null && !date.isEmpty() && latestUploadDate != null) {
            historyList = fixedScanMapper.getHistoryByDate(store, latestUploadDate, date);
        }

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        ModelAndView mv = new ModelAndView("fixed/history");
        mv.addObject("StoreId", storeRegistry.getStoreIds());
        mv.addObject("StoreName", storeRegistry.getStoreNames());
        mv.addObject("store", store);
        mv.addObject("todayDate", today);
        mv.addObject("selectedDate", date);
        mv.addObject("uploadDate", latestUploadDate);
        mv.addObject("historyList", historyList);
        return mv;
    }

    // ========== 엑셀 다운로드 ==========

    @PostMapping("/fixed/category/download")
    public void categoryDownload(
            @RequestParam("store") String store,
            HttpServletResponse response) {

        String storeName = storeRegistry.getStoreNames().get(store);
        String filename = storeName + "_분류등록_" + new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date()) + ".xlsx";
        setExcelResponse(response, filename);

        try {
            String errMsg = fixedCategoryService.getExcel(store, storeName, response.getOutputStream());
            if (!errMsg.isEmpty()) writeError(response, errMsg);
        } catch (Exception ex) {
            log.error("### [category/download] Ex: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/fixed/return/download")
    public void returnDownload(
            @RequestParam("store") String store,
            @RequestParam(value = "date", required = false) String date,
            HttpServletResponse response) {

        String storeName = storeRegistry.getStoreNames().get(store);
        String filename = storeName + "_반품등록_" + new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date()) + ".xlsx";
        setExcelResponse(response, filename);

        try {
            String errMsg = fixedReturnService.getExcel(store, storeName, date, response.getOutputStream());
            if (!errMsg.isEmpty()) writeError(response, errMsg);
        } catch (Exception ex) {
            log.error("### [return/download] Ex: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void setExcelResponse(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
    }

    private void writeError(HttpServletResponse response, String errMsg) {
        try {
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write(errMsg);
        } catch (Exception ignored) {}
    }
}
