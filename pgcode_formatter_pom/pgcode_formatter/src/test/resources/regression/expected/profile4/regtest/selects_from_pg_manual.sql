WITH RECURSIVE
    employee_recursive (distance, employee_name, manager_name) AS 
        ( SELECT 1, employee_name, manager_name
          FROM employee
          WHERE manager_name = 'Mary'
          UNION ALL
          SELECT er.distance + 1, e.employee_name, e.manager_name
          FROM employee_recursive er, employee e
          WHERE er.employee_name = e.manager_name
        )
SELECT distance, employee_name FROM employee_recursive;

WITH RECURSIVE t(nombre) AS (VALUES (2) UNION ALL SELECT 2 * nombre FROM t WHERE 2 * nombre < 100) SELECT nombre FROM t;

SELECT f.title, f.did, d.name, f.date_prod, f.kind
FROM distributors d, films f
WHERE f.did = d.did;

CREATE FUNCTION distributors(int)
RETURNS SETOF distributors
AS $$
    SELECT * FROM distributors WHERE did = $1;
$$
LANGUAGE SQL;

WITH t AS (SELECT random() as x FROM generate_series(1, 3))
SELECT *
FROM t
UNION ALL
SELECT * FROM t;

SELECT m.name AS mname, pname
FROM manufacturers m, LATERAL get_product_names(m.id) pname;

SELECT m.name AS mname, pname
FROM manufacturers                             m
     LEFT JOIN LATERAL get_product_names(m.id) pname ON true;
