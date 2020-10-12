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

