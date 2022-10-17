CREATE or REPLACE FUNCTION function_with_long_arguments
        ( IN  num                                 integer
        , IN  function_arg_input_argument_first   varchar
        , IN  function_arg_input_argument_second  varchar
        , IN  function_arg_input_arg_third        varchar
        , OUT function_arg_output_argument_fourth integer )
AS $body$
DECLARE
    expr2      integer;
BEGIN
    raise notice 'Testing ...';
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER;
