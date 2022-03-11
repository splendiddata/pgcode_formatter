/* comment */
CREATE OR REPLACE FUNCTION "increment"
        (     i integer
        , out b numeric(20)
        )
AS $function$
BEGIN
    a := CASE
         WHEN b THEN true
                ELSE false
         END;
    RETURN 'test';
END;
$function$
LANGUAGE plpgsql
immutable
security definer
not leakproof;

create or replace function add_one(integer)
returns integer
as '/usr/lib/postgresql/9.1/lib/add_one', 'add_one'
language c;

CREATE FUNCTION pystrip(x text)
RETURNS text
AS $$
  global x
  x = x.strip()  # ok now
  return x
$$
LANGUAGE plpythonu;

CREATE OR REPLACE FUNCTION plpgsql_inline_handler_test(internal)
RETURNS void
LANGUAGE c STRICT
AS '$libdir/plpgsql' /* comment test */ , $function$plpgsql_inline_handler$function$
security definer
not leakproof;
