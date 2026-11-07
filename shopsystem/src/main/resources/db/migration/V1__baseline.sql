-- V1: esquema base, tal y como lo creaba Hibernate (ddl-auto=update) antes de usar Flyway.
--
-- En una base de datos NUEVA se ejecuta tal cual.
-- En la base de datos de PRODUCCIÓN (que ya tiene estas tablas) NO se ejecuta: Flyway la
-- marca como aplicada ("baseline", spring.flyway.baseline-on-migrate=true) y solo aplica
-- las migraciones siguientes.
--
-- No modificar nunca una migración ya publicada: los cambios van en una versión nueva.

-- Las tablas van en orden alfabético: las claves foráneas se comprueban al final.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `billing_chain` (
  `id` varchar(10) NOT NULL,
  `last_fecha` varchar(10) DEFAULT NULL,
  `last_hash` varchar(64) DEFAULT NULL,
  `last_nif` varchar(9) DEFAULT NULL,
  `last_num_serie` varchar(60) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `billing_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aeat_error_code` varchar(10) DEFAULT NULL,
  `aeat_error_description` varchar(1500) DEFAULT NULL,
  `attempts` int NOT NULL,
  `csv` varchar(32) DEFAULT NULL,
  `fecha_expedicion` varchar(10) NOT NULL,
  `generated_at` varchar(32) NOT NULL,
  `hash` varchar(64) NOT NULL,
  `importe_total` decimal(12,2) DEFAULT NULL,
  `last_attempt_at` datetime(6) DEFAULT NULL,
  `next_attempt_at` datetime(6) DEFAULT NULL,
  `nif` varchar(9) NOT NULL,
  `num_serie` varchar(60) NOT NULL,
  `previous_hash` varchar(64) DEFAULT NULL,
  `registro_xml` varchar(8000) NOT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `status` enum('ACCEPTED','ACCEPTED_WITH_ERRORS','PENDING','REJECTED') NOT NULL,
  `trade_id` bigint NOT NULL,
  `type` enum('ALTA','ANULACION') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_billing_status` (`status`,`id`),
  KEY `idx_billing_trade` (`trade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `categories` (
  `id` bigint NOT NULL,
  `igic_rate` decimal(5,2) DEFAULT NULL,
  `image` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `invoice_counters` (
  `series` varchar(20) NOT NULL,
  `last_number` bigint NOT NULL,
  PRIMARY KEY (`series`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `items` (
  `id` bigint NOT NULL,
  `bare_minimun` int DEFAULT '0',
  `ignore_stock` tinyint(1) DEFAULT '0',
  `image` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `stock` int DEFAULT '0',
  `category_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjcdcde7htb3tyjgouo4g9xbmr` (`category_id`),
  CONSTRAINT `FKjcdcde7htb3tyjgouo4g9xbmr` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `net_history` (
  `item_id` bigint NOT NULL,
  `net` double DEFAULT NULL,
  `date` datetime(6) NOT NULL,
  PRIMARY KEY (`item_id`,`date`),
  CONSTRAINT `FKb7s41o1qa0bg6ysbxch94npd2` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `price_history` (
  `item_id` bigint NOT NULL,
  `price` double DEFAULT NULL,
  `date` datetime(6) NOT NULL,
  PRIMARY KEY (`item_id`,`date`),
  CONSTRAINT `FKsxcwl5qf7kphxip0j9pm9dr6k` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `trade_lines` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) NOT NULL,
  `item_id` bigint DEFAULT NULL,
  `line_total` decimal(12,2) NOT NULL,
  `quantity` int NOT NULL,
  `tax_amount` decimal(12,2) NOT NULL,
  `tax_base` decimal(12,2) NOT NULL,
  `tax_rate` decimal(5,2) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `trade_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8vfbss4safvhhxcvfuk4cped9` (`trade_id`),
  CONSTRAINT `FK8vfbss4safvhhxcvfuk4cped9` FOREIGN KEY (`trade_id`) REFERENCES `trades` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `trades` (
  `id` bigint NOT NULL,
  `cancellation_reason` varchar(255) DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `date` datetime(6) DEFAULT NULL,
  `number` bigint DEFAULT NULL,
  `payment_method` varchar(10) NOT NULL DEFAULT 'CASH',
  `series` varchar(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ISSUED',
  `total` decimal(12,2) DEFAULT NULL,
  `total_base` decimal(12,2) DEFAULT NULL,
  `total_tax` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_series_number` (`series`,`number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `trades_items` (
  `trades_id` bigint NOT NULL,
  `items_id` bigint NOT NULL,
  KEY `FKsui3iijlev5vdx4ojtrs4dvma` (`items_id`),
  KEY `FK7yt22oasw9k980hii1hmro95r` (`trades_id`),
  CONSTRAINT `FK7yt22oasw9k980hii1hmro95r` FOREIGN KEY (`trades_id`) REFERENCES `trades` (`id`),
  CONSTRAINT `FKsui3iijlev5vdx4ojtrs4dvma` FOREIGN KEY (`items_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `trades_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `users` (
  `id` bigint NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `users_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- Contadores de las secuencias de Hibernate (GenerationType.AUTO)
INSERT INTO `trades_seq` (`next_val`) VALUES (1);
INSERT INTO `users_seq` (`next_val`) VALUES (1);
