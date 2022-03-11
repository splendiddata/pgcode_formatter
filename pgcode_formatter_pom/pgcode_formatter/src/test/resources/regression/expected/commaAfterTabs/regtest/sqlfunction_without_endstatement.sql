CREATE OR REPLACE FUNCTION pg_statio_user_tables_delta(p_sample_time			TIMESTAMPTZ)
RETURNS TABLE (
			  relid OID,
			  schemaname NAME,
			  relname NAME,
			  heap_blks_read BIGINT,
			  heap_blks_read_delta BIGINT,
			  heap_blks_hit BIGINT,
			  heap_blks_hit_delta BIGINT,
			  idx_blks_read BIGINT,
			  idx_blks_read_delta BIGINT,
			  idx_blks_hit BIGINT,
			  idx_blks_hit_delta BIGINT,
			  toast_blks_read BIGINT,
			  toast_blks_read_delta BIGINT,
			  toast_blks_hit BIGINT,
			  toast_blks_hit_delta BIGINT,
			  tidx_blks_read BIGINT,
			  tidx_blks_read_delta BIGINT,
			  tidx_blks_hit BIGINT,
			  tidx_blks_hit_delta BIGINT,
			  sample_time TIMESTAMPTZ
			  )
AS $$
SELECT
	relid, schemaname, relname, heap_blks_read,
	heap_blks_read_delta, heap_blks_hit,
	heap_blks_hit_delta, idx_blks_read,
	idx_blks_read_delta, idx_blks_hit,
	idx_blks_hit_delta, toast_blks_read,
	toast_blks_read_delta, toast_blks_hit,
	toast_blks_hit_delta, tidx_blks_read,
	tidx_blks_read_delta, tidx_blks_hit,
	tidx_blks_hit_delta, sample_time
	FROM
		(
		SELECT
			relid, schemaname, relname, heap_blks_read,
			heap_blks_read -lag(heap_blks_read, 1) OVER w heap_blks_read_delta,
			heap_blks_hit,
			heap_blks_hit -lag(heap_blks_hit, 1) OVER w heap_blks_hit_delta,
			idx_blks_read,
			idx_blks_read -lag(idx_blks_read, 1) OVER w idx_blks_read_delta,
			idx_blks_hit,
			idx_blks_hit -lag(idx_blks_hit, 1) OVER w idx_blks_hit_delta,
			toast_blks_read,
			toast_blks_read -lag(toast_blks_read, 1) OVER w toast_blks_read_delta,
			toast_blks_hit,
			toast_blks_hit -lag(toast_blks_hit, 1) OVER w toast_blks_hit_delta,
			tidx_blks_read,
			tidx_blks_read -lag(tidx_blks_read, 1) OVER w tidx_blks_read_delta,
			tidx_blks_hit,
			tidx_blks_hit -lag(tidx_blks_hit, 1) OVER w tidx_blks_hit_delta,
			sample_time, row_number() OVER w rn
			FROM
				pg_statio_user_tables_hist
			WHERE
				sample_time > p_sample_time
			WINDOW
				w AS (PARTITION BY relid ORDER BY sample_time)
		)						   LOX
	WHERE
		rn > 1
$$
LANGUAGE SQL;
