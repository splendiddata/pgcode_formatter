create or replace function literal_with_newline_character
        ( q varchar
        , s varchar 
                  DEFAULT '
                  ,'::varchar
        , f varchar 
                  DEFAULT NULL::varchar
        , l varchar 
                  DEFAULT NULL::varchar )
returns integer
language plpgsql
security definer
as $function$
DECLARE
    i          integer := 0;
BEGIN
    RETURN i;
END;
$function$
