CREATE OR REPLACE FUNCTION double_backslash(text VARCHAR)
RETURNS VARCHAR
LANGUAGE plpgsql
SECURITY DEFINER
AS $function$
DECLARE
    varchar                   a;
BEGIN
    a := regexp_replace(TEXT, '^\S+ function (.*) line [0-9]+ at .*', E'\\1');

    RETURN a::VARCHAR;
END;
$function$
