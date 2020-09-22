package uk.ac.ucl.rits.inform.informdb.identity;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This a single visit to the hospital. This is not necessarily an inpatient
 * visit, but includes outpatients, imaging, etc.
 *
 * @author UCL RITS
 *
 */
@Entity
@Table(indexes = { @Index(name = "encounterIndex", columnList = "encounter", unique = false) })
@JsonIgnoreProperties({ "mrns" })
public class HospitalVisit implements Serializable {

    private static final long serialVersionUID = -6495238097074592105L;

    /**
     * A primary key for this row. Due to the temporal nature of the table, this
     * should not be used for linkage.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long              hospitalVisitId;

    /**
     * A persistent key for this hospital visit. As data is updated, the durable key
     * will stay the same. This should be used for all joins.
     */
    @Column(nullable = false)
    private Long              hospitalVisitDurableId;

    /**
     * The source system identifier of this hospital visit. In Epic this corresponds
     * to the CSN.
     */
    @Column(nullable = false)
    private String            encounter;

    /**
     * The source system from which we learnt about this hospital visit.
     */
    @Column(nullable = false)
    private String            sourceSystem;

    /**
     * The time the patient was first seen in the hospital as part of their visit.
     * This may be prior to their admission.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           presentationTime;

    /**
     * The time the patient was formally admitted.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           admissionTime;

    /**
     * The time the patient was discharged.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           dischargeTime;

    /**
     * The patient class. E.g. Inpatient or Outpaitent.
     */
    private String            patientClass;

    /**
     * The patient's arrival method at hospital.
     */
    private String            arrivalMethod;

    /**
     * Where the patient went after their departure.
     */
    private String            dischargeDestination;

    /**
     * The patient's disposition on departure.
     */
    private String            dischargeDisposition;

    /**
     * @return the hospitalVisitId
     */
    public Long getHospitalVisitId() {
        return hospitalVisitId;
    }

    /**
     * @param hospitalVisitId the hospitalVisitId to set
     */
    public void setHospitalVisitId(Long hospitalVisitId) {
        this.hospitalVisitId = hospitalVisitId;
    }

    /**
     * @return the hospitalVisitDurableId
     */
    public Long getHospitalVisitDurableId() {
        return hospitalVisitDurableId;
    }

    /**
     * @param hospitalVisitDurableId the hospitalVisitDurableId to set
     */
    public void setHospitalVisitDurableId(Long hospitalVisitDurableId) {
        this.hospitalVisitDurableId = hospitalVisitDurableId;
    }

    /**
     * @return the encounter
     */
    public String getEncounter() {
        return encounter;
    }

    /**
     * @param encounter the encounter to set
     */
    public void setEncounter(String encounter) {
        this.encounter = encounter;
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
        return String.format("HospitalVisit [hospital_visit_durable_id=%d, encounter=%s, source_system=%s]",
                this.hospitalVisitDurableId, encounter, sourceSystem);
    }

}
