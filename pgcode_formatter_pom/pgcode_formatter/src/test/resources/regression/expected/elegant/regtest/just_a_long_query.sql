SELECT code.code, properties_code.wert
     , properties_code1.wert, properties_code2.wert
FROM code
     INNER JOIN commoncode ON (code.id_code = commoncode.id_codeparent)
     INNER JOIN commoncode      commoncode1 ON (commoncode.id_kindcode = commoncode1.id_codeparent)
     INNER JOIN code            code1 ON (commoncode1.id_kindcode = code1.id_code)
     INNER JOIN properties_code ON (code1.id_code = properties_code.id_code)
     INNER JOIN properties_code properties_code1 ON (code1.id_code = properties_code1.id_code)
     INNER JOIN properties_code properties_code2 ON (code1.id_code = properties_code2.id_code)
     INNER JOIN properties ON (properties_code.id_propriete = properties.id_propriete)
     INNER JOIN properties      properties1 ON (properties_code1.id_propriete = properties1.id_propriete)
     INNER JOIN properties      properties2 ON (properties_code2.id_propriete = properties2.id_propriete)
     INNER JOIN variables ON (properties.id_variable = variables.id_variable)
     INNER JOIN variables       variables1 ON (properties1.id_variable = variables1.id_variable)
     INNER JOIN variables       variables2 ON (properties2.id_variable = variables2.id_variable)
     INNER JOIN mvt_temps ON (code.id_code = mvt_temps.id_code)
     INNER JOIN transactions ON (mvt_temps.id_transaction = transactions.id_transaction)
     INNER JOIN products ON (code.id_product = products.id_product)
WHERE variables.name = 'acktuel_adresse'
      AND variables1.name = 'variable_name'
      AND variables2.name = 'variable_name'
      AND transactions.code = 'XXXXXXXXXXXXX'
      AND mvt_temps.statutok = TRUE
      AND products.produktcode = '123456789'
