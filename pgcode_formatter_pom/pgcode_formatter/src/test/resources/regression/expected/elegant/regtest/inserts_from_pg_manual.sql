INSERT INTO films
VALUES ('UA502', 'Bananas', 105, '1971-07-13', 'Comedy', '82 minutes');

INSERT INTO films (code, title, did, date_prod, kind)
VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');

INSERT INTO films (code, title, did, date_prod, kind)
VALUES ('B6717', 'Tampopo', 110, '1985-02-10', 'Comedy'), ('HG120', 'The Dinner Game', 140, DEFAULT, 'Comedy');

WITH upd AS (
            UPDATE employees
                   SET sales_count = sales_count + 1
                   WHERE id = (SELECT sales_person FROM accounts WHERE NAME = 'Acme Corporation')
                   RETURNING * )
INSERT INTO employees_log
SELECT *, current_timestamp FROM upd;

-- Don't update existing distributors based in a certain ZIP code
INSERT INTO distributors AS d (did, dname)
VALUES (8, 'Anvil Distribution')
ON CONFLICT (did)
    DO UPDATE
           SET dname = EXCLUDED.dname || ' (formerly ' || d.dname || ')'
           WHERE d.zipcode <> '21201';

-- Name a constraint directly in the statement (uses associated
-- index to arbitrate taking the DO NOTHING action)
INSERT INTO distributors (did, dname)
VALUES (9, 'Antwerp Design')
ON CONFLICT ON CONSTRAINT distributors_pkey DO NOTHING;

INSERT INTO distributors (did, dname)
VALUES (10, 'Conrad International')
ON CONFLICT (did) WHERE is_active DO NOTHING;
