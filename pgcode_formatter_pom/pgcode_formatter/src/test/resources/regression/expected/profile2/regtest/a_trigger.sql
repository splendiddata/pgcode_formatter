CREATE OR REPLACE FUNCTION foo ()
RETURNS TRIGGER
AS $$
BEGIN
	CREATE TEMPORARY TABLE tb (id    INTEGER);
	SELECT
		*
		FROM
		NOTHING;
END;
$$
LANGUAGE 'plpgsql';
