DO $$
DECLARE
    r varchar;
BEGIN
    select a into r from coord;
    raise notice 'Value: %', r;
END
$$ LANGUAGE plpgsql;