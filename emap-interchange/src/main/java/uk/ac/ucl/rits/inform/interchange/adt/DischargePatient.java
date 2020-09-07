package uk.ac.ucl.rits.inform.interchange.adt;

import java.time.Instant;

/**
 * Discharge a patient.
 * HL7 messages: A03
 */
public class DischargePatient extends AdtMessageBase {
    private static final long serialVersionUID = -1528594767815651653L;

    private Instant dischargeDateTime;
    private String dischargeDisposition;
    private String dischargeLocation;

    /**
     * @return the dischargeDateTime
     */
    public Instant getDischargeDateTime() {
        return dischargeDateTime;
    }

    /**
     * @param dischargeDateTime the dischargeDateTime to set
     */
    public void setDischargeDateTime(Instant dischargeDateTime) {
        this.dischargeDateTime = dischargeDateTime;
    }

    /**
     * @return the dischargeDisposition
     */
    public String getDischargeDisposition() {
        return dischargeDisposition;
    }

    /**
     * @param dischargeDisposition the dischargeDisposition to set
     */
    public void setDischargeDisposition(String dischargeDisposition) {
        this.dischargeDisposition = dischargeDisposition;
    }

    /**
     * @return the dischargeLocation
     */
    public String getDischargeLocation() {
        return dischargeLocation;
    }

    /**
     * @param dischargeLocation the dischargeLocation to set
     */
    public void setDischargeLocation(String dischargeLocation) {
        this.dischargeLocation = dischargeLocation;
    }

}
