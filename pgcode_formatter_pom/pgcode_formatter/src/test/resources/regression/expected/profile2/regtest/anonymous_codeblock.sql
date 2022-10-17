DO $$
DECLARE
	r   VARCHAR;
BEGIN
	SELECT
		a
		INTO
			r
		FROM
			coord;
	RAISE NOTICE 'Value: %', r;
END
$$;

DO $$
BEGIN
	RAISE NOTICE '%', (
		SELECT
			'a'
		);
END -- with some end-of-line comment and /* before the dollardollar quote
$$

DO $do$
DECLARE
	some_variable INT := 0;
	a_constant    CONSTANT TEXT := 'a default value';
BEGIN
	SELECT
		a_column
		FROM
			some_schema.some_table
		INTO
			some_variable
		WHERE
			another_column > a_constant;
	another STATEMENT containing the word EXCEPTION, which has NO meaning here;
exception
	WHEN NOT found THEN
		RAISE NOTICE 'not found';
		INSERT INTO some_schema.some_table(a_column, another_column, reason, create_timestamp)
			VALUES
				(-1, 'this value is wrong', $text$the value was not found$text$, CURRENT_TIMESTAMP);
		RETURN -1;
	WHEN other     THEN
		RAISE NOTICE $msg$something went very wriong$msg$;
		RETURN -2;
END;
$do$;
