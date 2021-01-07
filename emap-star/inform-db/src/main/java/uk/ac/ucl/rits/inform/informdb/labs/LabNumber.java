package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.Instant;

/**
 * A lab number represents a reference number attached to a lab. As labs are
 * often done externally, there is separate fields to track the lab number from
 * the point of view of the EHR compared to the system processing the lab.
 * @author Roma Klapaukh
 */
@Entity
@Data
public class LabNumber implements Serializable {

    private static final long serialVersionUID = -5771782759320217911L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labNumberId;

    private long mrnId;
    private long hospitalVisitDurableId;

    /**
     * Lab number in the EHR.
     */
    private String internalLabNumber;
    /**
     * Lab number for the system doing the lab test.
     */
    private String externalLabNumber;
    private String sourceSystem;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedFrom;
}
