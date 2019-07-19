package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.time.Instant;
import java.util.Vector;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.segment.PV1;

/**
 * Wrapper around the HAPI parser's PV1 segment object, to make it easier to use.
 * Reference page: https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/PV1.html
 */
public interface PV1Wrap {
    /**
     * @return the PV1 object
     */
    PV1 getPV1();

    /**
     * @return whether the PV1 segment exists
     */
    default boolean pv1SegmentExists() {
        return getPV1() != null;
    }

    /**
     * @return PV1-2 patient class
     * @throws HL7Exception if HAPI does
     */
    default String getPatientClass() throws HL7Exception {
        if (!pv1SegmentExists()) {
            return null;
        }
        return getPV1().getPatientClass().getValue();
    }

    /**
     * @return PV1-3.1 Current Ward Code e.g. T06
     * @throws HL7Exception if HAPI does
     */
    default String getCurrentWardCode() throws HL7Exception {
        return getPV1().getAssignedPatientLocation().getPl1_PointOfCare().getValue();
    }

    /**
     * @return PV1-3.2 Current Room Code e.g. T06A
     * @throws HL7Exception if HAPI does
     */
    default String getCurrentRoomCode() throws HL7Exception {
        return getPV1().getAssignedPatientLocation().getPl2_Room().getValue();
    }

    /**
     * @return PV1-3.3 Current Bed e.g. T06-32
     * @throws HL7Exception if HAPI does
     */
    default String getCurrentBed() throws HL7Exception {
        return getPV1().getAssignedPatientLocation().getPl3_Bed().getValue();
    }

    /**
     * Get all the location components concatenated together.
     * We may need to canonicalise these to remove duplicate info.
     * @return the location string
     * @throws HL7Exception if HAPI does
     */
    default String getFullLocationString() throws HL7Exception {
        if (!pv1SegmentExists()) {
            return null;
        }
        return String.join("^", getCurrentWardCode(), getCurrentRoomCode(), getCurrentBed());
    }

    /**
     * Get the attending doctor(s) PV1-7.1 to PV1-7.7.
     * @return Vector of Doctor objects
     * @throws HL7Exception if HAPI does
     */
    default Vector<Doctor> getAttendingDoctors() throws HL7Exception {
        Vector<Doctor> v = new Vector<Doctor>();
        int reps = getPV1().getAttendingDoctorReps();
        for (int i = 0; i < reps; i++) {
            Doctor dr = new Doctor(getPV1().getAttendingDoctor(i));
            v.add(dr);
        }
        return v;
    }

    /**
     * Get Referring Doctor(s) PV1-8.1 to PV1-8.6
     *
     * I am not sure if Carecast uses PV1-8.7.
     *
     * @return Vector of Doctor objects
     * @throws HL7Exception if HAPI does
     */
    default Vector<Doctor> getReferringDoctors() throws HL7Exception {
        int reps = getPV1().getReferringDoctorReps();
        Vector<Doctor> v = new Vector<Doctor>(reps);
        for (int i = 0; i < reps; i++) {
            Doctor dr = new Doctor(getPV1().getReferringDoctor(i));
            v.add(dr);
        }
        return v;
    }

    /**
     * @return PV1-10 Hospital Service Specialty eg. 31015
     * @throws HL7Exception if HAPI does
     */
    default String getHospitalService() throws HL7Exception {
        return getPV1().getHospitalService().getValue();
    }

    /**
     * @return PV1-14 Admission Source. NB Carecast says ZLC8.1 Source of admission
     * @throws HL7Exception if HAPI does
     */
    default String getAdmitSource() throws HL7Exception {
        return getPV1().getAdmitSource().getValue();
    }

    /**
     * @return PV1-18 Patient Type
     * @throws HL7Exception if HAPI does
     */
    default String getPatientType() throws HL7Exception {
        return getPV1().getPatientType().getValue();
    }

    /**
     * @return PV1-19 Visit number
     * @throws HL7Exception if HAPI does
     */
    default String getVisitNumber() throws HL7Exception {
        return getPV1().getVisitNumber().getComponent(0).toString();
    }

    /**
     * We could probably use the HAPI functions to obtain the different components of this result,
     * e.g. for easier conversion to Postgres timestamp format.
     *
     * @return PV1-44.1 admission datetime
     * @throws HL7Exception if HAPI does
     */
    default Instant getAdmissionDateTime() throws HL7Exception {
        return HL7Utils.interpretLocalTime(getPV1().getAdmitDateTime());
    }

    /**
     * @return PV1-45.1 discharge datetime
     * @throws HL7Exception if HAPI does
     */
    default Instant getDischargeDateTime() throws HL7Exception {
        return HL7Utils.interpretLocalTime(getPV1().getDischargeDateTime());
    }
}
