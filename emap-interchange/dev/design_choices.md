Design decisions relating specifically to the interchange format and it's data are recorded here. Roughly following the
[architecture decision record template by Michael Nygard](https://github.com/joelparkerhenderson/architecture_decision_record/edit/master/adr_template_by_michael_nygard.md).



# Determine if an HL7 Value should be saved, deleted or ignored.


## Context

- HL7 messages can have a blank field when the value is unknown, "" if the value should be deleted or a value if known.
  We don't want to force HL7 syntax throughout our application.
- If a known value, the interchange null value should probably cause the current value to be deleted. 
  

## Options

### Using the Optional<> class 

The Java Optional class is pretty close to what we are looking for.

#### Pros

- Class exists already and API is known 
- Has useful methods for processing
- Interchange message fields which may or may not be present, will never be null - avoiding ambiguity or null pointer errors.

#### Cons

- Not serialisable by default, so some subclassing or similar would have to be done
- Not quite sure if you can define a difference between unknown value and a delete value
- A lot of extra functionality that isn't required


### Custom Hl7Value<> wrapper class

Create custom wrapper class for values. Storing the expected outcome (save, delete or ignore) and the value.
Build method from HL7 value would instantiate the wrapper with the correct data. 

#### Pros

- Interchange message fields which may or may not be present, will never be null - avoiding ambiguity or null pointer errors.
- Reduce duplication of code by using build from HL7Value to determine the final action of the value.


#### Cons

- Creating a custom class instead of using a library.


## Decision

*Custom Hl7Value<> wrapper class*: created an HL7Value wrapper class to ensure the expected outcome of HL7 message.

## Consequences

- The intended outcome of an HL7 value will be clear.
- Creating interchange messages will be more verbose because they will be wrapped in HL7Value
- Single way to determine the outcome from an hl7Field 
- Setting values from the HL7Value wrapper will need to be done using an `assignTo` method.
