\set ON_ERROR_STOP ON
\set VERBOSITY default
SELECT
	*
	FROM
	emp;
\echo :a 
SELECT
	:a;
\unset a
CREATE OR REPLACE FUNCTION add_one(                         INTEGER)
RETURNS INTEGER
AS '/usr/lib/postgresql/9.1/lib/add_one', 
LANGUAGE c;
