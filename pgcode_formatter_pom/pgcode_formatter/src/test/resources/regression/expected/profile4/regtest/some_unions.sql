select 'a'
union
select 'b';
select 'a'
union all
select ('b')
  , ('c');
values ('a'),('b'),('c') except select 'b';
values ('a'),('b'),('c') intersect select 'b';
