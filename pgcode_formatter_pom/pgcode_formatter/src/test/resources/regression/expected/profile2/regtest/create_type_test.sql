CREATE TYPE address AS (city VARCHAR(90), street VARCHAR(90));
CREATE TYPE status AS ENUM ('active', 'disabled');
CREATE TYPE color AS ENUM ('red', 'orange', 'yellow', 'green', 'blue');
CREATE TYPE new_type;
create type user_defined_type (INPUT = input_function, output = output_function);
