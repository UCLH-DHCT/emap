package uk.ac.ucl.rits.inform.interchange.visit_observations;

import java.time.Instant;

/**
 * Shared interface for all visit observation, with minimal information to create a new visit observation type in the core processor.
 */
public interface ObservationType {
    /**
     * @return Id of observation in application.
     */
    public String getId();

    /**
     * @return Type of observation (e.g. flowsheet)
     */
    public String getSourceObservationType();

    /**
     * @return System that provided the information (e.g. HL7, caboodle, clarity)
     */
    public String getSourceSystem();

    /**
     * @return Most recent update to the observation type or observation
     */
    public Instant getLastUpdatedInstant();
}
