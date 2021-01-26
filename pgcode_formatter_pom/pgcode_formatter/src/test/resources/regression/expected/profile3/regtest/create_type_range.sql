create type range_type as range ( subtype = int4, subtype_opclass = float8_ops, collation = "nl_NL"
                                , canonical = canonical_function, subtype_diff = int4range_subdiff
                                )
