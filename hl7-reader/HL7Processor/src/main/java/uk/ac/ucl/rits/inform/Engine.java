package uk.ac.ucl.rits.inform;

import java.lang.String;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.model.v27.segment.PID;
import ca.uhn.hl7v2.model.v27.datatype.XPN;
import ca.uhn.hl7v2.model.v27.datatype.CWE;
//import ca.uhn.hl7v2.model.v27.datatype.PN;
import ca.uhn.hl7v2.model.v27.message.*; //ADT_A01;

// HL7 2.2 only
//import ca.uhn.hl7v2.model.v22.message.ADT_A01;
//import ca.uhn.hl7v2.model.v22.message.ADT_A02; // was v25
////import ca.uhn.hl7v2.model.v22.segment.PID;
////import ca.uhn.hl7v2.model.v22.datatype.PN;

////import ca.uhn.hl7v2.model.v231.datatype.TS;

// etc.

/**
 * Engine.java
 * 
 * Given an HL7 message, take appropriate action based on what that message is.
 * <p>
 * Matthew Gillman, UCL, 29th August 2018
 * <p>
 * Note we are changing all messages to HL7 version 2.7.
 * 
 */
public class Engine {

    /**
     * Constructor
     * 
     * @param c A HapiContext
     * @param p A Hapi PipeParser
     */
    public Engine(HapiContext c, PipeParser p) {
        context = c;
        parser = p;
        System.out.println("Engine created successfully");
    }

    /**
     * Process a message based on its type (e.g. ADT_01)
     * 
     * See https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
     * for how we handkle multiple versions of HL7.
     * 
     * Also:
     * From HAPI FAQ: https://hapifhir.github.io/hapi-hl7v2/hapi-faq.html 
     * Q. Why are some message classes missing? For example, I can find
     * the class ADT_A01, but not the class ADT_A04.
     * A. HL7 defines that some message triggers reuse the same structure. So, for example,
     * the ADT^A04 message has the exact same structure as an ADT^A01 message. Therefore,
     * when an ADT^A04 message is parsed, or when you want to create one, you will actually
     * use the ADT_A01 message class, but the "triggerEvent" property of MSH-9 will be set to A04.
     * <p>
     * The full list is documented in 2.7.properties file:
     * A01 also handles A04, A08, A13
     * A05 also handles A14, A28, A31
     * A06 handles A07
     * A09 handles A10, A11
     * A21 handles A22, A23, A25, A26, A27, A29, A32, A33
     * ADT_A39 handles A40, A41, A42
     * ADT_A43 handles A49
     * ADT_A44 handles A47
     * ADT_A50 handles A51
     * ADT_A52 handles A53
     * ADT_A54 handles A55
     * ADT_A61 handles A62
     * 
     * @param msg The message to process.
     */
    public void processMessage(Message msg) {
        String ver = msg.getVersion();
        System.out.println("Engine: version is " + ver); // now will all be the same (2.7 in our case)

        System.out.println("HEY! Got " + ver);
     
        // We also refer to Functional Specification - Example CareCast HL7 ADT messages doc from Atos
        // as that has UCLH-specific things (e.g. adding death info to v2.2 messages)

        // All 2.2 ADT messages A01 to A37 have PID segment EXCEPT A20 (bed status update)
        // Maybe sometimes some of the PID info will be unknown??

        // NB https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/ExampleSuperStructures.html
        // I don't think we can do it that way as we have already initialised the CanonicalModelClassFactory
        // ADT A01: Admit a Patient
        if (msg instanceof ADT_A01)
        {
            System.out.println("Got an ADT_A01");
            // AO1 message type can be used to represent other message types 

            try {
                //pretty_print(msg);
                process_ADT_01_et_al(msg);
            }
            catch (HL7Exception e)  {
                e.printStackTrace();
            }
            
        } 
        
        // ADT A02: Transfer a patient
        else if (msg instanceof ADT_A02) {
            System.out.println("Got an ADT_A02!!"); //processAdtA02((ADT_A02) msg);

        }

        // ADT A03: Discharge a patient

        // ADT A04: Register a Patient
        else if (2==1/*msg instanceof ca.uhn.hl7v2.model.v27.message.ADT_A04*/) {
            System.out.println("Got an ADT_A04!!");

        }

        else System.out.println("Another message type");

    }


     /**
     * Utility function to parse a message and show what we can get from it.
     * 
     * @param p A Hapi PipeParser
     */
    public void pretty_print(String hl7String) {



        /*hl7String = "MSH|^~\\&|HIS|RIH|EKG|EKG|199904140038||ADT^A01||P|2.2\r"
                + "PID|0001|00009874|00001122|A00977|SMITH^JOHN^M|MOM|19581119|F|NOTREAL^LINDA^M|C|564 SPRING ST^^NEEDHAM^MA^02494^US|0002|(818)565-1551|(425)828-3344|E|S|C|0000444444|252-00-4414||||SA|||SA||||NONE|V1|0001|I|D.ER^50A^M110^01|ER|P00055|11B^M011^02|070615^BATMAN^GEORGE^L|555888^NOTREAL^BOB^K^DR^MD|777889^NOTREAL^SAM^T^DR^MD^PHD|ER|D.WT^1A^M010^01|||ER|AMB|02|070615^NOTREAL^BILL^L|ER|000001916994|D||||||||||||||||GDD|WA|NORM|02|O|02|E.IN^02D^M090^01|E.IN^01D^M080^01|199904072124|199904101200|199904101200||||5555112333|||666097^NOTREAL^MANNY^P\r"
                + "NK1|0222555|NOTREAL^JAMES^R|FA|STREET^OTHER STREET^CITY^ST^55566|(222)111-3333|(888)999-0000|||||||ORGANIZATION\r"
          + "PV1|0001|I|D.ER^1F^M950^01|ER|P000998|11B^M011^02|070615^BATMAN^GEORGE^L|555888^OKNEL^BOB^K^DR^MD|777889^NOTREAL^SAM^T^DR^MD^PHD|ER|D.WT^1A^M010^01|||ER|AMB|02|070615^VOICE^BILL^L|ER|000001916994|D||||||||||||||||GDD|WA|NORM|02|O|02|E.IN^02D^M090^01|E.IN^01D^M080^01|199904072124|199904101200|||||5555112333|||666097^DNOTREAL^MANNY^P\r"
         + "PV2|||0112^TESTING|55555^PATIENT IS NORMAL|NONE|||19990225|19990226|1|1|TESTING|555888^NOTREAL^BOB^K^DR^MD||||||||||PROD^003^099|02|ER||NONE|19990225|19990223|19990316|NONE\r"
         + "AL1||SEV|001^POLLEN\r"
        + "GT1||0222PL|NOTREAL^BOB^B||STREET^OTHER STREET^CITY^ST^77787|(444)999-3333|(222)777-5555||||MO|111-33-5555||||NOTREAL GILL N|STREET^OTHER STREET^CITY^ST^99999|(111)222-3333\r"
          + "IN1||022254P|4558PD|BLUE CROSS|STREET^OTHER STREET^CITY^ST^00990||(333)333-6666||221K|LENIX|||19980515|19990515|||PATIENT01 TEST D||||||||||||||||||02LL|022LP554";

        */



        Message msg = null;
        try {
            msg = parser.parse(hl7String);

        }
        catch (HL7Exception e) {
            e.printStackTrace();
            return;
        }

        if (msg instanceof ADT_A01) {
            try {
                process_ADT_01_et_al(msg);
                //System.exit(1);
            }
            catch (HL7Exception e) {
                e.printStackTrace();
                return;
            }

        }
        //MSH msh = hapiMsg.getMSH();
      
    } 



    /**
     * process_XXX_YY() functions
     * 
     * Try and abstract out common functionality across HL7 versions as much as possible.
     * Unfortunately we cannot use dynamic binding; e.g. the AbstractMessage class and Message
     * interface do not have the function getPID(). So we cannot say things like:
     * <pre>
     * ca.uhn.hl7v2.model.AbstractMessage adt_01;
     * adt_01 = (ca.uhn.hl7v2.model.v22.message.ADT_A01) msg;
     * PN patientName = adt_01.getPID().getPatientName();
     * </pre>
     * as we will get a "cannot find symbol" error. Instead, the way to handle multiple HL7 versions
     * is by using the parser:
     * https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
     * and CanonicalModelClassFactory set to latest version we wish to support.
     * 
     * @param msg A HAPI HL7 message object.
     * @throws HL7Exception
     */


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
    // (for A28 and A31) also A34, A01, A02, A03, A13, A08, 
    private void process_ADT_01_et_al(Message msg) throws HL7Exception {

        // Want data for:
        // hospital number ?? possibly map to person.person_source_value (can be encrypted)
        // PERSON - year_of_birth (required), month_of_birth, day_of_birth, birth_datetime
        // VISIT_OCCURRENCE - visit_start_date (required), visit_start_datetime
        // timestamp?
        // NB PID-2 patient ID withdrawn 2.7, now PID-3 patient_ID list ?? PID-7 date and time of birth, PID-8 sex, PID-29 and 30 death
     

        // The parser parses the v2.x message to a "v27" structure
        //ca.uhn.hl7v2.model.v27.message.ADT_ORU_R01 msg = (ca.uhn.hl7v2.model.v25.message.ORU_R01) parser.parse(v23message);
        ca.uhn.hl7v2.model.v27.message.ADT_A01 adt_01 = (ca.uhn.hl7v2.model.v27.message.ADT_A01) parser.parse(msg.encode());
        
        // According to the standard (2.7), the following segments should be present:
        // MSH, EVN, PID, PV1, plus many more optional ones.
        // Carecast: MSH, EVN, PID, [PD1], [NK1], PV1, [PV2], {[OBX]}, [ZLW], [ZUK], [ZLC]
        // i.e. Carecast has a subset of the possible segments the standard covers.

        // Obtain items which should be present (according to Carecast)
        // Unmapped items are not included here.

        // 1. MSH (Message Header) - mostly don't appear to be useful
        // MSH-1 Field Separator
        // MSH-2 Encoding Characters
        // MSH-3 Sending Application (“CARECAST”)
        // MSH-4 Sending Facility (“UCLH”)
        // MSH-5 Receiving Application (“Receiving system”)
        // MSH-7 Date/Time Of Message YYYYMMDDHHMM
        // MSH-9.1	Message Type (ADT)
        // MSH-9.2	Trigger Event (A01)
        // MSH-10.1	Message Control ID	(Unique identifier)
        // MSH-11	Processing ID (P)
        // MSH-12	Version ID (e.g. 2.4) (version of HL7 used)
        // MSH-15 	Accept Acknowledgment Type (AL)
        // MSH-16	Application Acknowledgment Type (NE)

        // 2. EVN (Event Type)
        // EVN-1 Event Type Code (A01)
        // EVN-2 Recorded Date/Time - Current Date/time YYYYMMDDHHMM
        // EVN-3 Date/Time Planned Event
        // EVN-4 Event Reason Code Eg. ADM
        // EVN-5.1 Operator ID
        // EVN-5.2 Operator Surname
        // EVN-5.3 Operator First name

        // 3. PID (Patient Identification)
        // patient ID PID-3   // NB is this the internal UCLH hospital number? (MRN)
        String familyName = "Doe"; // PID-5.1
        String givenName = "Jane"; // PID-5.2
        String middleName = ""; // middle name PID-5.3
        String patientTitle = ""; // title PID-5.5 e.g. "Mr"
        String birthdatetime = "unknown"; // DOB PID-7.1 YYYYMMDD (no time of birth)
        String sex = "unknown";  // PID-8
        String patientAddress = ""; // patient address PID-11.1 to PID-11.4
        String patientPostcode = ""; // patient postcode PID-11.5
        String patientCounty = ""; // patient county PID-11.9
        // home phone, mobile, email PID-13.1
        // business phone PID-14.1
        // PID-15.1	Interpreter code
        // PID-15.4	Language code
        // In place of PID-16 Carecast has ZLC.15 Marital status
        // PID-17.1 Religion
        // PID-18.1	Account number
        // In place of PID-22 Carecast has PID-10.1 Ethnicity
        // Date of death PID-29.1 YYYYMMDDHHmm format though hhmm is mainly 0000
        // Death indicator PID-30. Y = dead, N = alive.

        // 4. PV1 (Patient Visit) - note there is some duplication below e.g. consultant details
        // PV1-2  Patient Class	PV1-2.1	Eg. A or I. Episode Type. Ref[1]
        // PV1-3.1 Current Ward Code e.g. T06
        // PV1-3.2 Current Room Code e.g. T06A
        // PV1-3.3 Current Bed e.g. T06-32
        // PV1-4.1 1st repeat = admit priority (e.g. I), 2nd = admit type (e.g. A)
        // PV1-7.1 (1st repeat) Consultant code. Consultant for the clinic. Attending doctor GMC Code
        // PV1-7.2 (1st repeat) Consultant surname Eg. CASSONI
        // PV1-7.3 (1st repeat) Consultant Fname Eg. M
        // PV1-7.4 (1st repeat) Consultant mname Eg. A
        // PV1-7.6 (1st repeat) Consultant Title Eg. Dr
        // PV1-7.7 (1st repeat) Consultant local code Eg. AC3
        // PV1-8.1 (1st repeat) Consultant Code	(Registered GP user pointer), 2nd Consultant Code (Referring doctor GMC Code)
        // PV1-8.2 (1st repeat) Consultant surname Eg. CASSONI, 2nd Consultant surname Eg. CASSONI
        // PV1-8.3 (1st repeat) Consultant Fname Eg. M, 2nd Consultant Fname Eg. M
        // PV1-8.4 (1st repeat) Consultant mname Eg. A, 2nd Consultant mname Eg. A
        // PV1-8.6 1st repeat Consultant Title Eg. Dr, 2nd Consultant Title	Eg. Dr
        // PV1-10	Hospital Service	Specialty eg. 31015
        // PV1-14	Admission Source	ZLC8.1	Source of admission


        // 5. Optional segments (not shown here)
        // PD - has details of GP, dentist, disability and Do Not Disclose indicator
        // NK1 - next of kin details
        // PV2
        // OBX
        // ZUK (non-standard)
         
        //pretty_print_pid(adt_01);
        PID pid = adt_01.getPID();

        //XPN xpn[] = pid.getPatientName(); 
        XPN xpn = pid.getPatientName(0);
        if (xpn != null /*&& xpn.length > 0*/) {
            //System.out.println("Non-null xpn");
            givenName = xpn.getGivenName().getValue();
            familyName = xpn/*[0]*/.getFamilyName().getSurname().getValue();
            middleName = xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValue();
        }
        sex = pid.getAdministrativeSex().getIdentifier().getValue();  // M or F - comes out as CWE[F] etc
        birthdatetime = pid.getDateTimeOfBirth().toString(); // may be null? e.g. 193508040000 or 19610615
        
        
        
       
        // NB public ID getIdentityUnknownIndicator()
        // Returns PID-31: "Identity Unknown Indicator" - creates it if necessary - maybe should use instead - but not present in e.g. v2.2

        System.out.println("\ngiven name is " + givenName);
        System.out.println("middle name or initial: " + middleName);
        System.out.println("family name is " + familyName);
        System.out.println("sex is " + sex);
        System.out.println("birthdatetime is " + birthdatetime);

        // Other data
        
        
        //String patient_id = pid.getPatientID().toString(); // in our Atos examples this is sometimes null. Is that possible?
        // Also patient_id sometimes a non-numeric string e.g. ******** so we should probably check it's an int (or long).
        // NB PID-2 is external patient id and PID-3 is internal patient ID (according to v2.2)
        // PID-3 uses getPatientIdentifierList() nb what if >1 identifier?! Due to merging?
        // v2.7 standard says PID-2 removed as of 2.7 so should use PID-3
        //String patient_id = pid.getPatientIdentifierList()[0].toString(); // e.g. CX[PATID1234^5^M11^ADT1^MR^GOOD HEALTH HOSPITAL]
        //System.out.println("Data: " + sex + "," + birthdatetime + "," + patient_id); 

        /*ca.uhn.hl7v2.model.v27.segment.*/MSH msh = adt_01.getMSH();
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

    /**
     * Write information to UDS
     * TODO: not yet implemented.
     */
    private void writeToDatabase() {

    }

    private HapiContext context;
    private PipeParser parser;

}
