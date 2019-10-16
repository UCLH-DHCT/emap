# How to deploy a live version

How to deploy an instance of Emap that isn't attached to a person.

## source code location and setup

Find a place to put the source code. If this instance is not attached to a person, a directory in `/gae` is a good place. For example, Jeremy has deployed a live version of Emap in `/gae/emap-live/`, and this will be the example used in these instructions.

Once the directory has been created, it must be set up correctly:
 * `mkdir /gae/emap-live`
 * `chown -R :docker /gae/emap-live` -- set group to docker
 * `chmod -R g+rws /gae/emap-live` -- allow docker group the right access. `s` ensures the group ownership is inherited (but not the permissions)
 * `setfacl -d -m g::rwx /gae/emap-live` -- ensure the permissions for the group are also inherited

It should now look like:

```
$ ls -la /gae/emap-live
total 8
drwxrws---+  2 jstein01 docker 4096 Oct 16 10:59 .
drwxrwx---. 22 root     docker 4096 Oct 16 10:52 ..

```

You can `touch` a file and `mkdir` a dir to test that group and group permissions get inherited.

### git config

Ideally: Authentication should not be tied to any one user, we have the github user `inform-machine-user` for this. The password can be distributed through Lastpass to those who need it. The email address for the github account is a gmail account that also has its details in Lastpass.

For now: use your own username+password to clone/fetch/etc from github. I don't think it will matter if subsequent git fetches are done by a different GAE and/or github user. Jeremy puts the following in his `~/.gitconfig` on the GAE to reduce typing:

```
[credential "https://github.com"]
    username = jeremyestein
```
You could also add `password = foobar` depending on how comfortable you are having your github password stored in plaintext on the GAE.

We use https because outgoing ssh is blocked from the GAE.

Repositories must now be checked out to the correct branches. "Correct" will depend on what you're trying to deploy. Conventionally a live instance would all be deployed off master, but below is what I happened to want to run at the time I set this up.

 * `git clone --branch idempotentise https://github.com/inform-health-informatics/Emap-Core.git`
 * `git clone --branch master https://github.com/inform-health-informatics/Emap-Interchange.git`
 * `git clone --branch tidyup https://github.com/inform-health-informatics/Inform-DB.git`

## config

Supply the required config files in the `Emap-Core` directory (see below in readme for details). Make sure `INFORMDB_SCHEMA` is set to what it needs to be, in this example I'm using `live`. If you're writing to the UDS, use the `uds_write` user (password in lastpass).

## docker config

You need the `-p` option to `docker-compose` to make sure the container and network names don't clash.

`docker-compose -p emaplive up --build -d`

We avoid port clashes by not mapping any local ports. If something needs to connect, it should connect to the relevant docker network. Makes web browser debugging in rabbitmq hard.

# emapstar

## Dependencies

The Dockerfiles have to build the dependencies before building Emap-Core. Therefore your local directory structure has to be like this:

```
Emap [project dir, name doesn't actually matter]
 |
 +-- Emap-Core [git repo]
 |  |
 |  +-- docker-compose.yml [sets build directory to be .. (aka Emap)]
 +-- Emap-Interchange [git repo]
 +-- Inform-DB [git repo]
 +-- Some-Other-Repo [your repo goes here, eg. Caboodle]
```

The `docker-compose.yml` file sets the build directory to be the project root directory, to allow the Emap-Core Dockerfiles
to reference the code containing the dependencies.

## `config-envs` file

These are the required envs for this file with example values.

```bash
IDS_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/ids
IDS_USERNAME=someuser
IDS_PASSWORD=redacted
INFORMDB_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/informdb
INFORMDB_SCHEMA=devfoo
INFORMDB_USERNAME=someuser
INFORMDB_PASSWORD=redacted
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=8672
SPRING_RABBITMQ_USERNAME=someuser
SPRING_RABBITMQ_PASSWORD=redacted
```

## `rabbit-envs` file

This sets the username+password for the rabbitmq server. This helps prevents a user/malware outside the GAE from accessing the queue.

```bash
RABBITMQ_DEFAULT_USER=emap
RABBITMQ_DEFAULT_PASS=seelastpassforpassword
```

# hl7source

## HAPI

 See https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
 for how we handkle multiple versions of HL7.
 
 From HAPI FAQ: https://hapifhir.github.io/hapi-hl7v2/hapi-faq.html 
 Q. Why are some message classes missing? For example, I can find
 the class ADT_A01, but not the class ADT_A04.
 A. HL7 defines that some message triggers reuse the same structure. So, for example,
 the ADT^A04 message has the exact same structure as an ADT^A01 message. Therefore,
 when an ADT^A04 message is parsed, or when you want to create one, you will actually
 use the ADT_A01 message class, but the "triggerEvent" property of MSH-9 will be set to A04.

 The full list is documented in 2.7.properties file:
 A01 also handles A04, A08, A13
 A05 also handles A14, A28, A31
 A06 handles A07
 A09 handles A10, A11
 A21 handles A22, A23, A25, A26, A27, A29, A32, A33
 ADT_A39 handles A40, A41, A42
 ADT_A43 handles A49
 ADT_A44 handles A47
 ADT_A50 handles A51
 ADT_A52 handles A53
 ADT_A54 handles A55
 ADT_A61 handles A62
