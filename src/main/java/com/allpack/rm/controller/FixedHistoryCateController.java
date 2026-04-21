package com.allpack.rm.controller;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletResponse;

import com.allpack.rm.dto.FixedHistoryDto;
import com.allpack.rm.mapper.FixedScanMapper;
import com.allpack.rm.service.FixedReturnService;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@Controller
public class FixedHistoryCateController {

    private final FixedReturnService fixedReturnService;
    private final FixedScanMapper fixedScanMapper;
    private final StoreRegistry storeRegistry;

    @GetMapping("/fixed/history-cate")
    public ModelAndView historyCate(
            @RequestParam(value = "store", required = false) String store,
            @RequestParam(value = "date", required = false) String date) {

        if (store == null || store.isEmpty()) store = storeRegistry.getStoreIds().get(0);

        String latestUploadDate = fixedReturnService.getLatestUploadDate(store);
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<FixedHistoryDto> historyList = Collections.emptyList();
        if (date != null && !date.isEmpty() && latestUploadDate != null) {
            historyList = fixedScanMapper.getCategoryHistoryByDate(store, latestUploadDate, date);
        }

        ModelAndView mv = new ModelAndView("fixed/history-cate");
        mv.addObject("StoreId", storeRegistry.getStoreIds());
        mv.addObject("StoreName", storeRegistry.getStoreNames());
        mv.addObject("store", store);
        mv.addObject("todayDate", today);
        mv.addObject("selectedDate", date);
        mv.addObject("uploadDate", latestUploadDate);
        mv.addObject("historyList", historyList);
        return mv;
    }

    @GetMapping("/fixed/history-cate/dates")
    @ResponseBody
    public List<String> availableDates(
            @RequestParam("store") String store,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {

        YearMonth ym = YearMonth.of(year, month);
        String startDate = ym.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = ym.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return fixedScanMapper.getScanDates(store, startDate, endDate);
    }

    @PostMapping("/fixed/history-cate/download")
    public void download(
            @RequestParam("store") String store,
            @RequestParam(value = "uploadDate", required = false) String uploadDate,
            @RequestParam(value = "date", required = false) String date,
            HttpServletResponse response) {

        String storeName = storeRegistry.getStoreNames().get(store);
        String filename = storeName + "_분류고정내역_" + (date != null ? date : "")
                + "_" + new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date()) + ".xlsx";
        ExcelResponseUtil.setExcelResponse(response, filename);

        List<FixedHistoryDto> list = (uploadDate != null && date != null)
                ? fixedScanMapper.getCategoryHistoryByDate(store, uploadDate, date)
                : Collections.emptyList();

        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(storeName + "_분류고정내역");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("장소");
            header.createCell(1).setCellValue("바코드");
            header.createCell(2).setCellValue("품목명");

            for (FixedHistoryDto dto : list) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(dto.getLocation());
                row.createCell(1).setCellValue(dto.getMainBarcode());
                row.createCell(2).setCellValue(dto.getProduct());
            }
            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 3500);
            sheet.setColumnWidth(2, 10000);

            OutputStream os = response.getOutputStream();
            workbook.write(os);
            workbook.dispose();
        } catch (Exception ex) {
            log.error("### [history-cate/download] Ex: {}", ex.getMessage());
            ExcelResponseUtil.writeError(response, ex.getMessage());
        }
    }
}
