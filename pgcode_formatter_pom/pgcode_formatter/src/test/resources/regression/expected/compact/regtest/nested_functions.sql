CREATE OR REPLACE FUNCTION outer()
RETURNS void
AS $outer$
DECLARE
	s text;
BEGIN
	CREATE OR REPLACE FUNCTION inner()
	RETURNS text
	AS $inner$
BEGIN
	RETURN 'returned by inner function';
END;
$inner$
	language plpgsql;
	SELECT inner() INTO s;
	RAISE NOTICE '%', s;
END;
$outer$
language plpgsql;
