package uk.ac.ucl.rits.inform.interchange;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Interface defining a patient condition message, either a 'problem' (aka. problem list) or an infection
 * @author Tom Young
 */
public interface PatientConditionMessage {

    String getSourceSystem();
    String getMrn();
    String getConditionCode();
    Instant getUpdatedDateTime();
    String getAction();
    String getStatus();
    Instant getAddedTime();
    InterchangeValue<String> getVisitNumber();
    InterchangeValue<String> getComment();
    InterchangeValue<Long> getEpicConditionId();
    InterchangeValue<String> getConditionName();
    InterchangeValue<Instant> getResolvedTime();
    InterchangeValue<LocalDate> getOnsetTime();
}
