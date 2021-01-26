SELECT
	n.nspname AS "Schema",
	p.proname AS "Name",
	pg_catalog.pg_get_function_result(p.OID) AS "Result data type",
	pg_catalog.pg_get_function_arguments(p.OID) AS "Argument data types",
	CASE
		WHEN p.proisagg THEN 'agg'
		WHEN p.proiswindow THEN 'window'
		WHEN p.prorettype = 'pg_catalog.trigger'::pg_catalog.REGTYPE THEN 'trigger'
		ELSE 'normal'
	END AS "Type"
	FROM
	pg_catalog.pg_proc p LEFT JOIN pg_catalog.pg_namespace n ON n.OID = p.pronamespace
	WHERE
		p.proname ~ '^(version)$'
		AND pg_catalog.pg_function_is_visible(p.OID)
	ORDER BY
		1, 2, 4;

SELECT
	CASE
		WHEN (FALSE) THEN 0
		WHEN (TRUE) THEN 2
	END AS dummy1
	FROM
	my_table;
