create or replace function pg_statio_user_tables_delta(p_sample_time timestamptz)
returns table 
        ( relid oid
        , schemaname name
        , relname name
        , heap_blks_read bigint
        , heap_blks_read_delta bigint
        , heap_blks_hit bigint
        , heap_blks_hit_delta bigint
        , idx_blks_read bigint
        , idx_blks_read_delta bigint
        , idx_blks_hit bigint
        , idx_blks_hit_delta bigint
        , toast_blks_read bigint
        , toast_blks_read_delta bigint
        , toast_blks_hit bigint
        , toast_blks_hit_delta bigint
        , tidx_blks_read bigint
        , tidx_blks_read_delta bigint
        , tidx_blks_hit bigint
        , tidx_blks_hit_delta bigint
        , sample_time timestamptz )
as $$
select relid
  , schemaname
  , relname
  , heap_blks_read
  , heap_blks_read_delta
  , heap_blks_hit
  , heap_blks_hit_delta
  , idx_blks_read
  , idx_blks_read_delta
  , idx_blks_hit
  , idx_blks_hit_delta
  , toast_blks_read
  , toast_blks_read_delta
  , toast_blks_hit
  , toast_blks_hit_delta
  , tidx_blks_read
  , tidx_blks_read_delta
  , tidx_blks_hit
  , tidx_blks_hit_delta
  , sample_time
from 
    ( select relid
        , schemaname
        , relname
        , heap_blks_read
        , heap_blks_read -lag(heap_blks_read, 1) over w heap_blks_read_delta
        , heap_blks_hit
        , heap_blks_hit -lag(heap_blks_hit, 1) over w heap_blks_hit_delta
        , idx_blks_read
        , idx_blks_read -lag(idx_blks_read, 1) over w idx_blks_read_delta
        , idx_blks_hit
        , idx_blks_hit -lag(idx_blks_hit, 1) over w idx_blks_hit_delta
        , toast_blks_read
        , toast_blks_read -lag(toast_blks_read, 1) over w toast_blks_read_delta
        , toast_blks_hit
        , toast_blks_hit -lag(toast_blks_hit, 1) over w toast_blks_hit_delta
        , tidx_blks_read
        , tidx_blks_read -lag(tidx_blks_read, 1) over w tidx_blks_read_delta
        , tidx_blks_hit
        , tidx_blks_hit -lag(tidx_blks_hit, 1) over w tidx_blks_hit_delta
        , sample_time
        , row_number() over w rn
      from pg_statio_user_tables_hist
      where sample_time > p_sample_time
      window w as (partition by relid order by sample_time) )
          LOX
where rn > 1
$$
language sql;
