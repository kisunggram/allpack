package com.allpack.rm.store;

import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import com.allpack.rm.dto.BarcodeDto;

/**
 * Store별 바코드/엑셀 파싱 규칙 인터페이스.
 * 새 Store 추가 시 이 인터페이스를 구현하고 StoreRegistry에 등록한다.
 */
public interface StoreParser {

    /** store id (ssg, cjonstyle, etc) */
    String getId();

    /** 표시 이름 (신세계, 온스타일, 기타) */
    String getName();

    /**
     * 스캔된 바코드에서 메인바코드를 추출한다.
     * 고정관리 스캔 API, 일반관리 스캔 API 모두에서 사용.
     */
    String extractMainBarcode(String code);

    /**
     * 일반관리 스캔 시 바코드 전처리 (패리티 제거 등).
     */
    String truncateScanBarcode(String code);

    /**
     * 분류등록 엑셀 행 파싱.
     * @param colIdx 헤더 칼럼명 → 인덱스 맵
     * @return 파싱 결과, null이면 skip
     */
    CategoryParseResult parseCategoryRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx);

    /**
     * 반품등록 엑셀 행 파싱.
     * @return 파싱 결과, null이면 skip
     */
    ReturnParseResult parseReturnRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx);

    /**
     * 일반관리 엑셀 행 파싱.
     * @return BarcodeDto, null이면 skip
     */
    BarcodeDto parseGeneralRow(Row row, DataFormatter formatter, Map<String, Integer> colIdx);

    // ===== 파싱 결과 DTO =====

    class CategoryParseResult {
        public String mainBarcode;
        public String subBarcode;
        public String product;

        public CategoryParseResult(String mainBarcode, String subBarcode, String product) {
            this.mainBarcode = mainBarcode;
            this.subBarcode = subBarcode;
            this.product = product;
        }
    }

    class ReturnParseResult {
        public String mainBarcode;
        public int qty;

        public ReturnParseResult(String mainBarcode, int qty) {
            this.mainBarcode = mainBarcode;
            this.qty = qty;
        }
    }
}
