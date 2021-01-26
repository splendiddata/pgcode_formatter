do $do$
begin
	if not exists (
		  select *
		  from pg_catalog.pg_proc
		  where pronamespace = 'myschema'::regnamespace
				and proname = 'details'
				and proargtypes[0] = 'numeric'::regtype
				and array_length(proargtypes, 1) = 1 ) then
		create function myschema.details (in numeric, out OUT_REC refcursor)
		as $$
declare
	stack text;
begin
	get diagnostics stack := PG_CONTEXT;
	raise exception 'function: % is only a prototype', regexp_replace(
			stack
			, 'PL/pgSQL function (.*) line [0-9]+ at GET DIAGNOSTICS'
			, E'\\1' ) using hint = 'please implement';
end
$$
		language plpgsql;
	end if;
end
$do$
language
plpgsql;
