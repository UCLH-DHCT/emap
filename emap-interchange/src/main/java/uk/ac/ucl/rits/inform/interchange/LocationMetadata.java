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
    private Long departmentRecordStatusId;
    private Long epicRoomId;
    private String roomRecordState;
    private String roomName;
    private String roomAssociatedPoolBedName;
    private Boolean roomPoolBedIsInCensus;
    private Long epicBedId;
    private String bedRecordState;
    private Boolean bedIsInCensus;
    private Boolean isPoolBed;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
