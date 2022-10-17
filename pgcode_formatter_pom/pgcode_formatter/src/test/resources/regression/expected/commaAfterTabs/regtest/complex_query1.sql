CREATE OR REPLACE VIEW v1 (
	e, id, s, t, st1, st2
	) AS WITH tab1 AS (
	SELECT
		tb1.col1, tb1.col2
		FROM
			tab2 tb1
		WHERE
			date_trunc('day', tb1.col1) = date_trunc('day', clock_timestamp())
	) , sta AS (
	SELECT
		row_number() over(ORDER BY col1 DESC) AS e,
		bl.col1 AS s,
		f1(bl.col1) over (ORDER BY col1) AS t
		FROM
			tab1 bl
		WHERE
			col2 = 'test: abc'
	) , stg AS (
	SELECT
		oid1 AS id, 'test: xyz'::TEXT AS col2,
		'a'::TEXT AS st1, 0 AS tag
		UNION ALL
		SELECT
			oid2 AS id, 'test: second' AS col2, 'b' AS st1,
			1 AS tag
			UNION ALL
			SELECT
				oid3 AS id, 'query nr 03 ... 1' AS col2,
				'query nr 03 ... 2' AS st1, 0 AS tag
				UNION ALL
				SELECT
					oid4 AS id, 'query nr 04 ... 1' AS col2,
					'query nr 04 ... 2' AS st1, 1 AS tag
					UNION ALL
					SELECT
						oid5 AS id, 'query nr 05 ... 1' AS col2,
						'query nr 05 ... 2' AS st1, 0 AS tag
						UNION ALL
						SELECT
							oid6 AS id, 'query nr 06' AS col2,
							'query nr 06' AS st1, 1 AS tag
							UNION ALL
							SELECT
								oid7 AS id, 'query nr 07 ... 1' AS col2,
								'query nr 07 ... 2' AS st1, 0 AS tag
								UNION ALL
								SELECT
									oid8 AS id, 'query nr 08 ... 1' AS col2,
									'query nr 08.... 2' AS st1, 1 AS tag
									UNION ALL
									SELECT
										oid9 AS id, 'query nr 09' AS col2,
										'query nr 09' AS st1, 0 AS tag
										UNION ALL
										SELECT
											oid10 AS id, 'query nr 10' AS col2,
											'query nr 10' AS st1, 1 AS tag
											UNION ALL
											SELECT
												oid11 AS id, 'query nr 11' AS col2,
												'query nr 11' AS st1, 0 AS tag
												UNION ALL
												SELECT
													oid12 AS id, 'query nr 12' AS col2,
													'query nr 12' AS st1, 1 AS tag
													UNION ALL
													SELECT
														oid13 AS id, 'query nr 13 ... 1' AS col2,
														'query nr 13 ... 2' AS st1, 0 AS tag
														UNION ALL
														SELECT
															oid14 AS id, 'query nr 14 ... 1' AS col2,
															'query nr 14 ... 2' AS st1, 0 AS tag
															UNION ALL
															SELECT
																oid15 AS id, 'query nr 15 ... 1' AS col2,
																'query nr 15 ... 2' AS st1, 1 AS tag
																UNION ALL
																SELECT
																	oid16 AS id, 'query nr 16 ... 1' AS col2,
																	'query nr 16 ... 2' AS st1, 0 AS tag
																	UNION ALL
																	SELECT
																		oid17 AS id, 'query nr 17 ... 1' AS col2,
																		'query nr 17 ... 2' AS st1, 1 AS tag
																		UNION ALL
																		SELECT
																			oid18 AS id, 'query nr 18 ... 1' AS col2,
																			'query nr 18 ... 2' AS st1, 0 AS tag
																			UNION ALL
																			SELECT
																				oid19 AS id, 'query nr 19 ... 1' AS col2,
																				'query nr 19 ... 2' AS st1, 1 AS tag
																				UNION ALL
																				SELECT
																					oid20 AS id, 'query nr 20 ... 1' AS col2,
																					'query nr 20 ... 2' AS st1, 0 AS tag
																					UNION ALL
																					SELECT
																						oid21 AS id, 'query nr 21 ... 1' AS col2,
																						'query nr 21 ... 2' AS st1, 0 AS tag
																						UNION ALL
																						SELECT
																							oid22 AS id, 'query nr 22 ... 1' AS col2,
																							'query nr 22 ... 2' AS st1, 0 AS tag
																							UNION ALL
																							SELECT
																								oid23 AS id, 'query nr 23 ... 1' AS col2,
																								'query nr 23 ... 2' AS st1, 1 AS tag
																								UNION ALL
																								SELECT
																									oid24 AS id, 'query nr 24 ... 1' AS col2,
																									'query nr 24 ... 2' AS st1, 1 AS tag
																									UNION ALL
																									SELECT
																										oid24 AS id,
																										'query nr 24 ... 1' AS col2,
																										'query nr 24 ... 2' AS st1, 1 AS tag
																										UNION ALL
																										SELECT
																											oid25 AS id,
																											'query nr 25 ... 1' AS col2,
																											'query nr 25 ... 2' AS st1,
																											1 AS tag
																											UNION ALL
																											SELECT
																												oid26 AS id,
																												'query nr 26 ... 1' AS col2,
																												'query nr 26 ... 2' AS st1,
																												1 AS tag
	)
	SELECT
		ch.e,
		min(stg.id) id,
		min(tb1.col1) s,
		max(tb1.col1) t,
		stg.st1,
		CASE
			WHEN min(tb1.col1) = max(tb1.col1) THEN 'case 1'
			WHEN min(tb1.col1) < max(tb1.col1) THEN 'case 2'
			ELSE 'other cases'
		END st2
		FROM
			stg
			JOIN sta			   ch ON ch.e <=100
			LEFT JOIN tab1		   tb1 ON tb1.col2 = stg.col2 AND (
			tb1.col1 >= ch.s AND tb1.col1 < coalesce(
				ch.t,
				clock_timestamp() + (1/ 60::NUMERIC /24::NUMERIC||' D')::INTERVAL
				)
			)
		GROUP BY
			ch.e, stg.st1;
