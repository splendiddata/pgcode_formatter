DO $$
DECLARE
	r							   VARCHAR;
BEGIN
	SELECT
		a
		INTO
			r
		FROM
			coord;
	RAISE NOTICE 'Value: %', r;
END
$$
LANGUAGE plpgsql;
