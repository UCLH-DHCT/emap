package uk.ac.ucl.rits.inform.informdb;

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
    private Instant           presentation_time;

    /**
     * The time the patient was formally admitted.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           admission_time;

    /**
     * The time the patient was discharged.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           discharge_time;

    /**
     * The patient class. E.g. Inpatient or Outpaitent.
     */
    private String            patient_class;

    /**
     * The patient's arrival method at hospital.
     */
    private String            arrival_method;

    /**
     * Where the patient went after their departure.
     */
    private String            discharge_destination;

    /**
     * The patient's disposition on departure.
     */
    private String            discharge_disposition;

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
     * @return the presentation_time
     */
    public Instant getPresentation_time() {
        return presentation_time;
    }

    /**
     * @param presentation_time the presentation_time to set
     */
    public void setPresentation_time(Instant presentation_time) {
        this.presentation_time = presentation_time;
    }

    /**
     * @return the admission_time
     */
    public Instant getAdmission_time() {
        return admission_time;
    }

    /**
     * @param admission_time the admission_time to set
     */
    public void setAdmission_time(Instant admission_time) {
        this.admission_time = admission_time;
    }

    /**
     * @return the discharge_time
     */
    public Instant getDischarge_time() {
        return discharge_time;
    }

    /**
     * @param discharge_time the discharge_time to set
     */
    public void setDischarge_time(Instant discharge_time) {
        this.discharge_time = discharge_time;
    }

    /**
     * @return the patient_class
     */
    public String getPatient_class() {
        return patient_class;
    }

    /**
     * @param patient_class the patient_class to set
     */
    public void setPatient_class(String patient_class) {
        this.patient_class = patient_class;
    }

    /**
     * @return the arrival_method
     */
    public String getArrival_method() {
        return arrival_method;
    }

    /**
     * @param arrival_method the arrival_method to set
     */
    public void setArrival_method(String arrival_method) {
        this.arrival_method = arrival_method;
    }

    /**
     * @return the discharge_destination
     */
    public String getDischarge_destination() {
        return discharge_destination;
    }

    /**
     * @param discharge_destination the discharge_destination to set
     */
    public void setDischarge_destination(String discharge_destination) {
        this.discharge_destination = discharge_destination;
    }

    /**
     * @return the discharge_disposition
     */
    public String getDischarge_disposition() {
        return discharge_disposition;
    }

    /**
     * @param discharge_disposition the discharge_disposition to set
     */
    public void setDischarge_disposition(String discharge_disposition) {
        this.discharge_disposition = discharge_disposition;
    }

    @Override
    public String toString() {
        return String.format("HospitalVisit [hospital_visit_durable_id=%d, encounter=%s, source_system=%s]",
                this.hospitalVisitDurableId, encounter, sourceSystem);
    }

}
