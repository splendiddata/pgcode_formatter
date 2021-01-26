-- Test else followed by an opening parenthesis
create or replace function else_and_opening_parenthesis()
 returns void
 language plpgsql
 security definer
as $function$ 
DECLARE
    rec_s RECORD;
BEGIN


				 UPDATE DEPARTMENT SET dep_name =
				 CASE WHEN city_id = 0  THEN dep1
                 								WHEN city_id = 1           THEN dep2
                 								ELSE (dep3 || '_special')
                 						   END
                 				 WHERE x = y
                 				 OR    u = v;


END;
$function$