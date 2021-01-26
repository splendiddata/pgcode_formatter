SELECT
	group_concat(k.COLUMN_NAME ORDER BY k.ordinal_position) AS column_names,
	t.TABLE_NAME AS table_name,
	t.table_schema AS table_schema,
	t.CONSTRAINT_NAME AS constraint_name
	FROM
	information_schema.table_constraints
										   t
		LEFT JOIN information_schema.key_column_usage
										   k USING (CONSTRAINT_NAME, table_schema, TABLE_NAME)
	WHERE
		t.constraint_type = 'PRIMARY KEY'
	GROUP BY
		t.table_schema, t.TABLE_NAME;
