// Doctor.java
package uk.ac.ucl.rits.inform.hl7;

/**
 * Doctor.java
 * 
 * Store information for GPs and consultants etc - fields PV1-7 and PV1-8
 */

public class Doctor {

    private String consultantCode, surname, firstname, middlename, title, localCode;

    /**
     * PV1-7.1 Consultant code or attending doctor GMC code.
     * PV1-8.1 Registered GP user pointer, Referring doctor GMC Code
     * NB this is Carecast usage.
     *
     * @return
     */
    public String getConsultantCode() {
        return consultantCode;
    }

    public void setConsultantCode(String code) {
        consultantCode = code;
    }

    /**
     * 
     * @return PV1-7.2 and PV-8.2 Consultant surname Eg. CASSONI
     */
    public String getSurname() {
        return surname;
    }

    public void setSurname(String sname) {
        surname = sname;
    }

    /**
     * 
     * @return PV1-7.3 or PV1-8.3 Consultant firstname/initial
     */
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String fname) {
        firstname = fname;
    }

    /**
     * 
     * @return PV1-7.4/PV1-8.4 consultant middle name or initial
     */
    public String getMiddlenameOrInitial() {
        return middlename;
    }

    public void setMiddlenameOrInitial(String mname) {
        middlename = mname;
    }


    /**
     * 
     * @return PV1-7.6/PV1-8.6 consultant title e.g. Dr
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String atitle) {
        title = atitle;
    }

  
    /**
     * 
     * Not necessarily used for PV1-8.7. Carecast appears to use this field for local code
     * but usually this is the degree or other qualification of the doctor.
     * 
     * @return PV1-7.7 local code e.g. AC3.
     */
    public String getLocalCode() {
        return localCode;
    }

    public void setLocalCode(String code) {
        localCode = code;
    }
}

