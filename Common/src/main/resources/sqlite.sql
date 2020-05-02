PRAGMA foreign_keys = OFF;

DROP TABLE IF EXISTS `{prefix}`;
DROP TABLE IF EXISTS `{prefix}consensus`;
DROP TABLE IF EXISTS `{prefix}queue`;
DROP TABLE IF EXISTS `{prefix}consensus_queue`;

DROP TABLE IF EXISTS `{prefix}data`;
CREATE TABLE `{prefix}data` (
	"key"	TEXT NOT NULL UNIQUE,
	"value"	TEXT NOT NULL
);

DROP TABLE IF EXISTS `{prefix}ips`;
CREATE TABLE `{prefix}ips` (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"ip"	TEXT NOT NULL UNIQUE
);

DROP TABLE IF EXISTS `{prefix}players`;
CREATE TABLE `{prefix}players` (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"uuid"	TEXT NOT NULL UNIQUE
);

DROP TABLE IF EXISTS `{prefix}vpn_values`;
CREATE TABLE `{prefix}vpn_values` (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"ip_id"	INTEGER NOT NULL UNIQUE,
	"cascade"	BOOLEAN,
	"consensus"	DOUBLE,
	"created"	TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY("ip_id") REFERENCES "{prefix}ips"("id") ON DELETE RESTRICT ON UPDATE RESTRICT
);

DROP TABLE IF EXISTS `{prefix}mcleaks_values`;
CREATE TABLE `{prefix}mcleaks_values` (
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"player_id"	INTEGER NOT NULL UNIQUE,
	"result"	BOOLEAN NOT NULL,
	"created"	TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY("player_id") REFERENCES "{prefix}players"("id") ON DELETE RESTRICT ON UPDATE RESTRICT
);

PRAGMA foreign_keys = ON;