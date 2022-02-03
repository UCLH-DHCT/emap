Design decisions relating specifically to emap-core are tracked here. Roughly following the
[architecture decision record template by Michael Nygard](https://github.com/joelparkerhenderson/architecture_decision_record/edit/master/adr_template_by_michael_nygard.md).



# Determine if an HL7 Value should be saved, deleted or ignored.




# Tracking state of database entity

## Context

- Most of the database tables have temporal data of last update and an audit table for every time
  the row entity has changed.
- Temporal data should only be set when the entity is created or updated.
  Audit table row should only be added when the original entity has changed values.

## Options

### Copy and check 

Copy the original entity, if it exists - then after processing a message check if the entity has changed.

#### Pros

- simple and will be easy to know if it's working.

#### Cons

- Might be extra processing for no reason.
- Requires a lot of repetition, with separate branches of logic for created or updated.

### State Wrapper

Wrap the entity get or create output into a class that tracks it's state (boolean for isCreated).
Update entity values through the state wrapper: checking if the new and current values differ,
if they do then update the value and change the state to isUpdated.

#### Pros

- Reusable for every time you're dealing with a tracked entity.
- Only if a value is being updated, set temporal data.
- Can create methods to convert data types on setting.

#### Cons

- Requires all updating of the entity to be done through the State wrapper, adds some complexity.
- Creating a custom class instead of using a library.

### RxJava library

Most likely exists in the RxJava ecosystem

#### Pros

- Reuse of existing code.

#### Cons

- Not sure if we want to commit to RxJava for just one simple class.

## Decision

*State Wrapper* - Created RowState class in Emap-Core. 

## Consequences

- It will be easier to track the state of entities. Getting the state directly from the state.
- Getting or creating entities will have to be wrapped in creating a RowState instance.


# Caching of type entities

## Context

- Every time we process some data that requires a`type` entity 
  (e.g. `VisitObservation` requires a `VisitObservationType`, `LocationVisit` requires a `Location` )
- These `type` entities are only required for saving the data with the correct foreign key to the type
  - There may be extra data that is added to the `type` over time, but this doesn't affect the core processing
- We want to be able to cache these entities so we're not carrying out unnecessary queries
- Spring has a starter for caching that makes sense to use, and allows us to control what fields are used for caching
- When a class calls its own method, Spring will not intercept the method to use the cache.
  We should address a way to allow for this self invocation.

## Options for caching broker

### Default Spring caching

Requires no configuration other than enabling the Caching 

#### Pros

- simple and basically a concurrent hashmap

#### Cons

- Only allows 256 entities per cache key

### [Caffeine](https://github.com/ben-manes/caffeine/wiki/)

Continues on Guava caching as that is now deprecated.

#### Pros

- In memory, pretty much a concurrent hashmap
- Faster than [concurrent hashmap](https://github.com/ben-manes/caffeine/wiki/Benchmarks)
- Allows configuration for more than 256 entities and expiry of entities
- Configuration options investigated
  - expireAfterAccess - expire entry after the time has passed for the entry being accessed by read or write
    `Requires Java configuration and can't be done from application.properties`
  - expireAfterWrite - expire entry after the time has passed for the entry since it was created
  - maximumSize - remove rarely used items from individual cache to ensure cache doesn't grow bigger than this 

#### Cons

- Requires more thought on configuration

## Options for allowing caching method calls to be intercepted

### Self-reference as a Spring @resource

In Spring versions < 2.6, this circular reference was allowed and works as long as you only use if after
the bean has been fully initialised. Spring versions >= 2.6 throw an error so this is not a workable solution


### Using AspectJ to intercept internal method calls 

#### Pros

- No change required to the classes which implement the @Cachable methods

#### Cons

- Configuration of AspectJ seems like it would take a reasonable amount of work, 
  seems like it may require adding an extra JVM argument when we run the application.
- Would add an extra layer of complexity to understanding our application configuration

### Using a cache delegate

We could create an internal Spring bean which has the cache methods to intercept defined, 
then call the cache methods using the public Spring bean class, effectively not calling self-invoked caching methods.  

#### Pros

- Does not affect the configuration and may even reduce the number of fields required in the public Spring beans.

#### Cons

- Internal classes within the same file will add some overhead to understanding the code-base.
- Not sure if there is a way to make a shared interface for all cache delegates

## Decision

- Caffeine caching with expireAfterWrite and maximumSize in application.properties.
- Caching disabled in testing as registering and creating new cache for each test adds ~50% run time
- Use of an internal delegate cache class to allow method calls to be intercepted

## Consequences

- No effect on tests
- We should ensure that when a `type` entity is updated, we remove the current entry (or all entries) from the cache
