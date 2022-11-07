CREATE OR REPLACE FUNCTION OUTER()
RETURNS VOID
AS $outer$
DECLARE
    s                         TEXT;
BEGIN
    CREATE OR REPLACE FUNCTION INNER()
    RETURNS TEXT
    AS $inner$
BEGIN
    RETURN 'returned by inner function';
END;
$inner$
    LANGUAGE plpgsql;

    SELECT inner() INTO s;
    RAISE NOTICE '%', s;
END;
$outer$
LANGUAGE plpgsql;
