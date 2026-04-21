-- 고정관리 - 분류등록
CREATE TABLE IF NOT EXISTS FIXED_CATEGORY (
  store        VARCHAR(20)  NOT NULL,
  mainBarcode  VARCHAR(20)  NOT NULL,
  subBarcode   VARCHAR(20)  DEFAULT NULL,
  product      VARCHAR(255) DEFAULT NULL,
  location     VARCHAR(20)  DEFAULT NULL,
  regDate      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (store, mainBarcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 고정관리 - 반품등록
CREATE TABLE IF NOT EXISTS FIXED_RETURN (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  store        VARCHAR(20)  NOT NULL,
  mainBarcode  VARCHAR(20)  NOT NULL,
  qty          INT          NOT NULL DEFAULT 0,
  vip          TINYINT(1)   NOT NULL DEFAULT 0,
  vipLocation  VARCHAR(20)  DEFAULT NULL,
  uploadDate   DATE         NOT NULL,
  updateDate   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixed_return (store, mainBarcode, uploadDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 고정관리 - 스캔 이력
CREATE TABLE IF NOT EXISTS FIXED_SCAN (
  store        VARCHAR(20)  NOT NULL,
  mainBarcode  VARCHAR(20)  NOT NULL,
  uploadDate   DATE         NOT NULL,
  scanDate     DATE         NOT NULL,
  scanCount    INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (store, mainBarcode, uploadDate, scanDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 재고관리
CREATE TABLE IF NOT EXISTS INVENTORY (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  area         VARCHAR(50)  DEFAULT NULL,
  place        VARCHAR(50)  DEFAULT NULL,
  location     VARCHAR(50)  DEFAULT NULL,
  barcode      VARCHAR(50)  DEFAULT NULL,
  product      VARCHAR(255) DEFAULT NULL,
  itemCode     VARCHAR(50)  DEFAULT NULL,
  itemName     VARCHAR(255) DEFAULT NULL,
  baseQty      INT          NOT NULL DEFAULT 0,
  inQty        INT          NOT NULL DEFAULT 0,
  outQty       INT          NOT NULL DEFAULT 0,
  regDate      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_inventory_barcode (barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS UTILE_CATEGORY (
  location     VARCHAR(20)  DEFAULT NULL,
  barcode      VARCHAR(20)  NOT NULL,
  product      VARCHAR(255) DEFAULT NULL,
  qty          INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
