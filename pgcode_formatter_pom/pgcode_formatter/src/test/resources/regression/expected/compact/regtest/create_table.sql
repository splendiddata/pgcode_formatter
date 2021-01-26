CREATE TABLE measurement_year_month 
		( logdate			  date not null
		, peaktemp			  int
		, unitsales 		  int )
	PARTITION BY RANGE (
		EXTRACT(YEAR FROM logdate)
		, EXTRACT(MONTH FROM logdate) );
create table my_schema.pktable
		( ptest1		int
		, constraint pk_base1 primary key(base1)
		, constraint unq_base1_ptest1 unique(base1, ptest1) )
	inherits (my_schema.pktable_base);
CREATE TABLE hours_to_days 
		( day					  date						  not null references media_calendar(gregorian)
		, time_of_day			  time without time zone	  not null references time_dim(time_of_day)
		, full_date 			  timestamp without time zone GENERATED ALWAYS AS (day + time_of_day) stored
		, PRIMARY KEY (day, time_of_day) )
	PARTITION BY RANGE (day);
CREATE TABLE hours_to_days_sep
	PARTITION OF hours_to_days FOR VALUES FROM ('2040-01-01') TO (maxvalue);
CREATE TABLE hours_to_days_ancient
	PARTITION OF hours_to_days FOR VALUES in ('1988-01-01', '1989-01-01', '1990-01-01') FROM (minvalue) TO ('1990-01-01');
CREATE GLOBAL TEMPORARY TABLE if not exists films 
		( code				  char(5)				  CONSTRAINT firstkey PRIMARY KEY
		, /* some
			 comment here */ title				 varchar(40)			 NOT NULL
		, -- line comment here
		did 				integer 				NOT NULL
		, date_prod 		  date
		, -- line comment 2
		kind				varchar(10)
		, len				  interval hour to minute );
create table sch.table_like_comptype (like sch.composite_type1 including all);
