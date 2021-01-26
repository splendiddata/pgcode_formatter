CREATE OR REPLACE FUNCTION union_in_declarationPart(a VARCHAR)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $function$
DECLARE
    curs1      CURSOR IS SELECT *
                         FROM tab1
                         union
                         SELECT * FROM tab1 WHERE a = b LIMIT 1;
    x          INTEGER;
BEGIN
    curs1 := NULL;
END;
$function$
