# electricity_manager
Api to manage your electricity production. This project has been made in a limited amount of time and is still considered a work in progress. Many mandatory features are still missing like encryption of user password in data base. See the issue page for more information.

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
