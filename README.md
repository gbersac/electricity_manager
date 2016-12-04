# electricity_manager
Api to manage your electricity production. This project has been made in a limited amount of time and is still considered a work in progress. See the issue page for more information.

## Installation

You'll need postgre sql. Install client and server using those [instructions](https://www.postgresql.org/download/) ([this one for debian](https://help.ubuntu.com/community/PostgreSQL)). Make sure the postgres sql user has [MD5 authentification process](http://stackoverflow.com/a/32927981/2482582) :

```sh
sudo vim /etc/postgresql/9.3/main/pg_hba.conf # debian only
```

Then run the following commands :

```sh
# for production
psql -c 'create database electricity_manager;' -U postgres -W
psql -c '\i conf/init_db.sql' -U postgres -d electricity_manager -W

#for test
psql -c 'create database electricity_manager_test;' -U postgres -W
psql -c '\i conf/init_db.sql' -U postgres -d electricity_manager_test -W
```

Data base options are available in the conf file.

## Usage

To create a new user :

```sh
curl --include \
  --request POST \
  --header "Content-type: application/json" \
  --data '{"pseudo":"John","password":"123456"}' \
  http://localhost:9000/user/create
```

To test login :

```sh
curl --include \
  --request POST \
  --header "Content-type: application/json" \
  --data '{"pseudo":"John","password":"123456"}' \
  http://localhost:9000/user/connect
```

To create a new power station :

```sh
curl --include \
  --request POST \
  --header "Content-type: application/json" \
  --data '{"pseudo":"John","password":"123456","typePW":"Solar Panel","code":"SP1","maxCapacity":100}' \
  http://localhost:9000/power_station/create
```

To add/remove energy in a power station :

```sh
curl --include \
  --request POST \
  --header "Content-type: application/json" \
  --data '{"pseudo":"John","password":"123456","delta":50,"stationId":2}' \
  http://localhost:9000/power_station/use
```

To get the list of all stations and electricity variation for a user :

```sh
curl --include \
  --request POST \
  --header "Content-type: application/json" \
  --data '{"pseudo":"John","password":"123456"}' \
  http://localhost:9000/power_station/power_variations
```
