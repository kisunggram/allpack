package com.allpack.rm.service;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.allpack.rm.dto.BarcodeDto;
import com.allpack.rm.dto.ReturnDto;
import com.allpack.rm.mapper.BarcodeMapper;


@Slf4j
@RequiredArgsConstructor
@Service
public class BarcodeService {

    private final BarcodeMapper barcodeMapper;

    // 반품제품 목록을 얻기
    public List<BarcodeDto> GetTargerProduct(String store) {
        return barcodeMapper.getProductList(store);
    }

    public String GetExcel(String store, String storeName, OutputStream ost) {
        String errMsg = "";
        try {
            Workbook workbook = new SXSSFWorkbook();
            Sheet sheet = workbook.createSheet(storeName);

            List<BarcodeDto> barcodes = barcodeMapper.getProductList(store);
            // 헤더를 생성
            // 바코드, 제품, 수량, 장소
            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            Cell headerCell1 = headerRow.createCell(0); headerCell1.setCellValue("바코드");
            Cell headerCell2 = headerRow.createCell(1); headerCell2.setCellValue("제품");
            Cell headerCell3 = headerRow.createCell(2); headerCell3.setCellValue("수량");
            Cell headerCell4 = headerRow.createCell(3); headerCell4.setCellValue("장소");
            headerRow.setHeight((short) 600);
            
            // 바디에 데이터를 넣어준다.	
            for (BarcodeDto dto : barcodes) {
                Row bodyRow = sheet.createRow(rowIndex++);
                Cell bodyCell1 = bodyRow.createCell(0); bodyCell1.setCellValue(dto.getBarcode());
                Cell bodyCell2 = bodyRow.createCell(1); bodyCell2.setCellValue(dto.getProduct());
                Cell bodyCell3 = bodyRow.createCell(2); bodyCell3.setCellValue(dto.getQty());
                Cell bodyCell4 = bodyRow.createCell(3); bodyCell4.setCellValue(dto.getLocation());
            }
            sheet.setColumnWidth(0, 3500);
            sheet.setColumnWidth(1, 10000);

            workbook.write(ost);
            workbook.close();
        } catch(Exception ex) {
            errMsg = ex.getMessage();
            log.error("### [GetExcel] store: {}, Ex: {}", store, ex.getMessage());
        }
        return errMsg;
    }

    
    // 반품 등록을 위한 임시(엑셀파일) 저장.
    public String SaveBarcode(String store, Boolean clear, MultipartFile file) {
        
        String originalName = file.getOriginalFilename();
        log.info("### [SaveTemp] store: {}, file: {}, size: {}", store, originalName, file.getSize());

        Workbook workbook = null;  
        try {
            // 엑셀 97 - 2003 까지는 HSSF(xls),  엑셀 2007 이상은 XSSF(xlsx)
            if (originalName.endsWith(".xls"))
                workbook = new HSSFWorkbook(file.getInputStream());
            else
                workbook = new XSSFWorkbook(file.getInputStream());

            Map<ColIdx, Integer> rstMap = new HashMap<ColIdx, Integer>();
            Sheet worksheet = workbook.getSheetAt(0);
            String errHdr = CheckHeader(worksheet.getRow(0), rstMap);
            if (errHdr != null)
                return errHdr;

            if (clear)
                barcodeMapper.clearProduct(store);

            List<BarcodeDto> barcodeList = new ArrayList<BarcodeDto>(); // 바코드 저장소 (중복없음)
            Map<String, BarcodeDto> barcodeTempMap = new HashMap<String, BarcodeDto>(); // 바코드별 맵
            
            int rowSize = worksheet.getPhysicalNumberOfRows();
            for (int i = 1; i < rowSize; i++) {
                Row row = worksheet.getRow(i);
                if (row == null) 
                    continue;
                
                BarcodeDto dto = ParseRow(row, rstMap);
                dto.setStore(store);
                
                // 바코드의 수량 누적하여 집계.
                String barcode = dto.getBarcode();
                if (!barcodeTempMap.containsKey(barcode)) {
                    barcodeTempMap.put(barcode, dto);
                    barcodeList.add(dto);
                } else {
                    barcodeTempMap.get(barcode).AddQty(dto.getQty());
                }
            }
            barcodeTempMap.clear();
            workbook.close();
                
            // 1. 수량 많은 순서로 정렬.
            Collections.sort(barcodeList, Collections.reverseOrder());

            // 2. 장소 메인번호 채번. (수량많은 상품(바코드X) 순서로)
            int locNo = 1;
            Map<String, List<BarcodeDto>> prdTempMap = new HashMap<String, List<BarcodeDto>>(); // <상품코드, 바코드저장소>
            for (BarcodeDto dto : barcodeList) {
                String prdCode = dto.getPrdCode(); 
                if (prdTempMap.containsKey(prdCode) == false) {
                    prdTempMap.put(prdCode, new ArrayList<BarcodeDto>(){{ add(dto); }});
                    dto.locNo = locNo++;
                    dto.setLocation(Integer.toString(dto.locNo));
                } else {
                    List<BarcodeDto> list = prdTempMap.get(prdCode);
                    int no = list.get(0).locNo;
                    if (list.size() == 1) 
                        list.get(0).setLocation(String.format("%d-%d", no, 1)); // 3 -> 3-1
                    list.add(dto);
                    dto.locNo = no;
                    dto.setLocation(String.format("%d-%d", no, list.size()));
                } 
            }

            // 저장
            for (BarcodeDto dto : barcodeList) {
                barcodeMapper.insProduct(dto);
                //log.info("### barcode: {}, qty: {}, locatin: {}, Name: {}", dto.getBarcode(), dto.getQty(), dto.getLocation(), dto.getProduct());
            }
        } catch (Exception ex) {
            log.error("### [SaveTemp] store: {}, file: {}, Ex: {}", store, originalName, ex.getMessage());
            throw new RuntimeException(ex);
        }

        return "";
    }

    
    private String CheckHeader(Row rowHd, Map<ColIdx, Integer> rstMap) {
  
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < rowHd.getPhysicalNumberOfCells(); i++) {
            Cell cell = rowHd.getCell(i);
            if (cell == null) 
                continue;
            String cellVal = formatter.formatCellValue(cell);
            switch(cellVal)
            {
                case "품목명": 
                    rstMap.put(ColIdx.PRD, i); break;
                case "단품코드": 
                    rstMap.put(ColIdx.ITEM, i); break;
                case "명칭": 
                    rstMap.put(ColIdx.NAME, i); break;
                case "수량": 
                    rstMap.put(ColIdx.QTY, i); break;
                default: continue;
            }
        }

        // 컬럼명 체크
        if (nvl(rstMap.get(ColIdx.PRD)) == -1) {
            if (nvl(rstMap.get(ColIdx.ITEM)) == -1) 
                return "[단품코드]를 찾을 수 없습니다.";
            if (nvl(rstMap.get(ColIdx.NAME)) == -1) 
                return "[명칭]를 찾을 수 없습니다.";
            if (nvl(rstMap.get(ColIdx.QTY)) == -1) 
                return "[수량]를 찾을 수 없습니다.";
            if (nvl(rstMap.get(ColIdx.ITEM)) == -1 && nvl(rstMap.get(ColIdx.NAME)) == -1 && nvl(rstMap.get(ColIdx.QTY)) == -1) 
                return "[품목명]을 찾을 수 없습니다.";
        }
        return null;
    }
    private int nvl(Integer v) {
        return v == null ? -1 : v;
    }

    private BarcodeDto ParseRow(Row row, Map<ColIdx, Integer> idxMap) {
        DataFormatter formatter = new DataFormatter();
        BarcodeDto dto = new BarcodeDto();

        if (idxMap.get(ColIdx.PRD) != null)
        {
            // 신세계 쇼핑몰 규칙
            Cell prdCell = row.getCell(idxMap.get(ColIdx.PRD));
            if (prdCell == null)
                return null;

            String prdVal = formatter.formatCellValue(prdCell);
            dto.PrdCode = NextToken(prdVal, 0);
            dto.ItemCode = NextToken(prdVal, dto.PrdCode.length()+1);
            dto.setBarcode(dto.PrdCode + dto.ItemCode);
            dto.setProduct(NextToken(prdVal, dto.getBarcode().length()+2));
            dto.setQty(1);

        } else {
            // CJ 온스타일 규칙
            Cell itemCell = row.getCell(idxMap.get(ColIdx.ITEM));
            Cell nameCell = row.getCell(idxMap.get(ColIdx.NAME));
            Cell qtyCell = row.getCell(idxMap.get(ColIdx.QTY));
            if (itemCell == null || nameCell == null || qtyCell == null)
                return null;

            String item = formatter.formatCellValue(itemCell);
            dto.PrdCode = item.substring(0, 8);
            dto.ItemCode = item.substring(8);
            dto.setBarcode(item);
            dto.setProduct(formatter.formatCellValue(nameCell));
            dto.setQty(Integer.parseInt(formatter.formatCellValue(qtyCell)));
        }
        return dto;
    }
    private String NextToken(String str, int startpos)
    {
        str = str.substring(startpos);
        int idx = str.indexOf("-");
        return idx == -1 ? null : str.substring(0, idx);
    }

    // 위치 수정
    public String Relocate(String store, String[] barcodes, String[] locations) {

        if (barcodes.length != locations.length)
            return "바코드와 장소의 개수가 일치하지 않습니다.";

        String errMsg = "";
        try {
            for (int i = 0; i < barcodes.length; i++) {
                BarcodeDto reloc = new BarcodeDto();
                reloc.setStore(store);
                reloc.setBarcode(barcodes[i]);
                reloc.setLocation(locations[i]);
                barcodeMapper.setLocation(reloc);
            }
        } catch(Exception ex) {
            errMsg = ex.getMessage();
            log.error("### [Relocate] store: {}, Ex: {}", store, ex.getMessage());
        }
        
        return errMsg;
    }

    // API
    public ReturnDto ScanBarcode(String store, String barcode) {
        ReturnDto result = new ReturnDto();
        try {
            BarcodeDto find = new BarcodeDto();
            find.setStore(store);
            find.setBarcode(barcode);

            List<BarcodeDto> products = barcodeMapper.getBarcode(find);
            int cnt = 0;
            if (products != null && products.size() > 0) {
                cnt = barcodeMapper.scanBarcode(find); // 스캔처리 일자 설정
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
        }
        catch(Exception ex) {
            result.setResult(false);
            result.setMessage(ex.getMessage());
            log.error("### [SetBarcode] store: {}, barcode: {}, Ex: {}", store, barcode, ex.getMessage());
        }

        return result;
    }
}

enum ColIdx {
    PRD, ITEM, NAME, QTY
    // 품목명, 단품코드, 명칭, 수량
}
