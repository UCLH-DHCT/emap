package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovement;

import java.time.Instant;
import java.util.Optional;

/**
 * Interaction with the PlannedMovement table.
 * @author Stef Piatek
 */
public interface PlannedMovementRepository extends CrudRepository<PlannedMovement, Long> {
    /**
     * Try and find a matching planned movement from a pending adt message (and not from a cancel pending adt message).
     * <p>
     * Always find by the event type, planned location and hospital visit Id, then:
     * - For in order messages: find the most recent message by the eventDatetime
     * - For cancel message arriving before the original pending adt: find message which has a null eventDatetime and a cancel after current datetime
     * @param eventType       type of the planned movement
     * @param hospitalVisitId hospital visit associated with the movement
     * @param plannedLocation planned location for the movement
     * @param eventDatetime   the datetime that event was created
     * @return Optional planned movement entity
     */
    @Query("from PlannedMovement "
            + "where eventType = :eventType and hospitalVisitId = :hospitalVisitId and locationId = :plannedLocation "
            + "and (eventDatetime < :eventDatetime "
            + "      or (eventDatetime is null and cancelledDatetime >= :eventDatetime))"
            + "order by eventDatetime, cancelledDatetime"
    )
    Optional<PlannedMovement> findFirstFromPlannedMovement(
            String eventType, HospitalVisit hospitalVisitId, Location plannedLocation, Instant eventDatetime
    );

    /**
     * For testing.
     * @param encounter      encounter string
     * @param locationString location string
     * @return optional planned movement
     */
    Optional<PlannedMovement> findByHospitalVisitIdEncounterAndLocationIdLocationString(String encounter, String locationString);
}
