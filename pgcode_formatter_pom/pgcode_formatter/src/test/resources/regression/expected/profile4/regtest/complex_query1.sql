CREATE or replace VIEW v1 (e, id, s, t, st1, st2) AS WITH tab1 as 
    ( SELECT tb1.col1, tb1.col2
    FROM tab2 tb1
    WHERE date_trunc('day', tb1.col1) =
          date_trunc('day', clock_timestamp())
    ) , sta as 
    ( SELECT row_number() over(order by col1 desc) as e, bl.col1 as s, f1(bl.col1) over (order by col1) as t
    FROM tab1 bl
    WHERE col2 = 'test: abc'
    ) , stg as 
    ( SELECT oid1 as id, 'test: xyz'::text as col2, 'a'::text as st1, 0 as tag
    union all
    SELECT oid2 as id, 'test: second' as col2, 'b' as st1, 1 as tag
    union all
    SELECT oid3 as id
        , 'query nr 03 ... 1' as col2
        , 'query nr 03 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid4 as id
        , 'query nr 04 ... 1' as col2
        , 'query nr 04 ... 2' as st1
        , 1 as tag
    union all
    SELECT oid5 as id
        , 'query nr 05 ... 1' as col2
        , 'query nr 05 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid6 as id
        , 'query nr 06' as col2
        , 'query nr 06' as st1
        , 1 as tag
    union all
    SELECT oid7 as id
        , 'query nr 07 ... 1' as col2
        , 'query nr 07 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid8 as id
        , 'query nr 08 ... 1' as col2
        , 'query nr 08.... 2' as st1
        , 1 as tag
    union all
    SELECT oid9 as id
        , 'query nr 09' as col2
        , 'query nr 09' as st1
        , 0 as tag
    union all
    SELECT oid10 as id
        , 'query nr 10' as col2
        , 'query nr 10' as st1
        , 1 as tag
    union all
    SELECT oid11 as id
        , 'query nr 11' as col2
        , 'query nr 11' as st1
        , 0 as tag
    union all
    SELECT oid12 as id
        , 'query nr 12' as col2
        , 'query nr 12' as st1
        , 1 as tag
    union all
    SELECT oid13 as id
        , 'query nr 13 ... 1' as col2
        , 'query nr 13 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid14 as id
        , 'query nr 14 ... 1' as col2
        , 'query nr 14 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid15 as id
        , 'query nr 15 ... 1' as col2
        , 'query nr 15 ... 2' as st1
        , 1 as tag
    union all
    SELECT oid16 as id
        , 'query nr 16 ... 1' as col2
        , 'query nr 16 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid17 as id
        , 'query nr 17 ... 1' as col2
        , 'query nr 17 ... 2' as st1
        , 1 as tag
    union all
    SELECT oid18 as id
        , 'query nr 18 ... 1' as col2
        , 'query nr 18 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid19 as id
        , 'query nr 19 ... 1' as col2
        , 'query nr 19 ... 2' as st1
        , 1 as tag
    union all
    SELECT oid20 as id
        , 'query nr 20 ... 1' as col2
        , 'query nr 20 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid21 as id
        , 'query nr 21 ... 1' as col2
        , 'query nr 21 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid22 as id
        , 'query nr 22 ... 1' as col2
        , 'query nr 22 ... 2' as st1
        , 0 as tag
    union all
    SELECT oid23 as id
        , 'query nr 23 ... 1' as col2
        , 'query nr 23 ... 2' as st1
        , 1 as tag
    union all
    SELECT oid24 as id
        , 'query nr 24 ... 1' as col2
        , 'query nr 24 ... 2' as st1
        , 1 as tag
    union all
    SELECT oid24 as id
        , 'query nr 24 ... 1' as col2
        , 'query nr 24 ... 2' as st1
        , 1 as tag
    union all
    SELECT oid25 as id
        , 'query nr 25 ... 1' as col2
        , 'query nr 25 ... 2' as st1
        , 1 as tag
    union all
    SELECT oid26 as id
        , 'query nr 26 ... 1' as col2
        , 'query nr 26 ... 2' as st1
        , 1 as tag
    ) SELECT ch.e
        , min(stg.id) id
        , min(tb1.col1) s
        , max(tb1.col1) t
        , stg.st1
        , CASE
            when min(tb1.col1) = max(tb1.col1) then 'case 1'
            when min(tb1.col1) < max(tb1.col1) then 'case 2'
            else 'other cases'
        END st2
    FROM stg
         join sta       ch on ch.e <=100
         left join tab1 tb1 on tb1.col2 = stg.col2 and 
             ( tb1.col1 >= ch.s and tb1.col1 < coalesce
                                                 ( ch.t
                                                 , clock_timestamp() + (1/ 60::numeric /24::numeric||' D')::interval
                                                 ) )
    GROUP BY ch.e, stg.st1;
