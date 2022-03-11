\set ON_ERROR_STOP ON
\set VERBOSITY default
select * from emp;
\echo :a
SELECT :a;
\unset a
create or replace function add_one(integer)
returns integer
as '/usr/lib/postgresql/9.1/lib/add_one' ,
language c;
