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
     * Room record state from CLARITY_ROM.RECORD_STATE(_C).
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
     * The bed record state from CLARITY_BED.RECORD_STATE.
     * Possible values in ZC_PBA_REC_STAT.
     * https://datahandbook.epic.com/ClarityDictionary/Details?tblName=CLARITY_BED
     */
    private String bedRecordState;

    /**
     * CLARITY_BED.CENSUS_INCLUSN_YN Indicates whether the bed record should be
     * included in bed census reports.
     * https://datahandbook.epic.com/ClarityDictionary/Details?tblName=CLARITY_BED
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
