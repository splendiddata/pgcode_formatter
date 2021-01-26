-- liberPLSQL translation Version #####
SET search_path = country, david, global_util, public;
CREATE OR REPLACE FUNCTION country.f_aastring()
RETURNS INTEGER
AS $body$
--
-- Signature:
-- CREATE or REPLACE FUNCTION country.f_aastring()
--	   RETURNS integer
DECLARE
	--
	-- dse March 6, 2013
	--	Generate and initialize a local Associative Array with key = string data type.
	--
	-- Oracle test
	--	   call f_AAString() into :myVar;
	--
	-- type country_records 											 -- original declaration
	-- is table of varchar2(40) INDEX BY varchar2(40);					 -- original declaration
	-- the associative array we will be working with
	country2012 				   country.rmh_country_records;
	-- TYPE stringTable is TABLE of varchar2(40);  -- nested table		 -- original declaration
	songList					   country.rmh_stringtable;
	v_val						   VARCHAR(40);
	v_valPrior					   VARCHAR(40);
	v_valNext					   VARCHAR(40);
	v_key						   VARCHAR(40);
	v_keyPrior					   VARCHAR(40);
	v_keyNext					   VARCHAR(40);
	l_idx						   VARCHAR(40);
	v_ret						   INTEGER := 1;
	v_cnt						   INTEGER := 0;
	v_n 						   INTEGER := 0;
	songlist_1					   country.rmh_stringtable; -- nested table emulation intermediate result
	country2012_2				   country.rmh_country_records; -- nested table emulation intermediate result
	country2012_3				   country.rmh_country_records; -- nested table emulation intermediate result
	country2012_4				   country.rmh_country_records; -- nested table emulation intermediate result
	songlist_5					   country.rmh_stringtable; -- nested table emulation intermediate result
	country2012_6				   country.rmh_country_records; -- nested table emulation intermediate result
	country2012_7				   country.rmh_country_records; -- nested table emulation intermediate result
	country2012_8				   country.rmh_country_records; -- nested table emulation intermediate result
	country2012_9				   country.rmh_country_records; -- nested table emulation intermediate result
	country2012_10				   country.rmh_country_records; -- nested table emulation intermediate result
BEGIN
	PERFORM country.gen_tbl_country2012_l1(0); -- create collection emulation table for country2012 (local instantiation)
	PERFORM country.gen_tbl_songlist_l2(1); -- create collection emulation table for songlist (local instantiation)
	RAISE NOTICE 'f_AAString.....|begin';
	-- country2012('Taylor Swift').val := 'Ours';
	country2012.val := 'Ours';
	INSERT INTO
			tbl_country2012_l1 (arrIx, val)
		VALUES
			('Taylor Swift', country2012.val)
		ON CONFLICT
			(arrIx)
			DO UPDATE SET val = country2012.val;
	-- country2012('Tim McGraw').val := 'Better Than I Used To Be';
	country2012.val := 'Better Than I Used To Be';
	INSERT INTO
			tbl_country2012_l1 (arrIx, val)
		VALUES
			('Tim McGraw', country2012.val)
		ON CONFLICT
			(arrIx)
			DO UPDATE SET val = country2012.val;
	-- country2012('Miranda Lambert').val := 'Over You';
	country2012.val := 'Over You';
	INSERT INTO
			tbl_country2012_l1 (arrIx, val)
		VALUES
			('Miranda Lambert', country2012.val)
		ON CONFLICT
			(arrIx)
			DO UPDATE SET val = country2012.val;
	-- country2012('Eric Church').val := 'Springsteen';
	country2012.val := 'Springsteen';
	INSERT INTO
			tbl_country2012_l1 (arrIx, val)
		VALUES
			('Eric Church', country2012.val)
		ON CONFLICT
			(arrIx)
			DO UPDATE SET val = country2012.val;
	--
	--	key ORDER test
	--
	l_idx := (
		SELECT
			min(arrIx)
			FROM
			tbl_country2012_l1
		);
	v_n := 1;
	WHILE (coalesce(l_idx, '') != '')
		LOOP
		SELECT
			tt.val
			INTO STRICT
				country2012
			FROM
			tbl_country2012_l1 tt
			WHERE
				arrIx = l_idx; -- RAISE NOTICE 'f_AAString.....| k ...
		RAISE NOTICE 'f_AAString.....| key = % val = %', l_idx, country2012.val;
		v_val := country2012.val;
		SELECT
			tt.val
			INTO STRICT
				songlist_1
			FROM
			tbl_songlist_l2 tt
			WHERE
				arrIx = v_n; -- if (v_val <> songList(v_n)) then
		IF (v_val <> songlist_1.val)
		THEN
			RAISE NOTICE 'f_AAString.....|***(Severe) error should be %', songlist_1.val;
			RAISE NOTICE 'f_AAString.....|                    is %', v_val;
			v_ret := 0;
		END IF;
		l_idx := (
			SELECT
				arrIx
				FROM
				tbl_country2012_l1
				WHERE
					arrIx > l_idx
				ORDER BY
					arrIx
				LIMIT 1
			);
		v_n := v_n + 1;
	END LOOP;
	--
	--	val UPDATE test
	--
	-- country2012('Taylor Swift').val := 'Begin Again';
	country2012.val := 'Begin Again';
	INSERT INTO
			tbl_country2012_l1 (arrIx, val)
		VALUES
			('Taylor Swift', country2012.val)
		ON CONFLICT
			(arrIx)
			DO UPDATE SET val = country2012.val;
	SELECT
		tt.val
		INTO STRICT
			country2012_2
		FROM
		tbl_country2012_l1 tt
		WHERE
			arrIx = 'Taylor Swift'; -- if (country2012('Taylor Swift'). ...
	IF (country2012_2.val <> 'Begin Again')
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) error should be Begin Again';
		RAISE NOTICE 'f_AAString.....|                    is %', country2012_2.val;
		v_ret := 0;
	END IF;
	--
	v_key := 'Miranda Lambert';
	SELECT
		tt.val
		INTO STRICT
			country2012_3
		FROM
		tbl_country2012_l1 tt
		WHERE
			arrIx = v_key; -- v_val := country2012(v_key).val; ...
	v_val := country2012_3.val; -- Miranda search
	SELECT
		tt.val
		INTO STRICT
			country2012_4
		FROM
		tbl_country2012_l1 tt
		WHERE
			arrIx = 'Miranda Lambert'; -- if (country2012('Miranda Lambert ...
	IF (country2012_4.val <> v_val)
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) error should be %', country2012_4.val;
		RAISE NOTICE 'f_AAString.....|                    is %', v_val;
		v_ret := 0;
	END IF;
	--
	--	intentionally missing key test
	--
	v_key := 'Jimi';
	BEGIN
	
		SELECT
			tt.val
			INTO STRICT
				country2012_3
			FROM
			tbl_country2012_l1 tt
			WHERE
				arrIx = v_key; -- v_val := country2012(v_key).val; ...
		v_val := country2012_3.val; -- Jimi search
		v_ret := 0; -- should not get here ... should take no data found exception
		RAISE NOTICE 'f_AAString.....|***(Severe) KEY Jimi MUST NOT be found.';
	EXCEPTION
		WHEN no_data_found THEN RAISE NOTICE 'f_AAString.....| good, Jimi was not found.';
	END;
	--
	--	EXISTs test
	--
	IF (
		SELECT
			(
				CASE count(*)
					WHEN 0 then FALSE
					ELSE TRUE
				END
				)
			FROM
			tbl_country2012_l1
			WHERE
				arrIx = v_key
		)
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) KEY Jimi MUST NOT EXIST.';
		v_ret := 0;
	ELSE
		RAISE NOTICE 'f_AAString.....| good because key Jimi does NOT EXIST';
	END IF;
	--
	--	intentionally missing Nested Table array entry test
	--
	-- nt deviation
	DELETE FROM tbl_songlist_l2 WHERE arrIx = 2; -- songlist.delete(2)
	BEGIN
	
		SELECT
			tt.val
			INTO STRICT
				songlist_5
			FROM
			tbl_songlist_l2 tt
			WHERE
				arrIx = 2; -- v_val := songList(2); -- no data ...
		v_val := songlist_5.val; -- no data found on NT test.
		v_ret := 0; -- should not get here ... should take no data found exception
		RAISE NOTICE 'f_AAString.....|***(Severe) KEY 2 MUST NOT be found.';
	EXCEPTION
		WHEN no_data_found THEN RAISE NOTICE 'f_AAString.....| good, entry 2 was deleted.';
	END;
	--
	--	COUNT test
	--
	v_cnt := (
		SELECT
			count(*)
			FROM
			tbl_country2012_l1
		);
	IF (v_cnt <> 4)
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) error COUNT should be 4 is %', v_cnt;
		v_ret := 0;
	END IF;
	--
	--	DELETE test
	--
	DELETE FROM tbl_country2012_l1 WHERE arrIx = 'Tim McGraw'; -- country2012.delete('Tim McGraw')
	v_cnt := (
		SELECT
			count(*)
			FROM
			tbl_country2012_l1
		);
	IF (v_cnt <> 3)
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) error COUNT after DELETE should be 3 is %', v_cnt;
		v_ret := 0;
	END IF;
	--
	--	LAST test
	--
	v_key := (
		SELECT
			max(arrIx)
			FROM
			tbl_country2012_l1
		);
	IF (v_key <> 'Taylor Swift')
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) error should be Taylor Swift';
		RAISE NOTICE 'f_AAString.....|                    is %', v_key;
		v_ret := 0;
	END IF;
	--
	--	REUSE test
	--
	BEGIN
	
		SELECT
			tt.val
			INTO STRICT
				country2012_4
			FROM
			tbl_country2012_l1 tt
			WHERE
				arrIx = 'Miranda Lambert'; -- v_val := country2012('Miranda La ...
		v_val := country2012_4.val; -- will be remembered from before when Miranda Lambert was a key
		IF (v_val <> 'Over You')
		THEN
			RAISE NOTICE 'f_AAString.....|***(Severe) error should be Over You';
			RAISE NOTICE 'f_AAString.....|                    is %', v_val;
			v_ret := 0;
		END IF;
	END;
	--
	--	FUNCTION vs the key
	--
	BEGIN
	-- country2012(upper('Toby Keith')).val := 'American Soldier'; -- oldie
		country2012.val := 'American Soldier'; -- oldie
		INSERT INTO
				tbl_country2012_l1 (arrIx, val)
			VALUES
				(upper('Toby Keith'), country2012.val)
			ON CONFLICT
				(arrIx)
				DO UPDATE SET val = country2012.val;
		SELECT
			tt.val
			INTO STRICT
				country2012_6
			FROM
			tbl_country2012_l1 tt
			WHERE
				arrIx = upper('Toby Keith'); -- v_val := country2012(upper('Toby ...
		v_val := country2012_6.val; -- upper function test
		IF (v_val <> 'American Soldier')
		THEN
			RAISE NOTICE 'f_AAString.....|***(Severe) error should be American Soldier';
			RAISE NOTICE 'f_AAString.....|                    is %', v_val;
			v_ret := 0;
		ELSE
			RAISE NOTICE 'f_AAString.....| + upper(Toby Keith) %', v_val;
		END IF;
	END;
	--
	BEGIN
	-- country2012('Toby Keith').val := upper('American Soldier'); -- oldie
		country2012.val := upper ('American Soldier'); -- oldie
		INSERT INTO
				tbl_country2012_l1 (arrIx, val)
			VALUES
				('Toby Keith', country2012.val)
			ON CONFLICT
				(arrIx)
				DO UPDATE SET val = country2012.val;
		SELECT
			tt.val
			INTO STRICT
				country2012_7
			FROM
			tbl_country2012_l1 tt
			WHERE
				arrIx = 'Toby Keith'; -- v_val := country2012('Toby Keith ...
		v_val := country2012_7.val;
		IF (v_val <> 'AMERICAN SOLDIER')
		THEN
			RAISE NOTICE 'f_AAString.....|***(Severe) error should be AMERICAN SOLDIER';
			RAISE NOTICE 'f_AAString.....|                    is %', v_val;
			v_ret := 0;
		ELSE
			RAISE NOTICE 'f_AAString.....| + Toby Keith again %', v_val;
		END IF;
	END;
	--
	--	check current contents of associative array
	--
	l_idx := (
		SELECT
			min(arrIx)
			FROM
			tbl_country2012_l1
		);
	v_n := 1;
	WHILE (coalesce(l_idx, '') != '')
		LOOP
		SELECT
			tt.val
			INTO STRICT
				country2012_8
			FROM
			tbl_country2012_l1 tt
			WHERE
				arrIx = l_idx; -- RAISE NOTICE 'f_AAString.....| k ...
		RAISE NOTICE 'f_AAString.....| key = % val = %', l_idx, country2012_8.val;
		l_idx := (
			SELECT
				arrIx
				FROM
				tbl_country2012_l1
				WHERE
					arrIx > l_idx
				ORDER BY
					arrIx
				LIMIT 1
			);
		v_n := v_n + 1;
	END LOOP;
	--
	-- position then get prior
	--
	v_key := 'Toby1';
	v_val := 'Courtesy of Red, White and Blue';
	-- country2012(v_key).val := v_val;
	country2012.val := v_val;
	INSERT INTO
			tbl_country2012_l1 (arrIx, val)
		VALUES
			(v_key, country2012.val)
		ON CONFLICT
			(arrIx)
			DO UPDATE SET val = country2012.val;
	v_key := 'Toby2';
	v_val := 'How do you like me now';
	-- country2012(v_key).val := v_val;
	country2012.val := v_val;
	INSERT INTO
			tbl_country2012_l1 (arrIx, val)
		VALUES
			(v_key, country2012.val)
		ON CONFLICT
			(arrIx)
			DO UPDATE SET val = country2012.val;
	v_key := 'Toby3';
	v_val := 'American Soldier';
	-- country2012(v_key).val := v_val;
	country2012.val := v_val;
	INSERT INTO
			tbl_country2012_l1 (arrIx, val)
		VALUES
			(v_key, country2012.val)
		ON CONFLICT
			(arrIx)
			DO UPDATE SET val = country2012.val;
	v_key := 'Toby2';
	v_keyPrior := (
		SELECT
			last_value(arrIx) over (ORDER BY arrIx DESC)
			FROM
			tbl_country2012_l1
			WHERE
				arrIx < v_key
			LIMIT 1
		);
	RAISE NOTICE 'f_AAString.....| key prior to Toby2 is %', v_keyPrior;
	SELECT
		tt.val
		INTO STRICT
			country2012_9
		FROM
		tbl_country2012_l1 tt
		WHERE
			arrIx = (
			SELECT
				last_value(arrIx) over (ORDER BY arrIx DESC)
				FROM
				tbl_country2012_l1
				WHERE
					arrIx < v_key
				LIMIT 1
			); -- v_valPrior := country2012(countr ...
	v_valPrior := country2012_9.val;
	RAISE NOTICE 'f_AAString.....| val prior to Toby2 is %', v_valPrior;
	IF (v_valPrior <> 'Courtesy of Red, White and Blue')
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) error should be Courtesy of Red, White and Blue';
		RAISE NOTICE 'f_AAString.....|                    is %', v_valPrior;
		v_ret := 0;
	END IF;
	--
	-- position then get next
	--
	v_key := 'Toby2';
	v_keyNext := (
		SELECT
			arrIx
			FROM
			tbl_country2012_l1
			WHERE
				arrIx > v_key
			ORDER BY
				arrIx
			LIMIT 1
		);
	RAISE NOTICE 'f_AAString.....| key next from Toby2 is %', v_keyNext;
	SELECT
		tt.val
		INTO STRICT
			country2012_10
		FROM
		tbl_country2012_l1 tt
		WHERE
			arrIx = (
			SELECT
				arrIx
				FROM
				tbl_country2012_l1
				WHERE
					arrIx > v_key
				ORDER BY
					arrIx
				LIMIT 1
			); -- v_valNext := country2012(country ...
	v_valNext := country2012_10.val;
	RAISE NOTICE 'f_AAString.....| val next from Toby2 is %', v_valNext;
	IF (v_valNext <> 'American Soldier')
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) error should be American Soldier';
		RAISE NOTICE 'f_AAString.....|                    is %', v_valNext;
		v_ret := 0;
	END IF;
	--
	-- delete all
	--
	PERFORM country.gen_tbl_country2012_l1(0); -- country2012.DELETE
	v_cnt := (
		SELECT
			count(*)
			FROM
			tbl_country2012_l1
		);
	IF (v_cnt <> 0)
	THEN
		RAISE NOTICE 'f_AAString.....|***(Severe) error COUNT should be 0 is %', v_cnt;
		v_ret := 0;
	END IF;
	--
	-- return
	--
	RAISE NOTICE 'f_AAString.....| exit with return val = %', v_ret;
	RETURN v_ret;
EXCEPTION
	WHEN OTHERS THEN RAISE NOTICE 'ERROR f_AAString sqlerrm: %', sqlerrm;
	RETURN 0; -- error
END; -- f_AAString
$body$
LANGUAGE plpgsql
SECURITY DEFINER;
