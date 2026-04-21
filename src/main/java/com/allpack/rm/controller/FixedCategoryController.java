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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;

import com.allpack.rm.dto.ApiResultDto;
import com.allpack.rm.dto.FixedCategoryDto;
import com.allpack.rm.dto.UploadResultDto;
import com.allpack.rm.service.FixedCategoryService;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@Controller
public class FixedCategoryController {

    private final FixedCategoryService fixedCategoryService;
    private final StoreRegistry storeRegistry;

    @GetMapping("/fixed/category")
    public ModelAndView category(@RequestParam(value = "store", required = false) String store) {
        if (store == null || store.isEmpty()) store = storeRegistry.getStoreIds().get(0);

        List<FixedCategoryDto> list = fixedCategoryService.getList(store);

        ModelAndView mv = new ModelAndView("fixed/category");
        mv.addObject("StoreId", storeRegistry.getStoreIds());
        mv.addObject("StoreName", storeRegistry.getStoreNames());
        mv.addObject("store", store);
        mv.addObject("list", list);
        mv.addObject("passwordRequired", true);
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

    @PostMapping("/fixed/category/insert")
    @ResponseBody
    public ApiResultDto categoryInsert(
            @RequestParam("store") String store,
            @RequestParam("mainBarcode") String mainBarcode,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "location", required = false) String location) {

        String errMsg = fixedCategoryService.insertRow(store, mainBarcode, product, location);
        if (!errMsg.isEmpty()) {
            return ApiResultDto.fail(errMsg);
        }
        return ApiResultDto.success("ok");
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

    @PostMapping("/fixed/category/download")
    public void categoryDownload(
            @RequestParam("store") String store,
            HttpServletResponse response) {

        String storeName = storeRegistry.getStoreNames().get(store);
        String filename = storeName + "_분류등록_" + new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date()) + ".xlsx";
        ExcelResponseUtil.setExcelResponse(response, filename);

        try {
            String errMsg = fixedCategoryService.getExcel(store, storeName, response.getOutputStream());
            if (!errMsg.isEmpty()) ExcelResponseUtil.writeError(response, errMsg);
        } catch (Exception ex) {
            log.error("### [category/download] Ex: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
