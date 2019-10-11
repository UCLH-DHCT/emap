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
```
IDS_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/ids
IDS_USERNAME=someuser
IDS_PASSWORD=redacted
INFORMDB_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/informdb
INFORMDB_USERNAME=someuser
INFORMDB_PASSWORD=redacted
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=8672
SPRING_RABBITMQ_USERNAME=someuser
SPRING_RABBITMQ_PASSWORD=redacted
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
