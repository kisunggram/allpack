package com.allpack.rm.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;

import com.allpack.rm.dto.BarcodeDto;
import com.allpack.rm.service.BarcodeService;
import com.allpack.rm.store.StoreRegistry;

import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
@Controller
public class BarcodeController {

    private final BarcodeService barcodeService;
    private final StoreRegistry storeRegistry;
    

    @GetMapping("/")
    public String main(Model model)
    {
        return "redirect:/barcode";
    }

    @GetMapping("/api--test")
    public String test(Model model)
    {
        return "barcode/apitest";
    }

    @GetMapping("/barcode")
    public ModelAndView barcode(Model model, 
        @RequestParam(value = "store", required = false) String store) {

        if (store == null || store.isEmpty())
            store = storeRegistry.getStoreIds().get(0);

        int totalCnt = 0;
        int scanCnt = 0;
        // 바코드 목록 조회
        List<BarcodeDto> barcodes = barcodeService.GetTargerProduct(store);
        if (barcodes!= null) {
            for (BarcodeDto bcd : barcodes) {
                totalCnt++;
                if (bcd.getScanDate() != null)
                    scanCnt++;
            }
        }
        
        log.info("### G[/barcode] store:{}, barcodesCnt:{}", store, barcodes.size());

        ModelAndView mv = new ModelAndView("barcode/upload");
        mv.addObject("StoreId", storeRegistry.getStoreIds());
        mv.addObject("StoreName", storeRegistry.getStoreNames());
        mv.addObject("store", store);
        mv.addObject("totalCnt", totalCnt);
        mv.addObject("scanCnt", scanCnt);
        mv.addObject("barcodes", barcodes);
        return mv;
    }

    @PostMapping("/barcode")
    public ModelAndView barcode(
        @RequestParam(value = "store", required = false) String store,
        @RequestParam(value = "clear", required = false) Integer clear,
        @RequestParam("excel") MultipartFile excelFile) {

        // (입력용)엑셀파일 저장
        String errMsg = barcodeService.SaveBarcode(store, clear != null && clear == 1, excelFile);

        ModelAndView mv = new ModelAndView();
        if ("".equals(errMsg)) {
            mv.setViewName("redirect:/barcode?store="+store);
        } else {
            mv.setViewName("barcode/error");
            mv.addObject("errMsg", errMsg);
        }
        return mv;
    }

    @PostMapping("/barcode/relocate")
    public ModelAndView relocate(
        @RequestParam(value = "store", required = false) String store,
        @RequestParam(value = "barcode", required = false) String[] barcode,
        @RequestParam(value = "reloc", required = false) String[] reloc) {

        // (입력용)엑셀파일 저장
        String errMsg = barcodeService.Relocate(store, barcode, reloc);

        ModelAndView mv = new ModelAndView();
        if ("".equals(errMsg)) {
            mv.setViewName("redirect:/barcode?store="+store);
        } else {
            mv.setViewName("barcode/error");
            mv.addObject("errMsg", errMsg);
        }
        return mv;
    }

    @PostMapping("/barcode/download")
    public void download(
        @RequestParam(value = "store", required = false) String store,
        HttpServletResponse response) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
	    String filename = String.format("%s_%s.xlsx", storeRegistry.getStoreNames().get(store), formatter.format(new Date()));

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", String.format("attachment;filename=%s", URLEncoder.encode(filename, StandardCharsets.UTF_8)));

        try {
            String errMsg = barcodeService.GetExcel(store, storeRegistry.getStoreNames().get(store), response.getOutputStream());
            if (!errMsg.isEmpty()) {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write(errMsg);
            }
        } catch (Exception ex) {
            log.error("### [download] store: {}, Ex: {}", store, ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}