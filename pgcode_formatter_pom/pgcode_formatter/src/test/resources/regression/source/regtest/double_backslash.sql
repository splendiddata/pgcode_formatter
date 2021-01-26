create or replace function double_backslash(text varchar)
 returns varchar
 language plpgsql
 security definer
as $function$
DECLARE
    varchar a;
BEGIN
    a := regexp_replace(text, '^\S+ function (.*) line [0-9]+ at .*', E'\\1');

    RETURN a::varchar;
END;
$function$