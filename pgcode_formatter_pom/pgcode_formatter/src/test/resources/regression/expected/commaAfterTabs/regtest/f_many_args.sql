CREATE OR REPLACE FUNCTION some_schema.f_many_args(
												  common_node			   NUMERIC,
												  argum_dc_zap			   VARCHAR,
												  argum_dc_bin			   VARCHAR,
												  argum_dv_zap			   VARCHAR,
												  argum_dv_bin			   VARCHAR,
												  argum_da_zap			   VARCHAR,
												  argum_da_bin			   VARCHAR,
												  argum_de_zap			   VARCHAR,
												  argum_de_bin			   VARCHAR,
												  argum_as_orig 		   VARCHAR,
												  argum_as_dest 		   VARCHAR,
												  argum_tc_orig 		   VARCHAR,
												  argum_tc_dest 		   VARCHAR
												  )
RETURNS NUMERIC
AS $body$
-- Signature:
-- CREATE or REPLACE FUNCTION f_many_args(
--	   common_node NUMERIC,
--	   argum_dc_zap VARCHAR,
--	   argum_dc_bin VARCHAR,
--	   argum_dv_zap VARCHAR,
--	   argum_dv_bin VARCHAR,
--	   argum_da_zap VARCHAR,
--	   argum_da_bin VARCHAR,
--	   argum_de_zap VARCHAR,
--	   argum_de_bin VARCHAR,
--	   argum_as_orig VARCHAR,
--	   argum_as_dest VARCHAR,
--	   argum_tc_orig VARCHAR,
--	   argum_tc_dest VARCHAR
-- )
-- RETURNS NUMERIC
--
DECLARE
	ln_count					   NUMERIC(20, 4) := 0;
BEGIN
	IF (coalesce(argum_dc_zap::TEXT, '') = '') AND (coalesce(argum_dc_bin::TEXT, '') = '') AND
			(coalesce(argum_dv_zap::TEXT, '') = '') AND (coalesce(argum_dv_bin::TEXT, '') = '') AND
			(
		coalesce(argum_da_zap::TEXT, '') = ''
		) AND (coalesce(argum_da_bin::TEXT, '') = '') AND (coalesce(argum_de_zap::TEXT, '') = '') AND
			(
		coalesce(argum_de_bin::TEXT, '') = ''
		) AND (coalesce(argum_as_dest::TEXT, '') = '') AND (coalesce(argum_tc_dest::TEXT, '') = '')
	THEN
		ln_count := some_schema.f_multiarg_function(
			common_node, argum_dc_zap, argum_dc_bin,
			argum_dv_zap, argum_dv_bin, argum_da_zap,
			argum_da_bin, argum_de_zap, argum_de_bin
			);
	ELSE
		ln_count := some_schema.F_ALSO_MANY(
			common_node, argum_dc_zap, argum_dc_bin,
			argum_dv_zap, argum_dv_bin, argum_da_zap,
			argum_da_bin, argum_de_zap, argum_de_bin,
			argum_as_orig, argum_as_dest, argum_tc_orig,
			argum_tc_dest
			) - some_schema.F_STILL_MUCH(
			argum_da_bin, argum_de_zap, argum_de_bin,
			argum_as_orig, argum_as_dest, argum_tc_orig,
			argum_tc_dest
			);
	END IF;
	RETURN ln_count;
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER;
