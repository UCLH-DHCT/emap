package uk.ac.ucl.rits.inform.interchange;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Interface defining a patient condition message
 * @author Tom Young
 */
public interface PatientConditionMessage {

    String getMrn();

    InterchangeValue<String> getVisitNumber();

    /**
     * Code defining the infection or problem
     */
    String getCode();

    /**
     * Human-readable name of the condition
     */
    InterchangeValue<String> getName();

    /**
     * Time of the update or message carrying this information.
     */
    Instant getUpdatedDateTime();

    /**
     * Unique Id for problem in EPIC.
     */
    InterchangeValue<Long> getEpicId();

    /**
     * Problem added at...
     */
    Instant getAddedTime();

    /**
     * Problem resolved at...
     */
    InterchangeValue<Instant> getResolvedTime();

    /**
     * Onset of problem known at...
     */
    InterchangeValue<LocalDate> getOnsetTime();

    /**
     * Status of the condition
     */
    InterchangeValue<String> getStatus();

    /**
     * Effectively message type, i.e. whether to add, update or delete the patient problem
     */
    String getAction();

    /**
     * Notes in relation to problem...
     */
    InterchangeValue<String> getComment();


    /**
     * System which this message came from
     */
    String getSourceSystem();
}
