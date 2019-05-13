package uk.ac.ucl.rits.inform.ids;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
    @Column(columnDefinition="text")
    private String hl7message;
    private Timestamp persistdatetime;

    public int getUnid() {
        return unid;
    }

    public String getPatientname() {
        return patientname;
    }

    public String getPatientmiddlename() {
        return patientmiddlename;
    }

    public String getPatientsurname() {
        return patientsurname;
    }

    public Timestamp getDateofbirth() {
        return dateofbirth;
    }

    public String getNhsnumber() {
        return nhsnumber;
    }

    public String getHospitalnumber() {
        return hospitalnumber;
    }

    public String getPatientclass() {
        return patientclass;
    }

    public String getPatientlocation() {
        return patientlocation;
    }

    public Timestamp getAdmissiondate() {
        return admissiondate;
    }

    public Timestamp getDischargedate() {
        return dischargedate;
    }

    public String getMessagetype() {
        return messagetype;
    }

    public String getSenderapplication() {
        return senderapplication;
    }

    public String getMessageidentifier() {
        return messageidentifier;
    }

    public String getMessageformat() {
        return messageformat;
    }

    public String getMessageversion() {
        return messageversion;
    }

    public Timestamp getMessagedatetime() {
        return messagedatetime;
    }

    public String getHl7message() {
        return hl7message;
    }

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
