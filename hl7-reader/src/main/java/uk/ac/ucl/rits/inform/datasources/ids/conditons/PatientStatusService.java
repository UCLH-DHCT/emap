package uk.ac.ucl.rits.inform.datasources.ids.conditons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.interchange.PatientConditionMessage;

import java.time.Instant;

public final class PatientStatusService {

    private static final Logger logger = LoggerFactory.getLogger(PatientStatusService.class);

    private PatientStatusService() {};

    /**
     * Adding a patient condition if it has an added datetime and adjusting the progress time stamp so that only
     * conditions thereafter will be added to the collection.
     * @param msg Interchange message
     * @param progress Current progress
     * @return If the progress should be updated with the message addedDatetime
     */
    public static boolean shouldUpdateProgressAndAddMessage(PatientConditionMessage msg, Instant progress) {

        Instant addedDatetime = msg.getAddedDatetime();
        if (addedDatetime == null || addedDatetime.isBefore(progress)) {
            logger.debug("Processing skipped as current infection added is {} and progress is {}", addedDatetime, progress);
            return false;
        }
        return true;
    }
}
