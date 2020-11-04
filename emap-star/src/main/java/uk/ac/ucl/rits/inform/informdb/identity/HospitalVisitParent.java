package uk.ac.ucl.rits.inform.informdb.identity;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Parent class that is not created as an entity to avoid polymorphic queries based on the original and audit table.
 * <p>
 * See {@link HospitalVisit} for more details
 * @author UCL RITS
 */
@MappedSuperclass
public abstract class HospitalVisitParent extends TemporalCore<HospitalVisit, HospitalVisitAudit> implements Serializable {

    private static final long serialVersionUID = -8922743168233635681L;
    /**
     * The MRN this hospital visit happened under.
     */
    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    /**
     * The source system from which we learnt about this hospital visit.
     */
    @Column(nullable = false)
    private String sourceSystem;

    /**
     * The time the patient was first seen in the hospital as part of their visit.
     * This may be prior to their admission.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant presentationTime;

    /**
     * The time the patient was formally admitted.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant admissionTime;

    /**
     * The time the patient was discharged.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant dischargeTime;

    /**
     * The patient class. E.g. Inpatient or Outpaitent.
     */
    private String patientClass;

    /**
     * The patient's arrival method at hospital.
     */
    private String arrivalMethod;

    /**
     * Where the patient went after their departure.
     */
    private String dischargeDestination;

    /**
     * The patient's disposition on departure.
     */
    private String dischargeDisposition;

    /**
     * Default constructor.
     */
    public HospitalVisitParent() {
    }

    /**
     * Build a hospital visit from an existing object.
     * @param other hospital visit
     */
    public HospitalVisitParent(HospitalVisitParent other) {
        super(other);
        this.admissionTime = other.admissionTime;
        this.arrivalMethod = other.arrivalMethod;
        this.dischargeDestination = other.dischargeDestination;
        this.dischargeDisposition = other.dischargeDisposition;
        this.dischargeTime = other.dischargeTime;
        this.mrnId = other.mrnId;
        this.patientClass = other.patientClass;
        this.presentationTime = other.presentationTime;
        this.sourceSystem = other.sourceSystem;
    }


    /**
     * @return the mrn
     */
    public Mrn getMrnId() {
        return mrnId;
    }

    /**
     * @param mrnId the mrn to set
     */
    public void setMrnId(Mrn mrnId) {
        this.mrnId = mrnId;
    }

    /**
     * @return the sourceSystem
     */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /**
     * @param sourceSystem the sourceSystem to set
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * @return the presentationTime
     */
    public Instant getPresentationTime() {
        return presentationTime;
    }

    /**
     * @param presentationTime the presentationTime to set
     */
    public void setPresentationTime(Instant presentationTime) {
        this.presentationTime = presentationTime;
    }

    /**
     * @return the admissionTime
     */
    public Instant getAdmissionTime() {
        return admissionTime;
    }

    /**
     * @param admissionTime the admissionTime to set
     */
    public void setAdmissionTime(Instant admissionTime) {
        this.admissionTime = admissionTime;
    }

    /**
     * @return the dischargeTime
     */
    public Instant getDischargeTime() {
        return dischargeTime;
    }

    /**
     * @param dischargeTime the dischargeTime to set
     */
    public void setDischargeTime(Instant dischargeTime) {
        this.dischargeTime = dischargeTime;
    }

    /**
     * @return the patientClass
     */
    public String getPatientClass() {
        return patientClass;
    }

    /**
     * @param patientClass the patientClass to set
     */
    public void setPatientClass(String patientClass) {
        this.patientClass = patientClass;
    }

    /**
     * @return the arrivalMethod
     */
    public String getArrivalMethod() {
        return arrivalMethod;
    }

    /**
     * @param arrivalMethod the arrivalMethod to set
     */
    public void setArrivalMethod(String arrivalMethod) {
        this.arrivalMethod = arrivalMethod;
    }

    /**
     * @return the dischargeDestination
     */
    public String getDischargeDestination() {
        return dischargeDestination;
    }

    /**
     * @param dischargeDestination the dischargeDestination to set
     */
    public void setDischargeDestination(String dischargeDestination) {
        this.dischargeDestination = dischargeDestination;
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

    @Override
    public String toString() {
        return String.format("HospitalVisitParent [source_system=%s]", sourceSystem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HospitalVisitParent that = (HospitalVisitParent) o;
        return Objects.equals(mrnId, that.mrnId)
                && Objects.equals(sourceSystem, that.sourceSystem)
                && Objects.equals(presentationTime, that.presentationTime)
                && Objects.equals(admissionTime, that.admissionTime)
                && Objects.equals(dischargeTime, that.dischargeTime)
                && Objects.equals(patientClass, that.patientClass)
                && Objects.equals(arrivalMethod, that.arrivalMethod)
                && Objects.equals(dischargeDestination, that.dischargeDestination)
                && Objects.equals(dischargeDisposition, that.dischargeDisposition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mrnId, sourceSystem, presentationTime, admissionTime, dischargeTime,
                patientClass, arrivalMethod, dischargeDestination, dischargeDisposition);
    }
}
