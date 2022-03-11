CREATE FUNCTION sales_func
        ( employee_id int
        , vehicle_id  int )
RETURNS sales
AS $$
DECLARE
    car_model   text;
    car_price   int;
    sales_bonus int;
    bonus       int;
BEGIN
    EXECUTE 'SELECT model, sales_bonus, price FROM cars WHERE car_id = $1' INTO car_model, sales_bonus, car_price USING vehicle_id;


    INSERT INTO sales (staff_id, car_id, staff_bonus, sales_price)
        VALUES (employee_id, vehicle_id, bonus, car_price);
    RETURN QUERY SELECT * FROM sales ORDER BY created_at;
END;
$$
LANGUAGE plpgsql;
