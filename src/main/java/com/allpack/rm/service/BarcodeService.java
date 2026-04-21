package com.allpack.rm.service;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.allpack.rm.dto.BarcodeDto;
import com.allpack.rm.dto.ReturnDto;
import com.allpack.rm.mapper.BarcodeMapper;
import com.allpack.rm.store.StoreParser;
import com.allpack.rm.store.StoreRegistry;
import com.allpack.rm.util.ExcelHeaderUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class BarcodeService {

    private static final Set<String> BARCODE_HEADERS = Set.of(
            "바코드", "상품코드", "명칭", "품목명", "수량");

    private final BarcodeMapper barcodeMapper;
    private final StoreRegistry storeRegistry;

    public List<BarcodeDto> GetTargerProduct(String store) {
        return barcodeMapper.getProductList(store);
    }

    public String GetExcel(String store, String storeName, OutputStream ost) {
        String errMsg = "";
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(storeName);

            List<BarcodeDto> barcodes = barcodeMapper.getProductList(store);
            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            Cell headerCell1 = headerRow.createCell(0);
            headerCell1.setCellValue("바코드");
            Cell headerCell2 = headerRow.createCell(1);
            headerCell2.setCellValue("제품");
            Cell headerCell3 = headerRow.createCell(2);
            headerCell3.setCellValue("수량");
            Cell headerCell4 = headerRow.createCell(3);
            headerCell4.setCellValue("위치");
            headerRow.setHeight((short) 600);

            for (BarcodeDto dto : barcodes) {
                Row bodyRow = sheet.createRow(rowIndex++);
                Cell bodyCell1 = bodyRow.createCell(0);
                bodyCell1.setCellValue(dto.getBarcode());
                Cell bodyCell2 = bodyRow.createCell(1);
                bodyCell2.setCellValue(dto.getProduct());
                Cell bodyCell3 = bodyRow.createCell(2);
                bodyCell3.setCellValue(dto.getQty());
                Cell bodyCell4 = bodyRow.createCell(3);
                bodyCell4.setCellValue(dto.getLocation());
            }
            sheet.setColumnWidth(0, 3500);
            sheet.setColumnWidth(1, 10000);

            workbook.write(ost);
        } catch (Exception ex) {
            errMsg = ex.getMessage();
            log.error("### [GetExcel] store: {}, Ex: {}", store, ex.getMessage());
        } finally {
            workbook.dispose();
            try {
                workbook.close();
            } catch (Exception ignored) {
            }
        }
        return errMsg;
    }

    @Transactional
    public String SaveBarcode(String store, Boolean clear, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        log.info("### [SaveTemp] store: {}, file: {}, size: {}", store, originalName, file.getSize());

        StoreParser parser = storeRegistry.getParser(store);

        try (Workbook workbook = originalName.endsWith(".xls")
                ? new HSSFWorkbook(file.getInputStream())
                : new XSSFWorkbook(file.getInputStream())) {

            Sheet worksheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            int headerRowIdx = ExcelHeaderUtils.findHeaderRowIndex(worksheet, formatter, BARCODE_HEADERS, 1);
            if (headerRowIdx < 0) {
                return "헤더 행이 없습니다.";
            }

            Row headerRow = worksheet.getRow(headerRowIdx);
            Map<String, Integer> colIdx = ExcelHeaderUtils.buildColumnIndex(headerRow, formatter);

            if (clear) {
                barcodeMapper.clearProduct(store);
            }

            List<BarcodeDto> barcodeList = new ArrayList<>();
            Map<String, BarcodeDto> barcodeTempMap = new HashMap<>();

            int rowSize = worksheet.getPhysicalNumberOfRows();
            for (int i = headerRowIdx + 1; i < rowSize; i++) {
                Row row = worksheet.getRow(i);
                if (row == null) {
                    continue;
                }

                BarcodeDto dto = parser.parseGeneralRow(row, formatter, colIdx);
                if (dto == null) {
                    continue;
                }

                dto.setStore(store);

                String barcode = dto.getBarcode();
                if (!barcodeTempMap.containsKey(barcode)) {
                    barcodeTempMap.put(barcode, dto);
                    barcodeList.add(dto);
                } else {
                    barcodeTempMap.get(barcode).AddQty(dto.getQty());
                }
            }
            barcodeTempMap.clear();

            Collections.sort(barcodeList, Collections.reverseOrder());

            int locNo = 1;
            Map<String, List<BarcodeDto>> prdTempMap = new HashMap<>();
            for (BarcodeDto dto : barcodeList) {
                String prdCode = dto.getPrdCode();
                if (!prdTempMap.containsKey(prdCode)) {
                    prdTempMap.put(prdCode, new ArrayList<BarcodeDto>() {{
                        add(dto);
                    }});
                    dto.locNo = locNo++;
                    dto.setLocation(Integer.toString(dto.locNo));
                } else {
                    List<BarcodeDto> list = prdTempMap.get(prdCode);
                    int no = list.get(0).locNo;
                    if (list.size() == 1) {
                        list.get(0).setLocation(String.format("%d-%d", no, 1));
                    }
                    list.add(dto);
                    dto.locNo = no;
                    dto.setLocation(String.format("%d-%d", no, list.size()));
                }
            }

            for (BarcodeDto dto : barcodeList) {
                barcodeMapper.insProduct(dto);
            }
        } catch (Exception ex) {
            log.error("### [SaveTemp] store: {}, file: {}, Ex: {}", store, originalName, ex.getMessage());
            throw new RuntimeException(ex);
        }

        return "";
    }

    public String Relocate(String store, String[] barcodes, String[] locations) {
        if (barcodes.length != locations.length) {
            return "바코드와 위치의 개수가 일치하지 않습니다.";
        }

        String errMsg = "";
        try {
            for (int i = 0; i < barcodes.length; i++) {
                BarcodeDto reloc = new BarcodeDto();
                reloc.setStore(store);
                reloc.setBarcode(barcodes[i]);
                reloc.setLocation(locations[i]);
                barcodeMapper.setLocation(reloc);
            }
        } catch (Exception ex) {
            errMsg = ex.getMessage();
            log.error("### [Relocate] store: {}, Ex: {}", store, ex.getMessage());
        }

        return errMsg;
    }

    public ReturnDto ScanBarcode(String store, String barcode) {
        ReturnDto result = new ReturnDto();
        try {
            BarcodeDto find = new BarcodeDto();
            find.setStore(store);
            find.setBarcode(barcode);

            List<BarcodeDto> products = barcodeMapper.getBarcode(find);
            int cnt = 0;
            if (products != null && !products.isEmpty()) {
                cnt = barcodeMapper.scanBarcode(find);
            }

            if (cnt > 0) {
                result.setResult(true);
                result.setMessage("ok");
                result.setData(products.get(0));
            } else {
                result.setResult(false);
                result.setMessage("cannot find");
            }
            log.info("### [SetBarcode] store: {}, barcode: {}, result: {}", store, barcode, cnt > 0);
        } catch (Exception ex) {
            result.setResult(false);
            result.setMessage(ex.getMessage());
            log.error("### [SetBarcode] store: {}, barcode: {}, Ex: {}", store, barcode, ex.getMessage());
        }

        return result;
    }
}
