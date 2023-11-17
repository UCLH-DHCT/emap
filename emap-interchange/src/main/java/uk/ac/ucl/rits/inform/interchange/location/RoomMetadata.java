package uk.ac.ucl.rits.inform.interchange.location;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EpicRecordStatus;

import java.io.Serializable;
import java.time.Instant;

@Data
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class RoomMetadata implements Serializable {
    /**
     * Unique ID for the contact, can have multiple from the same hl7 representation.
     */
    private Long roomCsn;
    private Boolean isRoomReady;
    private Instant roomContactDate;
    /**
     * Room record state.
     * See {@link EpicRecordStatus} for values
     */
    private EpicRecordStatus roomRecordState;
    private String roomHl7;
    private String roomName;

}
