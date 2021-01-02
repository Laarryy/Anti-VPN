CREATE TABLE IF NOT EXISTS `avpn_ip` (
	`id` INT NOT NULL AUTO_INCREMENT,
	`version` INT,
	`created` DATETIME,
	`modified` DATETIME,
	`ip` VARCHAR(45),
	`type` INT,
	`cascade` INT,
	`consensus` INT,
	PRIMARY KEY( `id` )
);

CREATE TABLE IF NOT EXISTS `avpn_player` (
	`id` INT NOT NULL AUTO_INCREMENT,
	`version` INT,
	`created` DATETIME,
	`modified` DATETIME,
	`uuid` VARCHAR(36),
	`mcleaks` INT,
	PRIMARY KEY( `id` )
);