CREATE or REPLACE FUNCTION function_with_long_arguments(
    num IN integer,
    function_arg_input_argument_first IN varchar,
    function_arg_input_argument_second IN varchar,
    function_arg_input_arg_third IN varchar,
    function_arg_output_argument_fourth OUT integer
)

AS $body$
DECLARE
    expr2                 integer;
BEGIN
     raise notice 'Testing ...';
END;
$body$ LANGUAGE plpgsql SECURITY DEFINER;
