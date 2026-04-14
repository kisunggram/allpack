CREATE TABLE `RETURN_TARGET` (
  `store` varchar(255) DEFAULT NULL COMMENT '...',
  `barcode` varchar(255) DEFAULT NULL COMMENT '...',
  `qty` int(11) DEFAULT '0',
  `product` varchar(255) DEFAULT NULL COMMENT '...',
  `location` varchar(255) DEFAULT NULL COMMENT '... ..',
  `regDate` datetime NOT NULL COMMENT '...',
  `scanDate` datetime DEFAULT NULL COMMENT '...'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='... ... ..';
