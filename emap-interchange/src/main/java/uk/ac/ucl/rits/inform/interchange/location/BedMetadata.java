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
public class BedMetadata implements Serializable {
    /**
     * Unique ID for the contact, can have multiple from the same hl7 representation.
     */
    private Long bedCsn;
    private String bedHl7;
    private Instant bedContactDate;
    /**
     * The bed record state.
     * See {@link EpicRecordStatus} for values
     */
    private EpicRecordStatus bedRecordState;
    /**
     * Pool beds are transient beds, usually waiting areas.
     */
    private Boolean isPoolBed;
    /**
     * Duplicate beds will be created in EPIC if moving someone to a bunk bed that is already filled.
     */
    private Boolean isBunkBed;
    /**
     * Whether the bed record should be
     * included in bed census reports.
     * <p>
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
    private String bedFacility;
}
