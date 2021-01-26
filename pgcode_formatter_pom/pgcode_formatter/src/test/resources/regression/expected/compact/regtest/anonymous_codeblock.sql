DO $$
DECLARE
	r varchar;
BEGIN
	select a into r from coord;
	raise notice 'Value: %', r;
END
$$;
do $$
begin
	raise notice '%', (select 'a');
end -- with some end-of-line comment and /* before the dollardollar quote
$$
do $do$
declare
	some_variable int := 0;
	a_constant constant text := 'a default value';
begin
	select a_column from some_schema.some_table into some_variable where another_column > a_constant;
	another statement containing the word exception, which has no meaning here;
exception
	when not found then raise notice 'not found';
	insert into some_schema.some_table(
			a_column, another_column, reason
			, create_timestamp )
		values (-1, 'this value is wrong', $text$the value was not found$text$, current_timestamp);
	return -1;
	when other then raise notice $msg$something went very wriong$msg$;
	return -2;
end;
$do$;
