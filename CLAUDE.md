# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**allpack rm** - Return Management (반품 관리) system for Allpack. A Spring Boot 3.4 web application that manages product returns by uploading Excel files, tracking barcodes, and scanning returns via a REST API. Stores are identified by key (`ssg`=신세계, `cjonstyle`=온스타일, `etc`=기타), each with different Excel parsing rules.

## Build & Run

```bash
# Build (WAR packaging)
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Run tests (none exist yet)
./mvnw test
```

- Java 17, Spring Boot 3.4.1
- Packaged as WAR (deployed to external Tomcat via `ServletInitializer`)
- Database: MariaDB (cafe24 hosted), accessed via MyBatis
- Template engine: Thymeleaf (templates in `src/main/resources/templates/`)
- Excel processing: Apache POI 4.1.2 (supports both .xls and .xlsx)
- Lombok used for DTOs and dependency injection (`@Data`, `@RequiredArgsConstructor`)

## Architecture

Single-module Maven project with a straightforward MVC pattern:

- **Controller layer**: `BarcodeController` (Thymeleaf views) + `BarcodeApiController` (REST JSON API at `/api/return`)
- **Service layer**: `BarcodeService` - contains all business logic including Excel parsing, barcode aggregation, location numbering, and scan processing
- **Data layer**: `BarcodeMapper` (MyBatis interface) with XML mappings in `src/main/resources/mybatis-mapper/`
- **Single DB table**: `RETURN_TARGET` (store, barcode, qty, product, location, regDate, scanDate)

### Key Business Logic (in BarcodeService)

- **Excel upload**: Parses uploaded Excel with store-specific rules. SSG uses a combined `품목명` column; CJ uses separate `단품코드`/`명칭`/`수량` columns. Barcodes are deduplicated and quantities aggregated.
- **Location assignment**: Products sorted by quantity descending, then assigned sequential location numbers. Same product code across different barcodes gets sub-locations (e.g., `3-1`, `3-2`).
- **Barcode scanning API**: `POST /api/return` with `store` + `code` params. For non-`etc` stores, barcodes are truncated to 11 chars (dropping parity digit). Returns `ReturnDto` JSON.

## Warning

`application.properties` contains hardcoded database credentials. These should be externalized (e.g., environment variables or Spring profiles) before any public deployment.
