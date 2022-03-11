SELECT name
  , author_id
  , count(1)
  , (
        SELECT count(1)
        FROM names AS n2
        WHERE n2.id = n1.id AND t2.author_id = t1.author_id
        )
FROM BOOKS AS n1
GROUP BY name, author_id
