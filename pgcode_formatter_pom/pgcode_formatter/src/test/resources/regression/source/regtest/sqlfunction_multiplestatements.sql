create or replace function
  deactivate_product(p_product_name text)
returns
  void
as
$$

  update product set product_active = false
  where product_name = lower(p_product_name);

  update product_version set product_version_active = false
  where product_id = (select product_id from product where product_name = lower(p_product_name));

  update product_component set component_active = false
  where product_id = (select product_id from product where product_name = lower(p_product_name));

$$ language sql;