# electricity_manager
Exercise to manage your electricity production

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

## Specification questions

> un utilisateur doit pouvoir s’enregistrer, s’identifier

Lorque vous dites qu'un utilisateur doit pouvoir s'identifier, est-ce que cette identification doit pouvoir être persistante ? Est-il possible de contraindre un utilisateur à s'identifier pour chaque requete (dans le but que chaque requête soit stateless, comme dans REST) ?

> Toutes les actions doivent pouvoir être réalisées depuis des points d’entrée HTTP dont les entités sont au format JSON.

Vous dites HTTP, mais si je souhaite réstreindre l'application à l'utilisation de HTTPS pour des raisons de sécurité, ais-je le droit ?

Quand vous dites entités au format JSON, cela signifie que les informations que l'on doit transmettre au serveur doivent se faire au serveur via une requête POST avec un corps contenant du json ?

> Un point d’entrée retournant un graphique (image) montrant l’évolution du volume de déchets au cours du temps.

De quels déchets parlez-vous ? Les spécifications que vous m'avez transmis ne mentionnent pas de déchets à d'autres moments qu'à celui-ci.
