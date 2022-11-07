CREATE TABLE measurement_year_month 
        ( logdate             DATE NOT NULL
        , peaktemp            INT
        , unitsales           INT )
    PARTITION BY RANGE (EXTRACT(YEAR FROM logdate), EXTRACT(MONTH FROM logdate));

CREATE TABLE my_schema.pktable
        ( ptest1        INT
        , CONSTRAINT pk_base1 PRIMARY KEY(base1)
        , CONSTRAINT unq_base1_ptest1 UNIQUE(base1, ptest1) )
    INHERITS (my_schema.pktable_base);

CREATE TABLE hours_to_days 
        ( DAY                     DATE                        NOT NULL REFERENCES media_calendar(gregorian)
        , time_of_day             TIME WITHOUT TIME ZONE      NOT NULL REFERENCES time_dim(time_of_day)
        , full_date               TIMESTAMP WITHOUT TIME ZONE GENERATED ALWAYS AS (DAY + time_of_day) STORED
        , PRIMARY KEY (DAY, time_of_day) )
    PARTITION BY RANGE (DAY);

CREATE TABLE hours_to_days_sep
    PARTITION OF hours_to_days FOR VALUES FROM ('2040-01-01') TO (MAXVALUE);

CREATE TABLE hours_to_days_ancient
    PARTITION OF hours_to_days FOR VALUES IN 
        ( '1988-01-01'
        , '1989-01-01'
        , '1990-01-01' ) FROM (MINVALUE) TO ('1990-01-01');

CREATE GLOBAL TEMPORARY TABLE IF NOT EXISTS films 
        ( code                CHAR(5)                 CONSTRAINT firstkey PRIMARY KEY
        , /* some
             comment here */ title               VARCHAR(40)             NOT NULL
        , -- line comment here
                  did                 INTEGER                 NOT NULL
        , date_prod           DATE
        , -- line comment 2
                  kind                VARCHAR(10)
        , len                 INTERVAL HOUR TO MINUTE );

CREATE TABLE sch.table_like_comptype (LIKE sch.composite_type1 INCLUDING ALL);
