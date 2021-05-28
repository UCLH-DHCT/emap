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

    private String EpicDepartmentName;
    private Instant EpicDepartmentMpiFromDate;
    private Instant EpicDepartmentMpiToDate;
    private Long DepartmentRecordStatusId;
    private Long EpicRoomId;
    private String RoomRecordState;
    private String RoomName;
    private String RoomAssociatedPoolBedName;
    private Boolean RoomPoolBedIsInCensus;
    private Long EpicBedId;
    private String BedRecordState;
    private Boolean BedIsInCensus;
    private Boolean IsPoolBed;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
