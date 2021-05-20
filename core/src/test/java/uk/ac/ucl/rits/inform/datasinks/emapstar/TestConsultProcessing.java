package uk.ac.ucl.rits.inform.datasinks.emapstar;

public class TestConsultProcessing extends MessageProcessingBase {

    /**
     * Given that no MRNS or hospital visits exist in the database
     * When a consult message is processed
     * Then minimal MRN and hospital visit should be created
     */

    /**
     * Given that MRNs and hospital visits exist in the database
     * When a consult message is processed with existing mrn and visit number
     * Then minimal MRN and hospital visit should not be created
     */


    /**
     * Given that no consult types exist in the database
     * When a consult message is processed
     * A new minimal consult type (only populating the code and source system, leaving the name column empty for hoovering) should be created
     */

    /**
     * Given that no consults exist in the database
     * When a consult message is processed
     * A new consult should be created (in addition to PK and FKs want to store internalConsultId, requestedDateTime, storedFrom, validFrom)
     */

    /**
     * Given that no questions and consult questions exist in the database
     * When a consult message is processed with 3 questions
     * Then 3 questions should be created and linked to 3 consult questions for the answers to the questions
     */

    /**
     * Given that no consults exist in the database
     * When a consult message is processed with notes
     * Then a new consult should be created with the notes being saved in the comments
     */

    /**
     * Given that the minimal consult has already been processed
     * When a later consult message with cancel=true with the same epicConsultId and consultationType is processed
     * Then the cancelled column of the consult is set to true and the storedFrom and validFrom fields update
     */

    /**
     * Given that the minimal consult has already been processed
     * When a later consult message with closedDueToDischarge=true with the same epicConsultId and consultationType is processed
     * The closedAtDischarge column of the consult it set to true and the storedFrom and validFrom fields update
     */

    /**
     * Given that a minimal consult has already been processed
     * When an earlier consult message with different data is processed
     * The consult entity in question should not be updated
     */

}
