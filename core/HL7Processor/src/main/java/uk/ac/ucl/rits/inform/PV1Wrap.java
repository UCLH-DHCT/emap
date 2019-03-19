// PV1Wrap.java
package uk.ac.ucl.rits.inform;

import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Vector;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v27.datatype.DTM;
import ca.uhn.hl7v2.model.v27.segment.PV1;

/**
 * class PV1Wrap
 * 
 * Wrapper around the HAPI parser's PV1 segment object, to make it easier to use.
 * 
 * Reference page: https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/PV1.html
 * 
 */
public class PV1Wrap {

    private PV1 _pv1;

    /**
     * @param myPV1 PV1 segment, obtained by parsing the message to which this segment relates (msg.getPV1
     */
    public PV1Wrap(PV1 myPV1) {
        if (myPV1 == null) {
            throw new IllegalArgumentException();
        }
        _pv1 = myPV1;
    }

    /**
     * If you wish test values to be generated
     */
    public PV1Wrap() {
    }

    /**
     * @return PV1-2 patient class
     * @throws HL7Exception
     */
    public String getPatientClass() throws HL7Exception {
        return _pv1.getPatientClass().getComponent(0).toString();
    }

    /**
     * @return PV1-3.1 Current Ward Code e.g. T06
     * @throws HL7Exception
     */
    public String getCurrentWardCode() throws HL7Exception {
        return _pv1.getAssignedPatientLocation().getPl1_PointOfCare().getComponent(0).toString();
    }

    /**
     * @return PV1-3.2 Current Room Code e.g. T06A
     * @throws HL7Exception
     */
    public String getCurrentRoomCode() throws HL7Exception {
        return _pv1.getAssignedPatientLocation().getPl2_Room().getComponent(0).toString();
    }

    /**
     * @return PV1-3.3 Current Bed e.g. T06-32
     * @throws HL7Exception
     */
    public String getCurrentBed() throws HL7Exception {
        return _pv1.getAssignedPatientLocation().getPl3_Bed().getComponent(0).toString();
    }

    /**
     * @return PV1-4.1 1st repeat (admit priority) e.g. I
     * @throws HL7Exception
     */
    public String getAdmitPriority() throws HL7Exception {
        return _pv1.getAdmissionType().getComponent(0).toString();
    }

    /**
     * @return PV1-4.1 2nd repeat (admit type) e.g. A
     * @throws HL7Exception
     */
    public String getAdmitType() throws HL7Exception {
        return _pv1.getAdmissionType().getComponent(1).toString();
    }

    /**
     * Get the attending doctor(s) PV1-7.1 to PV1-7.7
     * 
     * @return Vector of Doctor objects
     * @throws HL7Exception
     */
    public Vector<Doctor> getAttendingDoctors() throws HL7Exception {

        int reps = _pv1.getAttendingDoctorReps();
        Vector v = new Vector(reps, 1);
        for (int i = 0; i < reps; i++) {
            Doctor dr = new Doctor();
            dr.setConsultantCode(_pv1.getAttendingDoctor(i).getPersonIdentifier().toString()); // PV1-7.1
            dr.setSurname(_pv1.getAttendingDoctor(i).getFamilyName().getSurname().toString()); // PV1-7.2
            dr.setFirstname(_pv1.getAttendingDoctor(i).getGivenName().toString()); // PV1-7.3
            dr.setMiddlenameOrInitial(_pv1.getAttendingDoctor(i).getSecondAndFurtherGivenNamesOrInitialsThereof().toString()); // PV1-7.4
            dr.setTitle(_pv1.getAttendingDoctor(i).getPrefixEgDR().toString()); // PV1-7.6
            dr.setLocalCode(_pv1.getAttendingDoctor(i).getComponent(7)/*getDegreeEgMD()*/.toString()); // PV1-7.7

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
    public Vector<Doctor> getReferringDoctors() throws HL7Exception {

        int reps = _pv1.getReferringDoctorReps();
        Vector v = new Vector(reps, 1);
        for (int i = 0; i < reps; i++) {
            Doctor dr = new Doctor();
            dr.setConsultantCode(_pv1.getReferringDoctor(i).getPersonIdentifier().toString()); // PV1-8.1
            dr.setSurname(_pv1.getReferringDoctor(i).getFamilyName().getSurname().toString()); // PV1-8.2
            dr.setFirstname(_pv1.getReferringDoctor(i).getGivenName().toString()); // PV1-8.3
            dr.setMiddlenameOrInitial(_pv1.getReferringDoctor(i).getSecondAndFurtherGivenNamesOrInitialsThereof().toString()); // PV1-8.4
            dr.setTitle(_pv1.getReferringDoctor(i).getPrefixEgDR().toString()); // PV1-8.6
            //dr.setLocalCode(_pv1.); // PV1-8.7

            v.add(dr);
        }

        return v;
    }

    /**
     * @return PV1-10	Hospital Service	Specialty eg. 31015
     * @throws HL7Exception
     */
    public String getHospitalService() throws HL7Exception {
        return _pv1.getHospitalService().getComponent(0).toString();
    }

    /**
     * @return PV1-14	Admission Source. NB Carecast says ZLC8.1	Source of admission
     * @throws HL7Exception
     */
    public String getAdmitSource() throws HL7Exception {
        return _pv1.getAdmitSource().getComponent(0).toString();
    }

    /**
     * @return PV1-18 Patient Type
     * @throws HL7Exception
     */
    public String getPatientType() throws HL7Exception {
        return _pv1.getPatientType().getComponent(0).toString();
    }

    /**
     * @return PV1-19 Visit number
     * @throws HL7Exception
     */
    public String getVisitNumber() throws HL7Exception {
        return _pv1.getVisitNumber().getComponent(0).toString();
    }

    /**
     * We could probably use the HAPI functions to obtain the different components of this result,
     * e.g. for easier conversion to Postgres timestamp format.
     * 
     * @return PV1-44.1 admission datetime 
     * @throws HL7Exception
     */
    public Instant getAdmissionDateTime() throws HL7Exception {
        if (_pv1 == null) {
            // this is not a good way of doing test data
            return Instant.parse("2014-05-06T07:08:09Z");
        }
        return interpretLocalTime(_pv1.getAdmitDateTime());
    }

    /**
     * HL7 messages may or may not specify a timezone. Our assumption is
     * that we will interpret unspecified ones as "local" time, which means local time
     * for the hospital, NOT local time for the computer this code is running on.
     *
     * @param hl7DTM the hl7 DTM object as it comes from the message
     * @return an Instant representing this same point in time
     * @throws DataTypeException
     */
    public Instant interpretLocalTime(DTM hl7DTM) throws DataTypeException {
        Calendar valueAsCal = hl7DTM.getValueAsCalendar();
        // BUG: If no timezone/offset is specified in the HL7 message,
        // the Calendar object still comes out with a timezone of Europe/London,
        // or presumably whatever the tz is on the computer this is running on.
        // We need to be able to tell between no TZ specified and an explicit Europe/London.
        // Is this somewhere in the HAPI config?
        // Chances are no HL7 messages we ever see will specify it, though...
        TimeZone before = valueAsCal.getTimeZone();
        // The hospital will always be in London, however this code could
        // theoretically run anywhere. In fact, CircleCI containers seem to be set on UTC.
        // Therefore forcibly interpret as this timezone (this should be in config).
        // There's the possibility that different equipment in the hospital will be in
        // different timezones (or will be manually updated at different times), but we'll
        // deal with that when we come to it.
        valueAsCal.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        TimeZone after = valueAsCal.getTimeZone();
        Instant result = valueAsCal.toInstant();
        //System.out.println("before: " + hl7DTM + "|" + before + ", after: " + result + "|" + after);
        return result;
    }
    
    /**
     * We could probably use the HAPI functions to obtain the different components of this result,
     * e.g. for easier conversion to Postgres timestamp format.
     * 
     * @return PV1-45.1 discharge datetime
     * @throws HL7Exception
     */
    public String getDischargeDateTime() throws HL7Exception {
        return _pv1.getDischargeDateTime().toString();
    }

}
