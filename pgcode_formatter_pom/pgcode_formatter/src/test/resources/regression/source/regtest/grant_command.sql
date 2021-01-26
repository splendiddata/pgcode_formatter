create or replace function test_grant_command()
 returns void
 language plpgsql
 security definer
as $function$
BEGIN
    GRANT INSERT, UPDATE, SELECT, DELETE         ON tmp_view_deps        TO public;
    GRANT USAGE, SELECT ON view_deps_temp_deps_id_seq TO public;
END;
$function$
