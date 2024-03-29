SELECT DISTINCT (current_database())::information_schema.sql_identifier AS view_catalog,
	(nv.nspname)::information_schema.sql_identifier AS view_schema,
	(v.relname)::information_schema.sql_identifier AS view_name,
	(current_database())::information_schema.sql_identifier AS table_catalog,
	(nt.nspname)::information_schema.sql_identifier AS table_schema,
	(t.relname)::information_schema.sql_identifier AS table_name
FROM pg_namespace nv, pg_class v, pg_depend dv, pg_depend dt, pg_class t, pg_namespace nt
WHERE (
	(
		(
			(
				(
					(
						(
							(
								(
									(
										(
											(((nv.oid = v.relnamespace) AND (v.relkind = 'v'::"char")) AND (v.oid = dv.refobjid)) AND (
												dv.refclassid = ('pg_class'::REGCLASS)::OID ) ) AND (
											dv.classid = ('pg_rewrite'::REGCLASS)::OID ) ) AND (dv.deptype = 'i'::"char") ) AND (
									dv.objid = dt.objid ) ) AND (dv.refobjid <> dt.refobjid) ) AND (
							dt.classid = ('pg_rewrite'::REGCLASS)::OID ) ) AND (dt.refclassid = ('pg_class'::REGCLASS)::OID) ) AND (
					dt.refobjid = t.oid ) ) AND (t.relnamespace = nt.oid) ) AND (t.relkind = ANY (ARRAY['r'::"char", 'v'::"char"])) ) AND
		pg_has_role(t.relowner, 'USAGE'::TEXT) )
ORDER BY (current_database())::information_schema.sql_identifier,
	(nv.nspname)::information_schema.sql_identifier,
	(v.relname)::information_schema.sql_identifier,
	(current_database())::information_schema.sql_identifier,
	(nt.nspname)::information_schema.sql_identifier,
	(t.relname)::information_schema.sql_identifier;
