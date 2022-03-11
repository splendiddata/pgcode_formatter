CREATE OR REPLACE FUNCTION literal_with_newline_character(
														 q                        VARCHAR,
														 s                        VARCHAR        DEFAULT
																								 '
                  ,'::VARCHAR,                           f                        VARCHAR        DEFAULT
																								 NULL::VARCHAR,
														 l                        VARCHAR        DEFAULT
																								 NULL::VARCHAR
														 )
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $function$
DECLARE
	i                              INTEGER := 0;
BEGIN
	RETURN i;
END;
$function$
