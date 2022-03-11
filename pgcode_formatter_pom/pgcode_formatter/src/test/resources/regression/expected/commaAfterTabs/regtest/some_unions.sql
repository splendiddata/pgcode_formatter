SELECT
	'a'
	UNION
	SELECT
		'b';
SELECT
	'a'
	UNION ALL
	SELECT
		('b'), ('c');
VALUES ('a'),('b'),('c') EXCEPT
	SELECT
		'b';
VALUES ('a'),('b'),('c') INTERSECT
	SELECT
		'b';
