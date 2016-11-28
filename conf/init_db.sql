-- script for creation of the electricity manager database

CREATE TABLE IF NOT EXISTS utilizer (
    id integer PRIMARY KEY,
    pseudo varchar NOT NULL,
    password varchar NOT NULL
);

CREATE TABLE IF NOT EXISTS power_station (
    id integer PRIMARY KEY,
    type varchar NOT NULL,
    code varchar NOT NULL,
    max_capacity integer NOT NULL CHECK (max_capacity > 0),
    proprietary integer REFERENCES utilizer(id)
);

CREATE TABLE IF NOT EXISTS electricity_change (
    id integer PRIMARY KEY,
    execution timestamp NOT NULL,
    delta integer NOT NULL,
    station integer REFERENCES power_station(id)
);
