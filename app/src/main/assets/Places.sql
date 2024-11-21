BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "Places" (
	"name"	TEXT,
	"longitude"	REAL NOT NULL,
	"latitude"	REAL NOT NULL,
	"id"	INTEGER,
	"region"	TEXT,
	PRIMARY KEY("id")
);
INSERT INTO "Places" VALUES ('Chicago',-87.6298,41.8781,1,'Illinois');
INSERT INTO "Places" VALUES ('Los Angeles',-118.2426,34.0549,2,'California');
INSERT INTO "Places" VALUES ('New York',-74.006,40.7128,3,'New york');
INSERT INTO "Places" VALUES ('Bascom Hill',-89.40413,43.075349,4,'Wisconsin');
INSERT INTO "Places" VALUES ('Union South',-89.408129,43.07193,5,'Wisconsin');
INSERT INTO "Places" VALUES ('Memorial Library',-89.397986,43.075214,6,'Wisconsin');
INSERT INTO "Places" VALUES ('Nitty Gritty',-89.39564,43.07183,7,'Wisconsin');
COMMIT;
