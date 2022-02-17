# Local setup instructions using IntelliJ IDEA

- You can follow the [Emap-Core repository](https://github.com/inform-health-informatics/Emap-Core/)
  steps for setting up a local instance, cloning `emap-hl7-processor` and `Emap-Interchange`
  and adding their maven `pom.xml` files.
- If you are creating a directory only for hl7 processing, then you don't need to set up annotation preprocessing

# How to deploy a live version

Please see the [emap-core repository](https://github.com/inform-health-informatics/Emap-Core/)
for an overview of deploying a live instance of EMAP services

## Dependencies

- Repositories must be checked out to the correct branches.
  "Correct" will depend on what you're trying to do.
- Conventionally a live instance would all be deployed off master,
  but during the development phase `develop` is more likely to be the correct branch.
- Occasionally you may need to deploy off a feature branch -
  if in doubt ask the author of that code.

```shell script
 git clone --branch develop https://github.com/inform-health-informatics/emap-hl7-processor.git
 git clone --branch develop https://github.com/inform-health-informatics/Emap-Interchange.git
```

## config

Supply the required config files in the `config` directory required by the emap-core deployment. This service uses two
of these config files:

### `emap-hl7processor-config-envs` file

This file is used by hl7source (the hl7 processor's docker container) to point to the IDS and rabbitmq server.

The required envs in this file with example values are found in
[emap-hl7processor-config-envs.EXAMPLE](emap-hl7processor-config-envs.EXAMPLE)

# HAPI

All HL7 parsing is done with HAPI, allowing for [handling of multiple versions of HL7](https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html)

## From [HAPI FAQ](https://hapifhir.github.io/hapi-hl7v2/hapi-faq.html ): 

### Why are some message classes missing? For example, I can find the class ADT_A01, but not the class ADT_A04.

HL7 defines that some message triggers reuse the same structure. So, for example,
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
