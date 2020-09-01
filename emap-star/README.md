# Emap-Star

This repository describes the structure of the Emap-Star database (formerly known as Inform-db), using Hibernate to map between Java concepts and the relational model.


# Design considerations and conventions

## Intro

We chose an EAV structure ([Entity–attribute–value](https://en.wikipedia.org/wiki/Entity%E2%80%93attribute%E2%80%93value_model)) for the recording of data, as we'll need to frequently add new attributes without changing the database schema. Eg. We want to record a new type of vital sign in the DB, so we add a new attribute for it rather than a new column in our DB.

Its main feature of great flexibility in the types of data we can store is arguably also its greatest weakness, in that the design can sprawl into something quite messy, eg. multiple attributes for the same thing, like we had in ICIP. Therefore keeping some control over the addition of new attributes is important.

The attributes are defined in this repository in vocab.csv and AttributeKeyMap.java. There is some redundancy between these two files, this is checked for contradictions by a unit test in the Emap-Core repository, which arguably should be moved here.

## Patient identity


## Time travel

There are believed to be as many as three kinds of time travel in Emap-Star.

## Attribute management

Attribute IDs, once assigned, are never changed or deleted. Only new ones can be added. Attributes can be deprecated if they are no longer to be used. All "short_name"s must be unique.

How to avoid semantic duplicates?

## Properties



## DB naming conventions

Every table has a primary key ID field named after the name of the table with "_id" added to the end.

```
                         Table "public.patient_property"
    Column     |           Type           | Collation | Nullable | Default
---------------+--------------------------+-----------+----------+---------
patient_property_id | bigint          |           | not null |
...
```

In Java, these are defined as `Long` types, which default to null, allowing the auto ID generation to do its thing. `long` would default to 0.

```java
@GeneratedValue(strategy = GenerationType.AUTO)
private Long                    patientPropertyId;
```

Foreign key columns...

`valid_from` and `valid_until` are used for the "what did the doctor know" type of time travel.
`stored_from` and `stored_until` are used for the "when did Emap know it" type of time travel.