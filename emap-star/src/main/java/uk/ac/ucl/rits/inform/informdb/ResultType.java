package uk.ac.ucl.rits.inform.informdb;

/**
 * Possible result types for Attributes that take values.
 *
 * @author UCL RITS
 *
 */
public enum ResultType {

    /**
     * This attribute doesn't have a result.
     */
    None,
    /**
     * This attribute has a string result.
     */
    String,
    /**
     * This attribute is a date & time.
     */
    Datetime,
    /**
     * This attribute is a foreign key to a table.
     */
    Link

}
