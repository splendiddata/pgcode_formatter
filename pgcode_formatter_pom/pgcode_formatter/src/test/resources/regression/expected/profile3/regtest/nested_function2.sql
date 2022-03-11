CREATE OR REPLACE FUNCTION outer()
RETURNS void
AS $outer$
DECLARE
    s          text;
BEGIN
    CREATE OR REPLACE FUNCTION inner()
    RETURNS text
    AS $inner$
DECLARE
    s          text;
BEGIN
    CREATE OR REPLACE FUNCTION innerInner()
    RETURNS text
    AS $innerInner$
BEGIN
    RETURN 'returned by innerInner function';
END;
$innerInner$
    language plpgsql;

    SELECT innerInner() INTO s;
    RAISE NOTICE '%', s;

    RETURN 'returned by inner function';
END;
$inner$
    language plpgsql;

    SELECT inner() INTO s;
    RAISE NOTICE '%', s;
END;
$outer$
language plpgsql;
