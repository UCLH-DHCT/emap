package uk.ac.ucl.rits.inform.ids;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Representation of the main IDS table. Usually for reading but we can create a test IDS.
 */
@Entity
@Table(name = "tbl_ids_master")
public class IdsMaster {
    @Id
    private int unid;
    private String patientname;
    private String patientmiddlename;
    private String patientsurname;
    private Timestamp dateofbirth;
    private String nhsnumber;
    private String hospitalnumber;
    private String patientclass;
    private String patientlocation;
    private Timestamp admissiondate;
    private Timestamp dischargedate;
    private String messagetype;
    private String senderapplication;
    private String messageidentifier;
    private String messageformat;
    private String messageversion;
    private Timestamp messagedatetime;
    @Column(columnDefinition = "text")
    private String hl7message;
    private Timestamp persistdatetime;

    /**
     * @return the IDS unique ID
     */
    public int getUnid() {
        return unid;
    }

    /**
     * @return patient name
     */
    public String getPatientname() {
        return patientname;
    }

    /**
     * @return patient middle name
     */
    public String getPatientmiddlename() {
        return patientmiddlename;
    }

    /**
     * @return patient surname
     */
    public String getPatientsurname() {
        return patientsurname;
    }

    /**
     * @return patient dob
     */
    public Timestamp getDateofbirth() {
        return dateofbirth;
    }

    /**
     * @return patient NHS number
     */
    public String getNhsnumber() {
        return nhsnumber;
    }

    /**
     * @return patient MRN
     */
    public String getHospitalnumber() {
        return hospitalnumber;
    }

    /**
     * @return patient class
     */
    public String getPatientclass() {
        return patientclass;
    }

    /**
     * @return patient location
     */
    public String getPatientlocation() {
        return patientlocation;
    }

    /**
     * @return patient admission date
     */
    public Timestamp getAdmissiondate() {
        return admissiondate;
    }

    /**
     * @return patient discharge date
     */
    public Timestamp getDischargedate() {
        return dischargedate;
    }

    /**
     * @return message type (aka trigger event)
     */
    public String getMessagetype() {
        return messagetype;
    }

    /**
     * @return sender application
     */
    public String getSenderapplication() {
        return senderapplication;
    }

    /**
     * @return message identifier
     */
    public String getMessageidentifier() {
        return messageidentifier;
    }

    /**
     * @return message format
     */
    public String getMessageformat() {
        return messageformat;
    }

    /**
     * @return message version
     */
    public String getMessageversion() {
        return messageversion;
    }

    /**
     * @return message timestamp
     */
    public Timestamp getMessagedatetime() {
        return messagedatetime;
    }

    /**
     * @return the HL7 message text
     */
    public String getHl7message() {
        return hl7message;
    }

    /**
     * @return timestamp the message was persisted to the IDS
     */
    public Timestamp getPersistdatetime() {
        return persistdatetime;
    }

    public void setHl7message(String hl7message) {
        this.hl7message = hl7message;
    }

    public void setUnid(int unid) {
        this.unid = unid;
    }

    public void setMessagetype(String messagetype) {
        this.messagetype = messagetype;
    }

    public void setHospitalnumber(String hospitalnumber) {
        this.hospitalnumber = hospitalnumber;
    }

    public void setPatientname(String patientname) {
        this.patientname = patientname;
    }

    public void setPatientmiddlename(String patientmiddlename) {
        this.patientmiddlename = patientmiddlename;
    }

    public void setPatientsurname(String patientsurname) {
        this.patientsurname = patientsurname;
    }

    public void setDateofbirth(Timestamp dateofbirth) {
        this.dateofbirth = dateofbirth;
    }

    public void setNhsnumber(String nhsnumber) {
        this.nhsnumber = nhsnumber;
    }

    public void setPatientclass(String patientclass) {
        this.patientclass = patientclass;
    }

    public void setPatientlocation(String patientlocation) {
        this.patientlocation = patientlocation;
    }

    public void setAdmissiondate(Timestamp admissiondate) {
        this.admissiondate = admissiondate;
    }

    public void setDischargedate(Timestamp dischargedate) {
        this.dischargedate = dischargedate;
    }

    public void setSenderapplication(String senderapplication) {
        this.senderapplication = senderapplication;
    }

    public void setMessageidentifier(String messageidentifier) {
        this.messageidentifier = messageidentifier;
    }

    public void setMessageformat(String messageformat) {
        this.messageformat = messageformat;
    }

    public void setMessageversion(String messageversion) {
        this.messageversion = messageversion;
    }

    public void setMessagedatetime(Timestamp messagedatetime) {
        this.messagedatetime = messagedatetime;
    }

    public void setPersistdatetime(Timestamp persistdatetime) {
        this.persistdatetime = persistdatetime;
    }

}
