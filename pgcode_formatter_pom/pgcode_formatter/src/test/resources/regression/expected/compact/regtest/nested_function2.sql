CREATE OR REPLACE FUNCTION OUTER()
RETURNS VOID
AS $outer$
DECLARE
	s TEXT;
BEGIN
	CREATE OR REPLACE FUNCTION INNER()
	RETURNS TEXT
	AS $inner$
DECLARE
	s TEXT;
BEGIN
	CREATE OR REPLACE FUNCTION innerInner()
	RETURNS TEXT
	AS $innerInner$
BEGIN
	RETURN 'returned by innerInner function';
END;
$innerInner$
	LANGUAGE plpgsql;

	SELECT innerInner() INTO s;
	RAISE NOTICE '%', s;

	RETURN 'returned by inner function';
END;
$inner$
	LANGUAGE plpgsql;

	SELECT inner() INTO s;
	RAISE NOTICE '%', s;
END;
$outer$
LANGUAGE plpgsql;
