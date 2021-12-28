-- Test an empty function body
CREATE OR REPLACE FUNCTION empty_function_body()
RETURNS VOID
LANGUAGE SQL
SECURITY DEFINER
AS $function$
$function$
