package com.allpack.rm.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import com.allpack.rm.dto.FixedHistoryDto;
import com.allpack.rm.mapper.FixedScanMapper;
import com.allpack.rm.service.FixedReturnService;
import com.allpack.rm.store.StoreRegistry;

@Slf4j
@RequiredArgsConstructor
@Controller
public class FixedHistoryController {

    private final FixedReturnService fixedReturnService;
    private final FixedScanMapper fixedScanMapper;
    private final StoreRegistry storeRegistry;

    @GetMapping("/fixed/history")
    public ModelAndView history(
            @RequestParam(value = "store", required = false) String store,
            @RequestParam(value = "date", required = false) String date) {

        if (store == null || store.isEmpty()) store = storeRegistry.getStoreIds().get(0);

        String latestUploadDate = fixedReturnService.getLatestUploadDate(store);
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<FixedHistoryDto> historyList = Collections.emptyList();
        if (date != null && !date.isEmpty() && latestUploadDate != null) {
            historyList = fixedScanMapper.getHistoryByDate(store, latestUploadDate, date);
        }

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

    @GetMapping("/fixed/history/dates")
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
}
