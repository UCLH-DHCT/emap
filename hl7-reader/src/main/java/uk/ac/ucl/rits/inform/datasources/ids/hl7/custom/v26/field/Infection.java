package uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.field;

import ca.uhn.hl7v2.model.AbstractComposite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v26.datatype.DT;
import ca.uhn.hl7v2.model.v26.datatype.ST;

public class Infection extends AbstractComposite {

    private Type[] data;

    /**
     * Creates a new Infection type.
     * @param message hl7message
     */
    public Infection(Message message) {
        super(message);
        init();
    }

    private void init() {
        data = new Type[3];
        data[0] = new ST(getMessage());
        data[1] = new DT(getMessage());
        data[2] = new DT(getMessage());

    }


    /**
     * Returns an array containing the data elements.
     * @return all components
     */
    public Type[] getComponents() {
        return this.data;
    }

    /**
     * Returns an individual data component.
     * @param number The component number (0-indexed)
     * @return component at index
     * @throws DataTypeException if the given element number is out of range.
     */
    public Type getComponent(int number) throws DataTypeException {
        try {
            return this.data[number];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DataTypeException(String.format(
                    "Element %d doesn't exist (Type %s has only %d components)", number, getClass().getName(), this.data.length));
        }
    }


    /**
     * Returns Infection Name (component 1).  This is a convenience method that saves you from
     * casting and handling an exception.
     * @return infection name
     */
    public ST getInfection1Name() {
        return getTyped(0, ST.class);
    }


    /**
     * Returns Added DateTime (component 2).  This is a convenience method that saves you from
     * casting and handling an exception.
     * @return added date time
     */
    public DT getInfection2AddedDateTime() {
        return getTyped(1, DT.class);
    }


    /**
     * Returns Resolved DateTime (component 3).  This is a convenience method that saves you from
     * casting and handling an exception.
     * @return resolved date time
     */
    public DT getInfection3ResolvedDateTime() {
        return getTyped(2, DT.class);
    }

}
