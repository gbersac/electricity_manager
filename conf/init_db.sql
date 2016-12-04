-- script for creation of the electricity manager database

CREATE TABLE IF NOT EXISTS utilizer (
    id serial PRIMARY KEY,
    pseudo varchar NOT NULL UNIQUE,
    password varchar NOT NULL
);

CREATE TABLE IF NOT EXISTS power_station (
    id serial PRIMARY KEY,
    type varchar NOT NULL,
    code varchar NOT NULL,
    max_capacity integer NOT NULL CHECK (max_capacity > 0),
    proprietary integer REFERENCES utilizer(id)
);

CREATE TABLE IF NOT EXISTS electricity_variation (
    id serial PRIMARY KEY,
    execution_date timestamp NOT NULL,
    delta integer NOT NULL,
    station integer REFERENCES power_station(id)
);
