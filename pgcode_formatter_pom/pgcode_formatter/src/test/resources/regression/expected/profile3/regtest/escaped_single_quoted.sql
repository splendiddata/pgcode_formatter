-- Test escaped single quote
SELECT 'hello'
     , 2 + 2
     , E'o\'grady'
     , 0
     , ''
     , count(*)
     , E'that\'s the position you\'re in now no matter where you select it';
