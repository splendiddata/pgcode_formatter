CREATE OR REPLACE FUNCTION function_with_long_arguments(
													   IN  num                  INTEGER,
													   IN  function_arg_input_argument_first
																				VARCHAR,
													   IN  function_arg_input_argument_second
																				VARCHAR,
													   IN  function_arg_input_arg_third
																				VARCHAR,
													   OUT function_arg_output_argument_fourth
																				INTEGER
													   )
AS $body$
DECLARE
	expr2                          INTEGER;
BEGIN
	RAISE NOTICE 'Testing ...';
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER;
