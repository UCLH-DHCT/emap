// Engine.java
//
// Given an HL7 message, take appropriate action based on what that message is.
//
// Matthew Gillman, UCL, 29th August 2018

package uk.ac.ucl.rits.inform;

import java.lang.String;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
//import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.HL7Exception;

import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.model.v27.segment.PID;
import ca.uhn.hl7v2.model.v27.datatype.XPN;

import ca.uhn.hl7v2.model.v27.message.*; //ADT_A01;

// HL7 2.2 only
//import ca.uhn.hl7v2.model.v22.message.ADT_A01;
//import ca.uhn.hl7v2.model.v22.message.ADT_A02; // was v25
////import ca.uhn.hl7v2.model.v22.segment.PID;
////import ca.uhn.hl7v2.model.v22.datatype.PN;

////import ca.uhn.hl7v2.model.v231.datatype.TS;

// etc.

public class Engine {

    public Engine(HapiContext c, PipeParser p) {
        context = c;
        parser = p;
        System.out.println("Engine created successfully");
    }

    public void processMessage(Message msg) {
        String ver = msg.getVersion();
        System.out.println("Engine: version is " + ver); // now will all be the same (2.7 in our case)

        ///if (ver.equals("2.2")) {

        System.out.println("HEY! Got " + ver);
        
        ////////////////////////////////////////////////////////////////////////////////////
        // From HAPI FAQ: https://hapifhir.github.io/hapi-hl7v2/hapi-faq.html 
        // Why are some message classes missing? For example, I can find the class ADT_A01, but not the class ADT_A04.
        // HL7 defines that some message triggers reuse the same structure. So, for example, 
        // the ADT^A04 message has the exact same structure as an ADT^A01 message. Therefore,
        // when an ADT^A04 message is parsed, or when you want to create one, you will actually
        // use the ADT_A01 message class, but the "triggerEvent" property of MSH-9 will be set to A04.
        ////////////////////////////////////////////////////////////////////////////////////
        // The full list is documented in 2.7.properties file:
        // A01 also handles A04, A08, A13
        // A05 also handles A14, A28, A31
        // A06 handles A07
        // A09 handles A10, A11
        // A21 handles A22, A23, A25, A26, A27, A29, A32, A33
        // ADT_A39 handles A40, A41, A42
        // ADT_A43 handles A49
        // ADT_A44 handles A47
        // ADT_A50 handles A51
        // ADT_A52 handles A53
        // ADT_A54 handles A55
        // ADT_A61 handles A62
    
        // We also refer to Functional Specification - Example CareCast HL7 ADT messages doc from Atos
        // as that has UCLH-specific things (e.g. adding death info to v2.2 messages)

        // All 2.2 ASDT messages A01 to A37 have PID segment EXCEPT A20 (bed status update)
        // Maybe sometimes some of the PID info will be unknown??

        // NB https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/ExampleSuperStructures.html
        // I don't think we can do it that way as we have already initialised the CanonicalModelClassFactory
        // ADT A01: Admit a Patient
        if (/*msg instanceof ca.uhn.hl7v2.model.v22.message.ADT_A01 
            || msg instanceof ca.uhn.hl7v2.model.v23.message.ADT_A01
            || msg instanceof ca.uhn.hl7v2.model.v24.message.ADT_A01
            || */ msg instanceof /*ca.uhn.hl7v2.model.v27.message.*/ADT_A01
            // etc  ca.uhn.hl7v2.model.v27.segment.MSH msh = msg.getMSH();    
        ) {
            System.out.println("Got an ADT_A01");
            // AO1 message type can be used to represent other message types
            

            try {
                /*
                ca.uhn.hl7v2.model.v27.message.ADT_A01 adt_01 = (ca.uhn.hl7v2.model.v27.message.ADT_A01) parser.parse(msg.encode());
                ca.uhn.hl7v2.model.v27.segment.MSH msh = adt_01.getMSH();
                //ca.uhn.hl7v2.model.v27.datatype.MSG MSG =  msh.getMsh9_MessageType();
                String msgTrigger = msh.getMessageType().getTriggerEvent().getValue();
                System.out.println("Trigger is " + msgTrigger);
         
                if (msgTrigger.equals("A01") || msgTrigger.equals("A04")) {
                    process_ADT_01_et_al(msg, msgTrigger);
                }
                else {
                    System.out.println("Method not yet implemented");
                }*/
                process_ADT_01_et_al(msg);
            }
            catch (HL7Exception e)  {
                e.printStackTrace();
            }
            //ca.uhn.hl7v2.model.v22.segment.PID pid = msg.getPID();
            
        } 
        
        // ADT A02: Transfer a patient
        else if (msg instanceof /*ADT_A02*/ /*ca.uhn.hl7v2.model.v27.message.*/ADT_A02) {
            System.out.println("Got an ADT_A02!!"); //processAdtA02((ADT_A02) msg);

        }

        // ADT A03: Discharge a patient

        // ADT A04: Register a Patient
        else if (2==1/*msg instanceof ca.uhn.hl7v2.model.v27.message.ADT_A04*/) {
            System.out.println("Got an ADT_A04!!");

        }
    ///}

        else System.out.println("Another message type");

    }

    // process_XXX_YY() functions
    //
    // Try and abstract out common functionality across HL7 versions as much as possible.
    // Unfortunately we cannot use dynamic binding; e.g. the AbstractMessage class and Message
    // interface do not have the function getPID(). So we cannot say things like:
    //      ca.uhn.hl7v2.model.AbstractMessage adt_01;
    //      adt_01 = (ca.uhn.hl7v2.model.v22.message.ADT_A01) msg;
    //      PN patientName = adt_01.getPID().getPatientName();
    // as we will get a "cannot find symbol" error. Instead, the way to handle multiple HL7 versions
    // is by using the parser:
    // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
    // and CanonicalModelClassFactory set to latest version we wish to support

    // A01 also handles A04, A08, A13
    //
    // ADT/ACK - Admit/Visit Notification (Event A01)
    // "An A01 event is intended to be used for "Admitted" patients only. An A01 event is sent as a result of a patient
    // undergoing the admission process which assigns the patient to a bed. It signals the beginning of a patient's stay
    // in a healthcare facility.
    //
    // ADT^A04 - Register a patient
    // From 2.7 standard: "An A04 event signals that the patient has arrived or checked in as a one-time,
    // or recurring outpatient, and is not assigned to a bed. One example might be its use to signal the 
    // beginning of a visit to the Emergency Room (= Casualty, etc.). Note that some systems refer to these
    // events as outpatient registrations or emergency admissions. PV1-44 - Admit Date/Time is used for
    // the visit start date/time."
    //
    // ADT/ACK - Update Patient Information (Event A08)
    // "This trigger event is used when any patient information has changed but when no other trigger event 
    // has occurred. For example, an A08 event can be used to notify the receiving systems of a change of address
    // or a name change. We strongly recommend that the A08 transaction be used to update fields that are not
    // updated by any of the other trigger events. If there are specific trigger events for this update, these
    // trigger events should be used. For example, if a patient's address and location are to be changed, then
    // an A08 is used to change the patient address and the appropriate patient location trigger event is used to
    // change the patient location. The A08 event can include information specific to an episode of care, but it 
    // can also be used for demographic information only"
    //
    // Cancel Discharge / End Visit (Event A13)
    // "The A13 event is sent when an A03 (discharge/end visit) event is cancelled, either because of erroneous
    // entry of the A03 event or because of a decision not to discharge or end the visit of the patient after all.
    // PV1-3 - Assigned Patient Location should reflect the location of the patient after the cancellation has been
    // processed. Note that this location may be different from the patient's location prior to the erroneous discharge.
    // Prior Location could be used to show the location of the patient prior to the erroneous discharge.
    // The fields included when this message is sent should be the fields pertinent to communicate this event.
    // When other important fields change, it is recommended that the A08 (update patient information) event be used
    // in addition."
    //
    // NB v2.2 of standard only goes up to PID-27 but Atos UCLH doc has upto PID 30 (death)

    private void process_ADT_01_et_al(Message msg) throws HL7Exception {

        // Want data for:
        // name ??? probably not
        // hospital number ?? possibly map to person.person_source_value (can be encrypted)
        // PERSON - year_of_birth (required), month_of_birth, day_of_birth, birth_datetime
        // VISIT_OCCURRENCE - visit_start_date (required), visit_start_datetime
        // timestamp?
        // NB PID-2 patient ID withdrawn 2.7, now PID-3 patient_ID list ?? PID-7 date and time of birth, PID-8 sex, PID-29 and 30 death
        //ca.uhn.hl7v2.model.v27.segment.PID pid = adt_04.getPID();
        //TS birth = pid.getPid7_DateTimeOfBirth();

        // The parser parses the v2.x message to a "v27" structure
        //ca.uhn.hl7v2.model.v27.message.ADT_ORU_R01 msg = (ca.uhn.hl7v2.model.v25.message.ORU_R01) parser.parse(v23message);
        ca.uhn.hl7v2.model.v27.message.ADT_A01 adt_01 = (ca.uhn.hl7v2.model.v27.message.ADT_A01) parser.parse(msg.encode());
        

        //ca.uhn.hl7v2.model.v22.message.ADT_A01 a = (ca.uhn.hl7v2.model.v22.message.ADT_A01) msg;
        //PN patientName = adt_01.getPID().getPatientName();
        /*ca.uhn.hl7v2.model.v27.segment.*/PID pid = adt_01.getPID();
        /*ca.uhn.hl7v2.model.v27.datatype.*/XPN xpn[] = pid.getPatientName(); 
        //System.out.println("++++++ patientName is " + xpn[0].toString() + "++:-)+++++");

        // Gives output like:
        // ++++++ patientName is XPN[PECK^Jacqueline^Francis^^MISS]++:-)+++++
        // System.out.println("++++++ patientName is " + xpn[0] + "++:-)+++++");
        
        String patient_name;
        if (xpn != null && xpn.length > 0) {
            patient_name = xpn[0].toString(); // e.g. "XPN[PECK^Jacqueline^Francis^^MISS]" - need to parse
            // can also use this instead  - gives same answer: patient_name = pid.getPatientName(0).toString();

        }
        else patient_name = "Jane Doe";
        // NB public ID getIdentityUnknownIndicator()
        // Returns PID-31: "Identity Unknown Indicator" - creates it if necessary - maybe should use instead - but not present in e.g. v2.2

        System.out.println("++++++ patientName is " + patient_name + "++:-)+++++");

        // Other data
        String sex = pid.getAdministrativeSex().toString();  // M or F - comes out as CWE[F] etc
        String birth_date_time = pid.getDateTimeOfBirth().toString(); // may be null? e.g. 193508040000 or 19610615
        //String patient_id = pid.getPatientID().toString(); // in our Atos examples this is sometimes null. Is that possible?
        // Also patient_id sometimes a non-numeric string e.g. ******** so we should probably check it's an int (or long).
        // NB PID-2 is external patient id and PID-3 is internal patient ID (accroding to v2.2)
        // PID-3 uses getPatientIdentifierList() nb what if >1 identifier?! Due to merging?
        // v2.7 standard says PID-2 removed as of 2.7 so should use PID-3
        String patient_id = pid.getPatientIdentifierList()[0].toString(); // e.g. CX[PATID1234^5^M11^ADT1^MR^GOOD HEALTH HOSPITAL]
        System.out.println("Data: " + sex + "," + birth_date_time + "," + patient_id); 



        /*ca.uhn.hl7v2.model.v27.segment.*/MSH msh = adt_01.getMSH();
        //ca.uhn.hl7v2.model.v27.datatype.MSG MSG =  msh.getMsh9_MessageType();
        String msgTrigger = msh.getMessageType().getTriggerEvent().getValue();
        System.out.println("Trigger is " + msgTrigger);

        if (msgTrigger.equals("A01")) { // Admit - assign to a bed
            
        }
        else if (msgTrigger.equals("A04")) { // Register - not assigned to a bed.
            
        }
        else if (msgTrigger.equals("A08")) { // Update patient information
            
        }
        else if (msgTrigger.equals("A13")) { // Cancel discharge
            
        }
        else {
            System.out.println("Error: message cannot be handled by A01 method");
        }


    }

    private void writeToDatabase() {

    }

    private HapiContext context;
    private PipeParser parser;

}