CREATE OR REPLACE FUNCTION public.somefunc_usingLabel()
RETURNS INTEGER
LANGUAGE plpgsql
AS $function$
<< outerblock >>
DECLARE
	quantity                       INTEGER := 30;
BEGIN
	RAISE NOTICE 'Quantity here is %', quantity; -- Prints 30
	quantity := 50;
	--
	-- Create a subblock
	--
	DECLARE
		quantity                       INTEGER := 80;
	BEGIN
		RAISE NOTICE 'Quantity here is %', quantity; -- Prints 80
		RAISE NOTICE 'Outer quantity here is %', outerblock.quantity; -- Prints 50
	END;

	RAISE NOTICE 'Quantity here is %', quantity; -- Prints 50
	RETURN quantity;
END;
$function$
