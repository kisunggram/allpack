package com.allpack.rm.controller;

import com.allpack.rm.dto.UtilCategoryDto;
import com.allpack.rm.service.UtilService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Controller
public class UtilController {

    private final UtilService utilService;

    @GetMapping("/util/excel")
    public ModelAndView category() {
        List<UtilCategoryDto> list = utilService.getList();

        ModelAndView mv = new ModelAndView("util/excel");
        mv.addObject("list", list);
        return mv;
    }

    @PostMapping("/util/excel/upload")
    public ModelAndView inventoryUpload(@RequestParam("excel") MultipartFile excelFile) {
        String errMsg = utilService.uploadExcel(excelFile);

        ModelAndView mv = new ModelAndView();
        if ("".equals(errMsg)) {
            mv.setViewName("redirect:/util/excel");
        } else {
            mv.setViewName("barcode/error");
            mv.addObject("errMsg", errMsg);
        }
        return mv;
    }

    @PostMapping("/util/excel/download")
    public void inventoryDownload(HttpServletResponse response) {
        String filename = "분류고정_" + new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date()) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        try {
            String errMsg = utilService.getExcel(response.getOutputStream());
            if (!errMsg.isEmpty()) {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write(errMsg);
            }
        } catch (Exception ex) {
            log.error("### [util/excel/download] Ex: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
