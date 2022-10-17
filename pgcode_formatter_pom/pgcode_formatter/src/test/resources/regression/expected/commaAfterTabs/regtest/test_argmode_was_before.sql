CREATE OR REPLACE FUNCTION argModeBefore(
										IN expr1				 VARCHAR		DEFAULT
																				NULL::VARCHAR,
										IN expr2				 INTEGER
										)
RETURNS INTEGER
AS $$
DECLARE
BEGIN
	RETURN expr2;
END;
$$
LANGUAGE plpgsql
SECURITY DEFINER;
