-- Test an empty function body
CREATE OR REPLACE FUNCTION empty_function_body()
RETURNS void
LANGUAGE sql
SECURITY DEFINER
AS $function$
$function$
