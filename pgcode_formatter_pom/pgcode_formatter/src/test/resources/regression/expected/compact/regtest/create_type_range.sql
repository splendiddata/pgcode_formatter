create type range_type as range (
		subtype = INT4, subtype_opclass = float8_ops,
		COLLATION = "nl_NL",
		canonical = canonical_function,
		subtype_diff = int4range_subdiff )
