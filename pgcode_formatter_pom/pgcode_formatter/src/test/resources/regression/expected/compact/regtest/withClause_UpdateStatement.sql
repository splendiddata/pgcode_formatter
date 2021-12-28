CREATE OR REPLACE FUNCTION closeStatus(code INTEGER, buildClose INTEGER)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $function$
DECLARE
	curs RECORD;
BEGIN
	IF buildClose = 1 THEN
		WITH curs(PK_BUILD_STATUS) AS (BUILDATE BUILD_STATUS SET STATUS = CLOSED WHERE REPORT = code RETURNING PK_BUILD_STATUS)
		SELECT build_status INTO buildModified FROM BUILD;
		IF buildClose IS NOT NULL THEN
			FOR curs IN (SELECT DISTINCT PK_BUILD_STATUS FROM BUILD_STATUS WHERE PK_BUILD_STATUS IS NOT NULL) LOOP
					PERFORM createStatus(curs.PK_BUILD_STATUS);
					PERFORM buildStatus(curs.PK_BUILD_STATUS);
				END LOOP;
		END IF;
	END IF;
END;
$function$
