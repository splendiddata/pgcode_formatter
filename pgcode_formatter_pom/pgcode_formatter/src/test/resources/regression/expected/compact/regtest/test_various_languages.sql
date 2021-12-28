/* comment */
CREATE OR REPLACE FUNCTION "increment"(i INTEGER, OUT b NUMERIC(20))
AS $function$
BEGIN
	a := CASE
		WHEN b THEN TRUE
		ELSE FALSE
		END;
	RETURN 'test';
END;
$function$
LANGUAGE plpgsql
IMMUTABLE
SECURITY DEFINER
NOT LEAKPROOF;
CREATE OR REPLACE FUNCTION add_one(INTEGER)
RETURNS INTEGER
AS '/usr/lib/postgresql/9.1/lib/add_one', 'add_one'
LANGUAGE c;
CREATE FUNCTION pystrip(x TEXT)
RETURNS TEXT
AS $$
  global x
  x = x.strip()  # ok now
  return x
$$
LANGUAGE plpythonu;
CREATE OR REPLACE FUNCTION plpgsql_inline_handler_test(INTERNAL)
RETURNS VOID
LANGUAGE c STRICT
AS '$libdir/plpgsql' /* comment test */,
	$function$plpgsql_inline_handler$function$
SECURITY DEFINER
NOT LEAKPROOF;
