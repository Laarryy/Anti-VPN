DROP TABLE "avpn_6_ip";

CREATE TABLE "avpn_6_ip" (
	"id"	INTEGER NOT NULL,
	"version"	INTEGER NOT NULL,
	"created"	INTEGER NOT NULL,
	"modified"	INTEGER NOT NULL,
	"ip"	TEXT NOT NULL UNIQUE,
	"type"	INTEGER NOT NULL,
	"cascade"	INTEGER,
	"consensus"	REAL,
	PRIMARY KEY("id" AUTOINCREMENT)
);