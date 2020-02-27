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

Repositories must be checked out to the correct branches. "Correct" will depend on what you're trying to do. Conventionally a live instance would all be deployed off master, but during the development phase `develop` is more likely to be the correct branch. Occasionally you may need to deploy off a feature branch - if in doubt ask the author of that code.

 * `git clone --branch develop https://github.com/inform-health-informatics/Emap-Core.git`
 * `git clone --branch master https://github.com/inform-health-informatics/Emap-Interchange.git`
 * `git clone --branch develop https://github.com/inform-health-informatics/Inform-DB.git`

## config

Supply the required config files in the `Emap-Core` directory (see below in readme for details). Make sure `INFORMDB_SCHEMA` is set to what it needs to be, in this example I'm using `live`. If you're writing to the UDS, use the `uds_write` user (password in lastpass).

## docker config

You need the `-p` option to `docker-compose` to make sure the container and network names don't clash.

`docker-compose -p emaplive up --build -d`

## Adding in a fake UDS

In dev/test environments, you may want to make your own UDS in a docker container. It could also double as an IDS as there's nothing to say these can't be on the same postgres instance.

This container is defined in a separate docker-compose file, so you need to specify both files as below:

`docker-compose -f docker-compose.yml -f docker-compose.fakeuds.yml -p emaplive up --build -d`

## Emap-in-a-box

To make running with multiple docker-compose files easier, there's a script called `box.sh` which can run the above command for you. It's a wrapper around `docker-compose` that works out what the "stem" of the command should be (the `-f` and `-p` options, and you just have to pass in the compose commands like `ps`, `up`, `down`, etc).

It needs configuring with at least one environment variable `BOX_PROJECT_NAME` which must live in the file `box-config-envs` in the directory above `Emap-Core` and friends.

Because this is just a file containing environment variables, you can actually put all the local ports here as well, instead of spreading them over multiple `.env` files inside the repository dirs themselves:

Example `box-config-envs`:
```bash
BOX_PROJECT_NAME=jes1
RABBITMQ_PORT=5972
RABBITMQ_ADMIN_PORT=15972
FAKEUDS_PORT=5433
```

We should really have multiple "box" scripts that are less complete boxes - that is, they don't include the fake UDS and/or the DBfiller. Good, concise names on a postcard...

### Example

I've appended the docker-compose command `ps` to `box.sh`, and you can see all the services from all our repos in one place!
```
(develop) $ ./box.sh ps
Emap in a box config file: /Users/jeremystein/Emap/box-config-envs
++ docker-compose -f /Users/jeremystein/Emap/Emap-Core/docker-compose.yml -f /Users/jeremystein/Emap/Emap-Core/docker-compose.fakeuds.yml -f /Users/jeremystein/Emap/Emap-Core/../DatabaseFiller/docker-compose.yml -p jes1 ps
WARNING: The HTTP_PROXY variable is not set. Defaulting to a blank string.
WARNING: The http_proxy variable is not set. Defaulting to a blank string.
WARNING: The HTTPS_PROXY variable is not set. Defaulting to a blank string.
WARNING: The https_proxy variable is not set. Defaulting to a blank string.
      Name                    Command                State                                               Ports                                           
      ---------------------------------------------------------------------------------------------------------------------------------------------------------
      jes1_dbfiller_1    java -jar ./target/DBFille ...   Exit 255                                                                                             
      jes1_emapstar_1    /usr/local/bin/mvn-entrypo ...   Up                                                                                                   
      jes1_fakeuds_1     docker-entrypoint.sh postgres    Up         0.0.0.0:5433->5432/tcp                                                                    
      jes1_hl7source_1   /usr/local/bin/mvn-entrypo ...   Up                                                                                                   
      jes1_rabbitmq_1    docker-entrypoint.sh rabbi ...   Up         15671/tcp, 0.0.0.0:15972->15672/tcp, 25672/tcp, 4369/tcp, 5671/tcp, 0.0.0.0:5972->5672/tcp
```

# emapstar

## Dependencies

The Dockerfiles have to build the dependencies before building Emap-Core. Therefore your local directory structure has to be like this:

```
Emap [project dir, name doesn't actually matter]
 |
 +-- box-config-envs [emap in a box config file]
 +-- Emap-Core [git repo]
 |  |
 |  +-- box.sh [runs emap in a box]
 |  +-- docker-compose.yml [sets build directory to be .. (aka Emap)]
 +-- Emap-Interchange [git repo]
 +-- Inform-DB [git repo]
 +-- Some-Other-Repo [your repo goes here, eg. Caboodle]
```

The `docker-compose.yml` file sets the build directory to be the project root directory, to allow the Emap-Core Dockerfiles
to reference the code containing the dependencies.

## `config-envs` file

This file is used by hl7source and emapstar to point to the IDS, UDS, and rabbitmq server.

The required envs in this file with example values are found in [config-envs.EXAMPLE](config-envs.EXAMPLE)

## `rabbit-envs` file

This sets the username+password on the rabbitmq server. If you're on the GAE this should be a strong password to help prevent a user/malware outside the GAE from accessing the queue.

[rabbit-envs.EXAMPLE](rabbit-envs.EXAMPLE)

## `.env` file

In order to use environment variables from the docker-compose.yml file itself, they need to be in a file called `.env`. Hence yet another file.

This is used for specifying which port on the host your rabbitmq queue should bind to. Example found here:

[.env.EXAMPLE](.env.EXAMPLE)

We should allocate ports to people to avoid clashes, and double check what the firewall rules are for different ports. In the meantime, please use a strong password on your rabbitmq server.

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
