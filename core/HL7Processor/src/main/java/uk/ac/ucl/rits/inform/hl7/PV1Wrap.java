// PV1Wrap.java
package uk.ac.ucl.rits.inform.hl7;

import java.time.Instant;
import java.util.Vector;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v27.segment.PV1;

/**
 * class PV1Wrap
 * 
 * Wrapper around the HAPI parser's PV1 segment object, to make it easier to use.
 * 
 * Reference page: https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/PV1.html
 * 
 */
public interface PV1Wrap {
    PV1 getPV1();

    /**
     * @return Is this a test object which should generate synthetic data instead
     * of using the HL7 message data?
     */
    boolean isTest();

    default boolean pv1SegmentExists() {
        return getPV1() != null;
    }

    /**
     * @return PV1-2 patient class
     * @throws HL7Exception
     */
    default String getPatientClass() throws HL7Exception {
        if (isTest()) {
            // Need to come up with a better way of generating test data
            return "??";
        }
        if (!pv1SegmentExists()) {
            return null;
        }
        return getPV1().getPatientClass().getComponent(0).toString();
    }

    /**
     * @return PV1-3.1 Current Ward Code e.g. T06
     * @throws HL7Exception
     */
    default String getCurrentWardCode() throws HL7Exception {
        if (isTest()) {
            // Need to come up with a better way of generating test data
            return "Test poc location";
        }
        return getPV1().getAssignedPatientLocation().getPl1_PointOfCare().getComponent(0).toString();
    }

    /**
     * @return PV1-3.2 Current Room Code e.g. T06A
     * @throws HL7Exception
     */
    default String getCurrentRoomCode() throws HL7Exception {
        if (isTest()) {
            // Need to come up with a better way of generating test data
            return "Test room location";
        }
        return getPV1().getAssignedPatientLocation().getPl2_Room().getComponent(0).toString();
    }

    /**
     * @return PV1-3.3 Current Bed e.g. T06-32
     * @throws HL7Exception
     */
    default String getCurrentBed() throws HL7Exception {
        if (isTest()) {
            // Need to come up with a better way of generating test data
            return "Test bed location";
        }
        return getPV1().getAssignedPatientLocation().getPl3_Bed().getComponent(0).toString();
    }

    /**
     * Get all the location components concatenated together.
     * We may need to canonicalise these to remove duplicate info.
     * @return
     * @throws HL7Exception
     */
    default String getFullLocationString() throws HL7Exception {
        if (!pv1SegmentExists()) {
            return null;
        }
        return String.join("^", getCurrentWardCode(), getCurrentRoomCode(), getCurrentBed());
    }

    /**
     * @return PV1-4.1 1st repeat (admit priority) e.g. I
     * @throws HL7Exception
     */
    default String getAdmitPriority() throws HL7Exception {
        return getPV1().getAdmissionType().getComponent(0).toString();
    }

    /**
     * @return PV1-4.1 2nd repeat (admit type) e.g. A
     * @throws HL7Exception
     */
    default String getAdmitType() throws HL7Exception {
        return getPV1().getAdmissionType().getComponent(1).toString();
    }

    /**
     * Get the attending doctor(s) PV1-7.1 to PV1-7.7
     * 
     * @return Vector of Doctor objects
     * @throws HL7Exception
     */
    default Vector<Doctor> getAttendingDoctors() throws HL7Exception {
        Vector<Doctor> v = new Vector<Doctor>();
        if (isTest()) {
            return v;
        }
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
     * @throws HL7Exception
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
     * @return PV1-10	Hospital Service	Specialty eg. 31015
     * @throws HL7Exception
     */
    default String getHospitalService() throws HL7Exception {
        return getPV1().getHospitalService().getComponent(0).toString();
    }

    /**
     * @return PV1-14	Admission Source. NB Carecast says ZLC8.1	Source of admission
     * @throws HL7Exception
     */
    default String getAdmitSource() throws HL7Exception {
        return getPV1().getAdmitSource().getComponent(0).toString();
    }

    /**
     * @return PV1-18 Patient Type
     * @throws HL7Exception
     */
    default String getPatientType() throws HL7Exception {
        return getPV1().getPatientType().getComponent(0).toString();
    }

    /**
     * @return PV1-19 Visit number
     * @throws HL7Exception
     */
    default String getVisitNumber() throws HL7Exception {
        if (isTest()) {
            // Need to come up with a better way of generating test data
            return HL7Random.randomNumericSeeded(System.identityHashCode(this), 8);
        }
        return getPV1().getVisitNumber().getComponent(0).toString();
    }

    /**
     * We could probably use the HAPI functions to obtain the different components of this result,
     * e.g. for easier conversion to Postgres timestamp format.
     * 
     * @return PV1-44.1 admission datetime 
     * @throws HL7Exception
     */
    default Instant getAdmissionDateTime() throws HL7Exception {
        if (isTest()) {
            // this is not a good way of doing test data
            return Instant.parse("2014-05-06T07:08:09Z");
        }
        return HL7Utils.interpretLocalTime(getPV1().getAdmitDateTime());
    }

    
    /**
     * @return PV1-45.1 discharge datetime
     * @throws HL7Exception
     */
    default Instant getDischargeDateTime() throws HL7Exception {
        if (isTest()) {
            // this is not a good way of doing test data
            return Instant.parse("2014-05-06T12:34:56Z");
        }
        return HL7Utils.interpretLocalTime(getPV1().getDischargeDateTime());
    }

}
