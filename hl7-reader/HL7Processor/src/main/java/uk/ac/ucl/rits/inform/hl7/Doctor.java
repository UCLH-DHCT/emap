package uk.ac.ucl.rits.inform.hl7;

import ca.uhn.hl7v2.model.v27.datatype.XCN;

/**
 * Store information for GPs and consultants etc - fields PV1-7 and PV1-8
 * This is actually a wrapper around an XCN, so it should probably be
 * generalised to all uses of XCN.
 */
public class Doctor {

    private String consultantCode, surname, firstname, middlename, title;

    public Doctor(XCN xcnDoc) {
        consultantCode = xcnDoc.getPersonIdentifier().toString(); // PV1-7.1
        surname = xcnDoc.getFamilyName().getSurname().toString(); // PV1-7.2
        firstname = xcnDoc.getGivenName().toString(); // PV1-7.3
        middlename = xcnDoc.getSecondAndFurtherGivenNamesOrInitialsThereof().toString(); // PV1-7.4
        title = xcnDoc.getPrefixEgDR().toString(); // PV1-7.6
    }

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

    /**
     * @return PV1-7.2 and PV-8.2 Consultant surname Eg. CASSONI
     */
    public String getSurname() {
        return surname;
    }

    /**
     * @return PV1-7.3 or PV1-8.3 Consultant firstname/initial
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @return PV1-7.4/PV1-8.4 consultant middle name or initial
     */
    public String getMiddlenameOrInitial() {
        return middlename;
    }

    /**
     * @return PV1-7.6/PV1-8.6 consultant title e.g. Dr
     */
    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "Doctor [consultantCode=" + consultantCode + ", surname=" + surname + ", firstname=" + firstname
                + ", middlename=" + middlename + ", title=" + title + "]";
    }
}

