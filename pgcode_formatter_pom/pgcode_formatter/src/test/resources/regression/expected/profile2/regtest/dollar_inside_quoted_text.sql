CREATE FUNCTION sales_func(
	employee_id INT,
	vehicle_id  INT
	)
RETURNS sales
AS $$
DECLARE
	car_model   TEXT;
	car_price   INT;
	sales_bonus INT;
	bonus       INT;
BEGIN
	EXECUTE 'SELECT model, sales_bonus, price FROM cars WHERE car_id = $1' INTO car_model, sales_bonus, car_price USING vehicle_id;
	  INSERT INTO sales (staff_id, car_id, staff_bonus, sales_price)
		VALUES
			(employee_id, vehicle_id, bonus, car_price);
	RETURN QUERY
		SELECT
			*
			FROM
				sales
			ORDER BY
				created_at;
END;
$$
LANGUAGE plpgsql;
