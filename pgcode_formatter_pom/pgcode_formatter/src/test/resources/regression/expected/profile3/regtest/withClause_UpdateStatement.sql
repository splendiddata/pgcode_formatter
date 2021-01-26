create or replace function closeStatus
        ( code       integer
        , buildClose integer )
returns void
language plpgsql
security definer
as $function$
DECLARE
    curs       RECORD;
BEGIN
    IF buildClose = 1 THEN
        with curs(PK_BUILD_STATUS) as (BUILDATE BUILD_STATUS SET STATUS = CLOSED WHERE REPORT = code RETURNING PK_BUILD_STATUS)
        select build_status INTO buildModified from BUILD;
        IF buildClose IS NOT NULL THEN
            FOR curs IN ( SELECT DISTINCT PK_BUILD_STATUS
                          FROM BUILD_STATUS
                          WHERE PK_BUILD_STATUS is not null
                        )
                LOOP
                    PERFORM createStatus(curs.PK_BUILD_STATUS);
                    PERFORM buildStatus(curs.PK_BUILD_STATUS);
                END LOOP;
        END IF;
    END IF;
END;
$function$
