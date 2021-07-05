## Reason for changes and summary
<!--- 
Add a summary of the reasons for this change (e.g. Fixes..., overview of reasons for each type of change) 
Can be useful to document any assumptions or problems encountered
-->

## Checklist for submitter & reviewer

- [ ] copy constructor copies the primary key Id of the entity
- [ ] copy constructor copies all other fields of the entity, starting with `super(other)`
- [ ] unique constraints added if appropriate
- [ ] text definition added to columns which could have more than 255 characters
- [ ] foreign key names are in the form `<entityClass>Id`, and no typos
- [ ] during run, glowroot is checked for query speed and indexes added if needed
- [ ] validation run completed and validated
