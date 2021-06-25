package uk.ac.ucl.rits.inform.interchange;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author Jeremy Stein
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LocationMetadata extends EmapOperationMessage {
    private String hl7String;

    private String epicDepartmentName;
    private Instant epicDepartmentMpiFromDate;
    private Instant epicDepartmentMpiToDate;
    /**
     * The department record status.
     * Possible values in ZC_PBA_REC_STAT.
     * https://datahandbook.epic.com/ClarityDictionary/Details?tblName=CLARITY_DEP
     */
    private String departmentRecordStatus;
    private Long epicRoomId;

    /**
     * Room record state.
     * Possible values in ZC_PBA_REC_STAT.
     * https://datahandbook.epic.com/ClarityDictionary/Details?tblName=CLARITY_ROM
     */
    private String roomRecordState;
    private String roomName;
    private String roomAssociatedPoolBedName;

    /**
     * CLARITY_ROM.BED_POOL_CENSUS_YN Indicates whether the pool beds for the room
     * record should be included in bed census reports.
     * https://datahandbook.epic.com/ClarityDictionary/Details?tblName=CLARITY_ROM
     */
    private Boolean roomPoolBedIsInCensus;
    private Long epicBedId;

    /**
     * The bed record state.
     * Possible values in ZC_PBA_REC_STAT.
     * <p>
     */
    private String bedRecordState;

    /**
     * Whether the bed record should be
     * included in bed census reports.
     * <p>
     *
     * Roma says: Census beds are beds that count as real beds
     * where you stay (and we get ADT messages for), rather than non-census beds
     * which are transient and don't trigger ADT messages (at the moment, and when
     * they do, it's a different type).
     * E.g. Say you have a patient in ED. They have a location which is the bed in
     * ED they are assigned to. Let's say that it is ED-1. Then they decide they
     * want to take the patient to get an X-RAY. They want to record that the
     * patient is going to X-RAY Room 1 but without freeing up bed ED-1 (so it's
     * still there for the patient to come back to). So ED-1 is the census bed
     * (where the patient is considered to be), while X-RAY Room 1 is the non-census
     * location (where they are at the moment, but moving here doesn't move them out
     * of their current census location, because it's not freeing up space, but you
     * do need to track that the non-census location has a person there and so is in
     * use.)
     */
    private Boolean bedIsInCensus;

    /**
     * Pool beds are transient beds.
     */
    private Boolean isPoolBed;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
