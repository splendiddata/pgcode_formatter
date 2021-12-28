-- From 9.4 JSON page - examples
SELECT '[{"a":"foo"},{"b":"bar"},{"c":"baz"}]'::JSON -> 2;
SELECT '{"a": {"b":"foo"}}'::JSON -> 'a';
SELECT '[1,2,3]'::JSON ->> 2;
SELECT '{"a":1,"b":2}'::JSON ->> 'b';
SELECT '{"a": {"b":{"c": "foo"}}}'::JSON #> '{a,b}';
SELECT '{"a":[1,2,3],"b":[4,5,6]}'::JSON #>> '{a,2}';
-- From 9.4 JSON page - jsonb examples
SELECT '{"a":1, "b":2}'::JSONB @> '{"b":2}'::JSONB;
SELECT '{"b":2}'::JSONB <@ '{"a":1, "b":2}'::JSONB;
SELECT '{"a":1, "b":2}'::JSONB ? 'b';
SELECT '{"a":1, "b":2, "c":3}'::JSONB ?| ARRAY [ 'b',
	'c' ];
SELECT '{"a":1, "b":2, "c":3}'::JSONB ?| ARRAY [ 'b',
	'c' ];
SELECT '["a", "b"]'::JSONB ?& ARRAY [ 'a', 'b' ];
SELECT'{"a": {"b":{"c": "foo"}}}'::JSON#>'{a,b}',
	'{"a":[1,2,3],"b":[4,5,6]}'::JSON#>>'{a,2}';
