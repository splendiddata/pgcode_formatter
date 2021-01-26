CREATE TABLE films (
		code							   CHAR(5)	 CONSTRAINT firstkey PRIMARY KEY,
		title							   VARCHAR(40)
													 NOT NULL,
		did 							   INTEGER	 NOT NULL,
		date_prod						   DATE,
		kind							   VARCHAR(10),
		len 							   INTERVAL HOUR TO MINUTE
	);

CREATE TABLE array_int (vector							   INT[][]);

CREATE TABLE films (
		code							   CHAR(5),
		title							   VARCHAR(40),
		did 							   INTEGER,
		date_prod						   DATE,
		kind							   VARCHAR(10),
		len 							   INTERVAL HOUR TO MINUTE,
		CONSTRAINT production UNIQUE(date_prod)
	);

CREATE TABLE distributors (
		did 							   INTEGER	 CHECK (did > 100),
		NAME							   VARCHAR(40)
	);

CREATE TABLE films (
		code							   CHAR(5),
		title							   VARCHAR(40),
		did 							   INTEGER,
		date_prod						   DATE,
		kind							   VARCHAR(10),
		len 							   INTERVAL HOUR TO MINUTE,
		CONSTRAINT code_title PRIMARY KEY(code, title)
	);

CREATE TABLE distributors (
		NAME							   VARCHAR(40)
													 DEFAULT 'Luso Films',
		did 							   INTEGER	 DEFAULT nextval('distributors_serial'),
		modtime 						   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);

CREATE TABLE distributors (
		did 							   INTEGER,
		NAME							   VARCHAR(40),
		UNIQUE(NAME) WITH (fillfactor=70)
	) WITH (fillfactor=70);

CREATE TABLE measurement_year_month (
		logdate 						   DATE 	 NOT NULL,
		peaktemp						   INT,
		unitsales						   INT
	)
	PARTITION BY RANGE (
		EXTRACT(YEAR FROM logdate),
		EXTRACT(MONTH FROM logdate)
		);
