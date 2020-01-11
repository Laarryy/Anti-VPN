DROP TABLE IF EXISTS `{prefix}`;
DROP TABLE IF EXISTS `{prefix}consensus`;
DROP TABLE IF EXISTS `{prefix}queue`;
DROP TABLE IF EXISTS `{prefix}consensus_queue`;

DROP TABLE IF EXISTS `{prefix}data`;
CREATE TABLE `{prefix}data` (
  `id` tinyint(3) unsigned NOT NULL AUTO_INCREMENT,
  `key` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `{prefix}name_UNIQUE` (`key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `{prefix}ips`;
CREATE TABLE `{prefix}ips` (
  `id` bigint(8) unsigned NOT NULL AUTO_INCREMENT,
  `ip` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `{prefix}ip_UNIQUE` (`ip`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `{prefix}players`;
CREATE TABLE `{prefix}players` (
  `id` bigint(8) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `{prefix}uuid_UNIQUE` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `{prefix}vpn_values`;
CREATE TABLE `{prefix}vpn_values` (
  `id` bigint(8) unsigned NOT NULL AUTO_INCREMENT,
  `ip_id` bigint(8) unsigned NOT NULL,
  `cascade` boolean,
  `consensus` double(5, 4) unsigned,
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `{prefix}fk_vpn_values_ip_id_UNIQUE` (`ip_id`),
  CONSTRAINT `{prefix}fk_vpn_values_ip_id` FOREIGN KEY (`ip_id`) REFERENCES `{prefix}ips` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `{prefix}mcleaks_values`;
CREATE TABLE `{prefix}mcleaks_values` (
  `id` bigint(8) unsigned NOT NULL AUTO_INCREMENT,
  `player_id` bigint(8) unsigned NOT NULL,
  `result` boolean NOT NULL,
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `{prefix}fk_mcleaks_values_player_id_UNIQUE` (`player_id`),
  CONSTRAINT `{prefix}fk_mcleaks_values_player_id` FOREIGN KEY (`player_id`) REFERENCES `{prefix}players` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `{prefix}get_vpn_ip`;
DELIMITER ;;
CREATE PROCEDURE `{prefix}get_vpn_ip`(`ip_id` BIGINT UNSIGNED, `cache_time_millis` BIGINT)
BEGIN
  DECLARE `from` DATETIME DEFAULT DATE_SUB(CURRENT_TIMESTAMP, INTERVAL `cache_time_millis` * 1000 MICROSECOND);
  SELECT
    `v`.`id`,
    `i`.`ip` AS `ip`,
    `v`.`cascade`,
    `v`.`consensus`,
    `v`.`created`
  FROM `{prefix}vpn_values` `v`
  JOIN `{prefix}ips` `i` ON `i`.`id` = `v`.`ip_id`
  WHERE `v`.`created` >= `from` AND `v`.`ip_id` = `ip_id`;
END ;;
DELIMITER ;

DROP PROCEDURE IF EXISTS `{prefix}get_mcleaks_player`;
DELIMITER ;;
CREATE PROCEDURE `{prefix}get_mcleaks_player`(`player_id` BIGINT UNSIGNED, `cache_time_millis` BIGINT)
BEGIN
  DECLARE `from` DATETIME DEFAULT DATE_SUB(CURRENT_TIMESTAMP, INTERVAL `cache_time_millis` * 1000 MICROSECOND);
  SELECT
    `v`.`id`,
    `p`.`uuid` AS `player_id`,
    `v`.`value`,
    `v`.`created`
  FROM `{prefix}mcleaks_values` `v`
  JOIN `{prefix}players` `p` ON `p`.`id` = `v`.`player_id`
  WHERE `v`.`created` >= `from` AND `v`.`player_id` = `player_id`;
END ;;
DELIMITER ;

DROP PROCEDURE IF EXISTS `{prefix}get_vpn_queue_date`;
DELIMITER ;;
CREATE PROCEDURE `{prefix}get_vpn_queue_date`(`after` DATETIME)
BEGIN
  SELECT
    `v`.`id`,
    `i`.`ip` AS `ip`,
    `v`.`cascade`,
    `v`.`consensus`,
    `v`.`created`
  FROM `{prefix}vpn_values` `v`
  JOIN `{prefix}ips` `i` ON `i`.`id` = `v`.`ip_id`
  WHERE `v`.`created` > `after`;
END ;;
DELIMITER ;

DROP PROCEDURE IF EXISTS `{prefix}get_mcleaks_queue_date`;
DELIMITER ;;
CREATE PROCEDURE `{prefix}get_mcleaks_queue_date`(`after` DATETIME)
BEGIN
  SELECT
    `v`.`id`,
    `p`.`uuid` AS `player_id`,
    `v`.`value`,
    `v`.`created`
  FROM `{prefix}mcleaks_values` `v`
  JOIN `{prefix}players` `p` ON `p`.`id` = `v`.`player_id`
  WHERE `v`.`created` > `after`;
END ;;
DELIMITER ;

DROP PROCEDURE IF EXISTS `{prefix}get_vpn_queue_id`;
DELIMITER ;;
CREATE PROCEDURE `{prefix}get_vpn_queue_id`(`after` BIGINT UNSIGNED)
BEGIN
  SELECT
    `v`.`id`,
    `i`.`ip` AS `ip`,
    `v`.`cascade`,
    `v`.`consensus`,
    `v`.`created`
  FROM `{prefix}vpn_values` `v`
  JOIN `{prefix}ips` `i` ON `i`.`id` = `v`.`ip_id`
  WHERE `v`.`id` > `after`;
END ;;
DELIMITER ;

DROP PROCEDURE IF EXISTS `{prefix}get_mcleaks_queue_id`;
DELIMITER ;;
CREATE PROCEDURE `{prefix}get_mcleaks_queue_id`(`after` BIGINT UNSIGNED)
BEGIN
  SELECT
    `v`.`id`,
    `p`.`uuid` AS `player_id`,
    `v`.`value`,
    `v`.`created`
  FROM `{prefix}mcleaks_values` `v`
  JOIN `{prefix}players` `p` ON `p`.`id` = `v`.`player_id`
  WHERE `v`.`id` > `after`;
END ;;
DELIMITER ;