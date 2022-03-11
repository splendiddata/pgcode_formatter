CREATE OR REPLACE FUNCTION deactivate_product(p_product_name TEXT)
RETURNS VOID
AS $$
UPDATE product
SET product_active = FALSE
WHERE product_name = lower(p_product_name); 
UPDATE product_version
SET product_version_active = FALSE
WHERE product_id = (
	SELECT product_id FROM product WHERE product_name = lower(p_product_name) ); 
UPDATE product_component
SET component_active = FALSE
WHERE product_id = (
	SELECT product_id FROM product WHERE product_name = lower(p_product_name) );
$$
LANGUAGE SQL;
