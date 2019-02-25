// PV1Wrap.java
package uk.ac.ucl.rits.inform;

import ca.uhn.hl7v2.HL7Exception;

import ca.uhn.hl7v2.model.v27.datatype.CWE;
import ca.uhn.hl7v2.model.v27.datatype.CX;
import ca.uhn.hl7v2.model.v27.datatype.DTM;
import ca.uhn.hl7v2.model.v27.datatype.HD;
import ca.uhn.hl7v2.model.v27.datatype.ID;
import ca.uhn.hl7v2.model.v27.datatype.IS;
import ca.uhn.hl7v2.model.v27.datatype.MSG;
import ca.uhn.hl7v2.model.v27.datatype.PL;
import ca.uhn.hl7v2.model.v27.datatype.SAD;
import ca.uhn.hl7v2.model.v27.datatype.XAD;
import ca.uhn.hl7v2.model.v27.datatype.XCN;
import ca.uhn.hl7v2.model.v27.datatype.XPN;
import ca.uhn.hl7v2.model.v27.datatype.XTN;
import ca.uhn.hl7v2.model.AbstractType;

import ca.uhn.hl7v2.model.v27.segment.PV1;

import java.util.Vector;

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
     * Constructor
     * 
     * @param myPV1 PV1 segment, obtained by parsing the message to which this segment relates (msg.getPV1
     */
    public PV1Wrap(PV1 myPV1) {
        _pv1 = myPV1;
    }


    /**
     * 
     * @return PV1-2 patient class
     * @throws HL7Exception
     */
    public String getPatientClass() throws HL7Exception {
        return _pv1.getPatientClass().getComponent(0).toString();
    }


    /**
     * 
     * @return PV1-3.1 Current Ward Code e.g. T06
     * @throws HL7Exception
     */
    public String getCurrentWardCode() throws HL7Exception {
        return _pv1.getAssignedPatientLocation().getPl1_PointOfCare().getComponent(0).toString();
    }


    /**
     * 
     * @return PV1-3.2 Current Room Code e.g. T06A
     * @throws HL7Exception
     */
    public String getCurrentRoomCode() throws HL7Exception {
        return _pv1.getAssignedPatientLocation().getPl2_Room().getComponent(0).toString();
    }


    /**
     * 
     * @return PV1-3.3 Current Bed e.g. T06-32
     * @throws HL7Exception
     */
    public String getCurrentBed() throws HL7Exception {
        return _pv1.getAssignedPatientLocation().getPl3_Bed().getComponent(0).toString();
    }


    /**
     * 
     * @return PV1-4.1 1st repeat (admit priority) e.g. I
     * @throws HL7Exception
     */
    public String getAdmitPriority() throws HL7Exception {
        return _pv1.getAdmissionType().getComponent(0).toString();
    }


    /**
     * 
     * @return PV1-4.1 2nd repeat (admit type) e.g. A
     * @throws HL7Exception
     */
    public String getAdmitType() throws HL7Exception {
        return _pv1.getAdmissionType().getComponent(1).toString();
    }


    /*int reps = pv1.getAttendingDoctorReps();
 System.out.println("THERE ARE " + reps + " ATTENDING DOCTOR(S):");
 for (int i = 0; i < reps; i++) {
     // PV1-7.1 (1st repeat) Consultant code. Consultant for the clinic. Attending doctor GMC Code
     System.out.println("\t(" + i + ") PV1-7.1 consultant code = " + pv1.getAttendingDoctor(i).getPersonIdentifier().toString());
     // PV1-7.2 (1st repeat) Consultant surname Eg. CASSONI
     System.out.println("\t(" + i + ") PV1-7.2 consultant surname = " + pv1.getAttendingDoctor(i).getFamilyName().getSurname().toString());
     // PV1-7.3 (1st repeat) Consultant Fname Eg. M
     System.out.println("\t(" + i + ") PV1-7.3 consultant firstname = " + pv1.getAttendingDoctor(i).getGivenName().toString());
     // PV1-7.4 (1st repeat) Consultant mname Eg. A
     System.out.println("\t(" + i + ") PV1-7.4 Consultant mname = " + pv1.getAttendingDoctor(i).getSecondAndFurtherGivenNamesOrInitialsThereof().toString());
     // PV1-7.6 (1st repeat) Consultant Title Eg. Dr
     System.out.println("\t(" + i + ") PV1-7.6 Consultant Title = " + pv1.getAttendingDoctor(i).getPrefixEgDR().toString());
     // PV1-7.7 (1st repeat) Consultant local code Eg. AC3
     System.out.println("\t(" + i + ") PV1-7.7 Consultant local code = " + pv1.getAttendingDoctor(i)./*getComponent(7).*//*getDegreeEgMD().toString());
 }
    */

    public Vector<Doctor> getAttendingDoctors() throws HL7Exception {

        int reps = _pv1.getAttendingDoctorReps();
        Vector v = new Vector(reps, 1);
        for (int i = 0; i < reps; i++) {
            Doctor dr = new Doctor();
            dr.setConsultantCode(_pv1.getAttendingDoctor(i).getPersonIdentifier().toString());

            v.add(dr);
        }

        return v;
    }



    /**
     * 
     * @return PV1-10	Hospital Service	Specialty eg. 31015
     * @throws HL7Exception
     */
    public String getHospitalService() throws HL7Exception {
        return _pv1.getHospitalService().getComponent(0).toString();
    }

    /**
     * 
     * @return PV1-14	Admission Source. NB Carecast says ZLC8.1	Source of admission
     * @throws HL7Exception
     */
    public String getAdmitSource() throws HL7Exception {
        return _pv1.getAdmitSource().getComponent(0).toString();
    }


    /**
     * 
     * @return PV1-18 Patient Type
     * @throws HL7Exception
     */
    public String getPatientType() throws HL7Exception {
        return _pv1.getPatientType().getComponent(0).toString();
    }


    /**
     * 
     * @return PV1-19 Visit number
     * @throws HL7Exception
     */
    public String getVisitNumber() throws HL7Exception {
        return _pv1.getVisitNumber().getComponent(0).toString();
    }


    /**
     * 
     * @return PV1-44.1 admission datetime - may want to get individual components instead/as well as
     * @throws HL7Exception
     */
    public String getAdmissionDateTime() throws HL7Exception {
        return _pv1.getAdmitDateTime().toString();
    }


    /**
     * 
     * @return PV1-45.1 discharge datetime - may want to get individual components instead/as well as
     * @throws HL7Exception
     */
    public String getDischargeDateTime() throws HL7Exception {
        return _pv1.getDischargeDateTime().toString();
    }

 }
 
 /*
 int reps = pv1.getAttendingDoctorReps();
 System.out.println("THERE ARE " + reps + " ATTENDING DOCTOR(S):");
 for (int i = 0; i < reps; i++) {
     // PV1-7.1 (1st repeat) Consultant code. Consultant for the clinic. Attending doctor GMC Code
     System.out.println("\t(" + i + ") PV1-7.1 consultant code = " + pv1.getAttendingDoctor(i).getPersonIdentifier().toString());
     // PV1-7.2 (1st repeat) Consultant surname Eg. CASSONI
     System.out.println("\t(" + i + ") PV1-7.2 consultant surname = " + pv1.getAttendingDoctor(i).getFamilyName().getSurname().toString());
     // PV1-7.3 (1st repeat) Consultant Fname Eg. M
     System.out.println("\t(" + i + ") PV1-7.3 consultant firstname = " + pv1.getAttendingDoctor(i).getGivenName().toString());
     // PV1-7.4 (1st repeat) Consultant mname Eg. A
     System.out.println("\t(" + i + ") PV1-7.4 Consultant mname = " + pv1.getAttendingDoctor(i).getSecondAndFurtherGivenNamesOrInitialsThereof().toString());
     // PV1-7.6 (1st repeat) Consultant Title Eg. Dr
     System.out.println("\t(" + i + ") PV1-7.6 Consultant Title = " + pv1.getAttendingDoctor(i).getPrefixEgDR().toString());
     // PV1-7.7 (1st repeat) Consultant local code Eg. AC3
     System.out.println("\t(" + i + ") PV1-7.7 Consultant local code = " + pv1.getAttendingDoctor(i)./*getComponent(7).*//*getDegreeEgMD().toString());
 }

 reps = pv1.getReferringDoctorReps();
 System.out.println("THERE ARE " + reps + " REFERRING DOCTOR(S):");
 for (int i = 0; i < reps; i++) {   
     // PV1-8.1 (1st repeat) Consultant Code	(Registered GP user pointer), 2nd Consultant Code (Referring doctor GMC Code)
     System.out.println("\t(" + i + ") PV1-8.1 consultant code = " + pv1.getReferringDoctor(i).getPersonIdentifier().toString());
     // PV1-8.2 (1st repeat) Consultant surname Eg. CASSONI, 2nd Consultant surname Eg. CASSONI
     System.out.println("\t(" + i + ") PV1-8.2 Consultant surname = " + pv1.getReferringDoctor(i).getFamilyName().getSurname().toString());
     // PV1-8.3 (1st repeat) Consultant Fname Eg. M, 2nd Consultant Fname Eg. M
     System.out.println("\t(" + i + ") PV1-8.3 Consultant Fname = " + pv1.getReferringDoctor(i).getGivenName().toString());
     // PV1-8.4 (1st repeat) Consultant mname Eg. A, 2nd Consultant mname Eg. A
     System.out.println("\t(" + i + ") PV1-8.4 Consultant mname = " + pv1.getReferringDoctor(i).getSecondAndFurtherGivenNamesOrInitialsThereof().toString());
     // PV1-8.6 1st repeat Consultant Title Eg. Dr, 2nd Consultant Title	Eg. Dr
     System.out.println("\t(" + i + ") PV1-8.6 Consultant Title = " + pv1.getReferringDoctor(i).getPrefixEgDR().toString());
 }
 */