DO $do$
BEGIN
	IF NOT exists (
		SELECT *
		FROM pg_catalog.pg_proc
		WHERE pronamespace = 'myschema'::REGNAMESPACE
			  AND proname = 'details'
			  AND proargtypes[0] = 'numeric'::REGTYPE
			  AND array_length(proargtypes, 1) = 1 ) THEN
		CREATE FUNCTION myschema.details (IN NUMERIC, OUT OUT_REC REFCURSOR)
		AS $$
DECLARE
	stack TEXT;
BEGIN
	GET DIAGNOSTICS stack := PG_CONTEXT;
	RAISE EXCEPTION 'function: % is only a prototype', regexp_replace(
		stack,
		'PL/pgSQL function (.*) line [0-9]+ at GET DIAGNOSTICS',
		E'\\1' ) USING HINT = 'please implement';
END
$$
		LANGUAGE plpgsql;
	END IF;
END
$do$
LANGUAGE plpgsql;
