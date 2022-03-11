CREATE TABLE films 
        ( code                char(5)                 CONSTRAINT firstkey PRIMARY KEY
        , title               varchar(40)             NOT NULL
        , did                 integer                 NOT NULL
        , date_prod           date
        , kind                varchar(10)
        , len                 interval hour to minute
        );

CREATE TABLE array_int (vector         int[][]);

CREATE TABLE films 
        ( code                char(5)
        , title               varchar(40)
        , did                 integer
        , date_prod           date
        , kind                varchar(10)
        , len                 interval hour to minute
        , CONSTRAINT production UNIQUE(date_prod)
        );

CREATE TABLE distributors 
        ( did            integer                  CHECK (did > 100)
        , name           varchar(40)
        );

CREATE TABLE films 
        ( code                char(5)
        , title               varchar(40)
        , did                 integer
        , date_prod           date
        , kind                varchar(10)
        , len                 interval hour to minute
        , CONSTRAINT code_title PRIMARY KEY(code, title)
        );

CREATE TABLE distributors 
        ( name            varchar(40)             DEFAULT 'Luso Films'
        , did             integer                 DEFAULT nextval('distributors_serial')
        , modtime         timestamp               DEFAULT current_timestamp
        );

CREATE TABLE distributors 
        ( did            integer
        , name           varchar(40)
        , UNIQUE(name) WITH (fillfactor=70)
        ) WITH (fillfactor=70);

CREATE TABLE measurement_year_month 
        ( logdate             date                not null
        , peaktemp            int
        , unitsales           int
        )
    PARTITION BY RANGE (EXTRACT(YEAR FROM logdate), EXTRACT(MONTH FROM logdate));
