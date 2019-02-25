// Doctor.java
package uk.ac.ucl.rits.inform;

/**
 * Doctor.java
 * 
 * Store information for GPs and consultants etc - fields PV1-7 and PV1-8
 */

public class Doctor {

    private String consultantCode, surname, firstname, middlename, title, localCode;

    // PV1-7.1 Consultant code or attending doctor GMC code.
    // PV1-8.1 Registered GP user pointer, Referring doctor GMC Code
    // NB this is Carecast usage.
    public String getConsultantCode() {
        return consultantCode;
    }

    public void setConsultantCode(String code) {
        consultantCode = code;
    }

    // etc.

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