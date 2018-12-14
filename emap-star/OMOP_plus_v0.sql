CREATE TABLE person (
	person_id SERIAL PRIMARY KEY NOT NULL,
	store_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

CREATE TABLE mrn (
	mrn INT PRIMARY KEY NOT NULL,
	store_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	end_datetime TIMESTAMP
	);

CREATE TABLE mpi (
	mrn INT NOT NULL REFERENCES mrn(mrn),
	person_id INT NOT NULL REFERENCES person(person_id),
	store_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	end_datetime TIMESTAMP
	);

CREATE TABLE attribute (
	attribute_id SERIAL PRIMARY KEY NOT NULL,
	short_label VARCHAR(50) NOT NULL,
	long_label VARCHAR(200)
	);

CREATE TABLE mrn_property (
	mrn INT NOT NULL REFERENCES mrn(mrn),
	attribute_id INT NOT NULL REFERENCES attribute(attribute_id),
	value_as_string VARCHAR(50),
	value_as_integer INT,
	value_as_real REAL,
	value_as_datetime TIMESTAMP
	);

CREATE TABLE fact (
	id SERIAL PRIMARY KEY NOT NULL,
	mrn INT NOT NULL REFERENCES mrn(mrn),
	store_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	end_datetime TIMESTAMP
	);

CREATE TABLE fact_property (
	fact_id INT NOT NULL REFERENCES fact(id),
	attribute_id INT NOT NULL REFERENCES attribute(attribute_id),
	value_as_string VARCHAR(50),
	value_as_integer INT,
	value_as_real REAL,
	value_as_datetime TIMESTAMP
	);
