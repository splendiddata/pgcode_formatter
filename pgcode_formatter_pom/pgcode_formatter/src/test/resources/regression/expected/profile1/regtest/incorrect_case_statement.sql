CASE WHEN (val IS NULL OR val = '') AND (start_date < 2020) THEN '1' WHEN (val IS NULL OR val = '') AND (start_date >= 2020) THEN '2' WHEN
	(val IS NULL OR val = '') AND (start_date IS NULL OR end_date IS NULL) THEN '3' END AS val;
