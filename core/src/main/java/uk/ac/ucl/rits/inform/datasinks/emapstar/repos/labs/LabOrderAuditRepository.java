package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.time.Instant;
import java.util.List;

/**
 * Lab Order Audit repository.
 * @author Stef Piatek
 */
public interface LabOrderAuditRepository extends CrudRepository<LabOrderAudit, Long> {
    /**
     * Find Audit Ids.
     * @param labBatteryId  lab battery id
     * @param labSampleId   lab sample id
     * @param orderDateTime order date time
     * @param epicOrder     epic order number
     * @return list of ids
     */
    @Query("select a.labOrderAuditId from LabOrderAudit a where (a.labBatteryId = :labBatteryId and a.labSampleId = :labSampleId) "
            + "AND (a.orderDatetime = :orderDateTime or a.internalLabNumber = :epicOrder) ")
    List<Long> findAllIds(Long labBatteryId, Long labSampleId, Instant orderDateTime, String epicOrder);

    /**
     * Assuming an entity was created, it was previously deleted if it has an audit log.
     * @param labBatteryId          lab battery id
     * @param labSampleId           lab sample id
     * @param possibleOrderDateTime order date time
     * @param possibleEpicOrder     epic order number
     * @return true if found in audit table.
     */
    default boolean previouslyDeleted(
            Long labBatteryId, Long labSampleId, InterchangeValue<Instant> possibleOrderDateTime, InterchangeValue<String> possibleEpicOrder) {
        Instant orderDateTime = null;
        String orderNumber = null;
        if (possibleOrderDateTime.isSave()) {
            orderDateTime = possibleOrderDateTime.get();
        }
        if (possibleEpicOrder.isSave()) {
            orderNumber = possibleEpicOrder.get();
        }
        return !findAllIds(labBatteryId, labSampleId, orderDateTime, orderNumber).isEmpty();

    }
}
