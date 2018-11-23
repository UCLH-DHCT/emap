
import java.sql.*; // Uses postgresql-42.2.5.jar driver
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject; 
import org.json.simple.parser.*;

import java.io.FileNotFoundException;
import java.io.FileReader; 
import java.io.PrintWriter;
import java.io.File;

/**
 * JDBCTest was written for the autumn 2018 demo. It extracts data from the IDS (DUMMY_IDS) and 
 * writes relevant info to the UDS (INFORM_SCRATCH)
 * <p>
 * To compile:
 * <pre>
 * export CLASSPATH=.:postgresql-42.2.5.jar:json-simple-1.1.1.jar
 * javac JDBCTest.java
 * java JDBCTest
 * </pre>
 * A typical location value is {@code T11S^B11S^T11S-32}
 * <p>
 * NB this demo code is intended to be single threaded. Later might want to use things like ConcurrentHashMap.
 * </p>
 * <p>
 * From PostgresApp documentation
 * https://postgresapp.com/documentation/cli-tools.html
 * to install the App:
 * </p>
 * <pre>
 * sudo mkdir -p /etc/paths.d 
 * sudo echo /Applications/Postgres.app/Contents/Versions/latest/bin | sudo tee /etc/paths.d/postgresapp
 * </pre>
 */
public class JDBCTest {

	private static long last_unid_processed_last_time = 0; // Last UNID currently stored in UDS

	
	///////////////////////////////////////////////////
	// Strings to hold the names of the keys in dict.
	///////////////////////////////////////////////////

	// IDS column names:
	private static final String UNID = "UNID"; // PK
	private static final String PATIENT_NAME = "PatientName";
	private static final String PATIENT_MIDDLE_NAME = "PatientMiddleName";
	private static final String PATIENT_SURNAME = "PatientSurname";
	private static final String DATE_OF_BIRTH = "DateOfBirth";
	//private static final String NHS_NUMBER = "NHSNumber";
	private static final String HOSPITAL_NUMBER = "HospitalNumber";
	private static final String PATIENT_CLASS = "PatientClass";
	private static final String PATIENT_LOCATION = "PatientLocation";
	private static final String ADMISSION_DATE = "AdmissionDate";
	private static final String DISCHARGE_DATE = "DischargeDate";
	private static final String MESSAGE_TYPE = "MessageType";
	//private static final String SENDER_APPLICATION = "SenderApplication"; // NOT NULL
	//private static final String MESSAGE_IDENTIFIER = "MessageIdentifier"; // NOT NULL
	private static final String MESSAGE_VERSION = "MessageVersion"; // NOT NULL
	private static final String MESSAGE_DATE_TIME = "MessageDateTime"; // NOT NULL
	//private static final String HL7_MESSAGE = "HL7Message"; // NOT NULL 
	private static final String PERSIST_DATE_TIME = "PersistDateTime"; // NOT NULL

	// UDS column names:
	//birth_date_time - change so same as UDS
	private static final String SEX = "Sex";
	private static final String PATIENT_ADDRESS = "PatientAddress";
	private static final String PATIENT_DEATH_DATE = "PatientDeathDate";
	//patient_death_indicator
	//identity_unknown_indicator
	// readmission _indicator
	// hospital_service
	private static final String PATIENT_FULL_NAME = "PatientFullName";
	private static final String LAST_UPDATED = "LastUpdated"; // *** NB *** we tend to use the IDS MessageDateTime

	// Do not put single quotes around this String when using in SQL queries.
	// We only do that for timestamps with values.
	private static final String NULL_TIMESTAMP = "null::timestamp";

	public static void main(String[] args) {

		String filename = "config.json"; // maybe better to make this a command-line option?

		String udshost = "", idshost = "", idsusername = "", idspassword = "", udsusername = "", udspassword = "";

		Object obj;
		JSONObject jo; 
 		try {
			 obj = new JSONParser().parse(new FileReader(filename)); 
			 
			 // typecasting obj to JSONObject 
			 jo = (JSONObject) obj;

			udshost = (String)jo.get("udshost");
			idshost = (String)jo.get("idshost");
			idsusername = (String)jo.get("idsusername");
			udsusername = (String)jo.get("udsusername");
			// Passwords - can we store the MD5 hashes (rather than plsin text) in the Json file so not exposed?
			idspassword	= (String)jo.get("idspassword");
			udspassword	= (String)jo.get("udspassword");

			//System.out.println("UDS:" + udshost + ", IDS: " + idshost);
			//System.exit(1);
 		}
 		catch (Exception e) { // FileNotFoundException or IOException
 			e.printStackTrace();
 			System.out.println("*** DEBUG: file not found or IO exception - exiting ***");
 			System.exit(1);
 		}


		// Keep track of possibly-changing values e.g. name etc. 
		// Key is Postgres table column name, value = what we need to insert
		Map<String, String> dict = new HashMap<String, String>();


		// process_command_line_arguments(args);

		Connection conn;
		Statement st;
		ResultSet rs;

		//////////////////////////////////////////
		// OK now try reading from the dummy IDS.
		//
		// We want to pull data from the IDS and insert it to the UDS.
		// Fields required for UDS (some may be null for a given message):
		//
		// PERSON - (setID), patient_ID_list NOT NULL, patient_name NOT NULL, birth_date_time,
		// sex, patient_address,  patient_death_date_time, patient_death_indicator, 
		// identity_unknown_indicator, last_update_date_time (PID-33)
		//
		// PATIENT_VISIT - setID, patient_class, assigned_location, admission_type, 
		// prior_location, attending_doctor, referring_doctor, hospital_service,
		// temp_location, preadmit_test_indicator, readmission_indicator, visit_number,
		// discharge_disposition, discharged_to_location, pending_location, 
		// admit_datetime, discharge_date_time, alternate_vist_id, visit_indicator
		// Need to add patient id list
		//////////////////////////////////////////
		//String ids_url = "jdbc:postgresql://localhost/DUMMY_IDS"; // IDS (dummy)
		//String uds_url = "jdbc:postgresql://localhost/INFORM_SCRATCH"; // UDS (dummy)
						// jdbc:postgresql:INFORM_SCRATCH

		String ids_url = "jdbc:postgresql://" + idshost + "/DUMMY_IDS"; // IDS (dummy)
		String uds_url = "jdbc:postgresql://" + udshost + "/INFORM_SCRATCH"; // UDS (dummy)

		// Extraction of data from IDS step. No HL7 parsing required.
		try {

			System.out.println("Trying to connect");
			
			Connection uds_conn = DriverManager.getConnection(uds_url, udsusername, udspassword);

			Statement uds_st = uds_conn.createStatement();
			last_unid_processed_last_time = read_last_unid_from_UDS(uds_conn);
			System.out.println("AT START, LAST UNID STORED = " + last_unid_processed_last_time);

			String ids_query = get_IDS_query_string(last_unid_processed_last_time);
			conn = DriverManager.getConnection(ids_url, idsusername, idspassword);
			st = conn.createStatement();
			rs = st.executeQuery(ids_query);
		
			long latest_unid_read_this_time = 0; // last one read from IDS this time

			while (rs.next()) // Iterate over records received from the query (if any)
			{

				dict.clear();

				// We don't want this stored in the per-person dict
				latest_unid_read_this_time = rs.getLong("UNID"); // use below if all successful.
				System.out.println("** DEBUG: latest_unid_read_this_time = " + latest_unid_read_this_time);
				extract_fields_from_IDS(rs, dict);
				
				// Now insert this record into the UDS PERSON table.

				// Check to see if this person is already in the PERSON table - obtain latest update time
				// if they are, and our update time is later than that stored, update table as appropriate.
				String person_entry_last_updated = "NULL";
				String who = dict.get(PATIENT_FULL_NAME); // debug
				if (already_in_person_table(uds_conn, dict)) {
			
					System.out.println("** DEBUG: " + who + " is already in UDS");

					// obtain timestamp last updated - are we more recent?
					person_entry_last_updated = get_last_timestamp_of_person(uds_conn, dict);
					System.out.println("Stored timestamp is " + person_entry_last_updated);

					// Now we need to update the PERSON record. Need to check for null values and also outdated values
					// eg they change their name

					// Also need to find current PATIENT_VISIT record and update as appropriate
					// e.g. we get a discharge or transfer message.
					long visitid = get_current_visitid_for_person(uds_conn, dict);
					System.out.println("** DEBUG: current visit id is " + visitid);
					// It will be a NEW visit if they were discharged last visit:
					if ( 0 == visitid )  {
						String patient_visit_insert = get_UDS_insert_patient_visit_string(dict);
						write_update_to_database (patient_visit_insert, uds_st);
					}
					else { // Found a "live" patient_visit entry. Update if necessary.
						update_patient_visit(uds_conn, dict, visitid);
					}
					

				}
				else { // It's a completely new PERSON entry - and therefore a new patient_visit entry.
					
					System.out.println("** DEBUG: " + who + " is NOT already in UDS");

					// Now we write the PERSON data to the UDS
					String person_insert = get_UDS_insert_person_string(dict);
					write_update_to_database(person_insert, uds_st);

					// As this person has only just been added to UDS, he/she will have a new
					// PATIENT_VISIT entry:
					String patient_visit_insert = get_UDS_insert_patient_visit_string(dict);
					write_update_to_database (patient_visit_insert, uds_st);

				}
		

			} // end (while)

			if (latest_unid_read_this_time > last_unid_processed_last_time) {
				write_last_unid_to_UDS(uds_conn, latest_unid_read_this_time, last_unid_processed_last_time);
			}

			last_unid_processed_last_time = read_last_unid_from_UDS(uds_conn);
			System.out.println("AFTER PROCESSING, LAST UNID STORED = " + last_unid_processed_last_time);

			uds_st.close();
			uds_conn.close();
			rs.close();
			st.close();	
			conn.close();

			
		}
		catch (SQLException e) {
			e.printStackTrace();
		}		

	}	// End (main)



	/**
	 * Build the string used to query the IDS for records since
	 * last_unid_processed_last_time.
	 * 
	 * @param last_unid_processed_last_time last UNID processed
	 * @return SQL query string 
	 */
	private static String get_IDS_query_string(long last_unid_processed_last_time) {

		System.out.println("** DEBUG - get_IDS_query_string fn");

		// Build the query - select all messages later than last_unid_processed_last_time:
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(UNID).append(", ");
		query.append(PATIENT_NAME).append(", ");  
		query.append(PATIENT_MIDDLE_NAME).append(", "); 
		query.append(PATIENT_SURNAME).append(", "); 
		query.append(DATE_OF_BIRTH).append(", "); 
		query.append(HOSPITAL_NUMBER).append(", ");  
		query.append(PATIENT_CLASS).append(", "); 
		query.append(PATIENT_LOCATION).append(", ");  
		query.append(ADMISSION_DATE).append(", ");  
		query.append(DISCHARGE_DATE).append(", "); 
		query.append(MESSAGE_TYPE).append(", "); 
		query.append(MESSAGE_VERSION).append(", "); 
		query.append(MESSAGE_DATE_TIME).append(", ");
		query.append(PERSIST_DATE_TIME);
		query.append(" FROM TBL_IDS_MASTER ");
		query.append(" where ").append(UNID).append(" > ").append(last_unid_processed_last_time).append(";");

		return query.toString();


	}


	/**
	 * Pulls fields from the IDS and populate the Dictionary for this record, 
	 * using default values where required.
	 * 
	 * @param rs ResultSet, obtained from query previously
	 * @param dict Map which keeps track of values from database columns.
	 * @throws SQLException
	 */
	private static void extract_fields_from_IDS(ResultSet rs,
					Map<String, String> dict) throws SQLException {

		System.out.println("** DEBUG - extract_fields_from_IDS fn");


		// NB some IDS fields cannot be null so we should always get a value for those:
		// UNID, SenderApplication, MessageIdentifier, MessageVersion, MessageDateTime, HL7Message, PersistDateTime
		
		String value = "";

		value = rs.getString(PATIENT_NAME);
		if (rs.wasNull()) {
			value = "NULL";
		}
		dict.put(PATIENT_NAME, value);
		
		value = rs.getString(PATIENT_MIDDLE_NAME);
		if (rs.wasNull()) {
			value = "NULL";
		}
		dict.put(PATIENT_MIDDLE_NAME, value);
		
		value = rs.getString(PATIENT_SURNAME);
		if (rs.wasNull()) {
			value = "NULL";
		}
		dict.put(PATIENT_SURNAME, value);

		value = rs.getString(DATE_OF_BIRTH);
		if (rs.wasNull()) {
			value = NULL_TIMESTAMP;
		}
		else {
			value = convert_timestamp(value);
		}
		dict.put(DATE_OF_BIRTH, value);

		// nhs number - not used

		value = rs.getString(HOSPITAL_NUMBER);
		if (rs.wasNull()) {
			value = "NULL";
		}
		dict.put(HOSPITAL_NUMBER, value);

		value = Character.toString(rs.getString(PATIENT_CLASS).charAt(0));
		if (rs.wasNull()) {
			value = "NULL";
		}
		dict.put(PATIENT_CLASS, value);

		value = rs.getString(PATIENT_LOCATION);
		if (rs.wasNull()) {
			value = "NULL";
		}
		dict.put(PATIENT_LOCATION, value);

		value = rs.getString(ADMISSION_DATE);
		if (rs.wasNull()) {
			value = NULL_TIMESTAMP;
		}
		else {
			value = convert_timestamp(value);
		}
		dict.put(ADMISSION_DATE, value);

		value = rs.getString(DISCHARGE_DATE);
		if (rs.wasNull()) {
			value = NULL_TIMESTAMP;
		}
		else {
			value = convert_timestamp(value);
		}
		dict.put(DISCHARGE_DATE, value);

		value = rs.getString(MESSAGE_TYPE);
		dict.put(MESSAGE_TYPE, value); // e.g. ADT^A28

		// The following should never be null so if we get an exception there is probably something wrong with the IDS.
		//msg_version = rs.getString("MessageVersion"); // e.g. "2.2" (HL7 version)	
		value = rs.getString(MESSAGE_VERSION);
		dict.put(MESSAGE_VERSION, value);
		value = rs.getString(MESSAGE_DATE_TIME);
		dict.put(MESSAGE_DATE_TIME, convert_timestamp(value));
		// SENDER_APPLICATION
		// MESSAGE_IDENTIFIER
		// HL7_MESSAGE
		value = rs.getString(PERSIST_DATE_TIME);
		dict.put(PERSIST_DATE_TIME, value);

		// The following can only have values obtained if the HL7 message is parsed,
		// so we use default vaues here:
		dict.put(SEX, "U");
		dict.put(PATIENT_ADDRESS, "unknown");
		dict.put(PATIENT_DEATH_DATE, NULL_TIMESTAMP);

		// In the UDS we just store the patient's name as one long string, not separate strings.
		StringBuilder patient_name = new StringBuilder(dict.get(PATIENT_NAME));
		patient_name.append(" ").append(dict.get(PATIENT_MIDDLE_NAME));
		patient_name.append(" ").append(dict.get(PATIENT_SURNAME));
		System.out.println(patient_name.toString());
		dict.put(PATIENT_FULL_NAME, patient_name.toString());

	}


	/**
	 * Buld string to insert a NEW record into the UDS PERSON table
	 * 
	 * @param dict Map of data items from IDS
	 * @return SQL INSERT string for UDS PERSON table
	 */
	private static String get_UDS_insert_person_string(Map<String,String> dict) { 

		System.out.println("** DEBUG - get_UDS_insert_person_string()");

		StringBuilder uds_insert = new StringBuilder(); 
		uds_insert.append("INSERT INTO PERSON ("); 
		uds_insert.append(HOSPITAL_NUMBER).append(", "); 
		uds_insert.append(PATIENT_FULL_NAME).append(", ");
		uds_insert.append(DATE_OF_BIRTH).append(","); 
		uds_insert.append(SEX).append(","); 
		uds_insert.append(PATIENT_ADDRESS).append(",");
		uds_insert.append(PATIENT_DEATH_DATE).append(","); 
		// here: patient death indicator
		// here: identity unknown
		uds_insert.append(LAST_UPDATED); 
		//uds_insert.append("patient_death_date_time, patient_death_indicator, identity_unknown_indicator, last_update_date_time");

		// NB Need to parse HL7 timestamps to get into correct format for Postgres.
		uds_insert.append(") VALUES (");
		uds_insert.append(dict.get(HOSPITAL_NUMBER)).append(", ");
		//uds_insert.append("NULL,"); // set_ID
		uds_insert.append("'").append(dict.get(PATIENT_FULL_NAME)).append("', ");
		uds_insert.append("'").append(dict.get(DATE_OF_BIRTH)).append("', "); // example: 2018-10-01 14:57:41.090449
		uds_insert.append("'").append(dict.get(SEX)).append("', ");
		uds_insert.append("'").append(dict.get(PATIENT_ADDRESS)).append("', ");
		String death = dict.get(PATIENT_DEATH_DATE);
		if (death.equals(NULL_TIMESTAMP)) {
			uds_insert.append(death).append(", ");
		}
		else {
			uds_insert.append("'").append(death).append("', "); // patient death time unknown without parsing HL7 (PID-29)
		}
			//uds_insert.append("''").append(", "); // patient death indicator unknown without parsing HL7 (PID-30)
		//uds_insert.append("''").append(", "); // identity unknown unknown without parsing HL7 (PID-31)
		//msg_date_time = convert_timestamp(msg_date_time);

		// To populate the UDS LastUpdated field, for now we are using the IDS's MessageDateTime field (NOT NULL)
		uds_insert.append("'").append(dict.get(MESSAGE_DATE_TIME)).append("'"); // last update date/time unknown without parsing HL7 (PID-33)	
		uds_insert.append(");");

		return uds_insert.toString();

	}


	/**
	 * Get SQL string needed to add a NEW patient_visit entry to UDS
	 * 
	 * @param dict the Map which keeps track of IDS database data items
	 * @return the INSERT statement string
	 */
	private static String get_UDS_insert_patient_visit_string(Map<String,String> dict) { 

		// Person will be in database by now.
		StringBuilder uds_insert = new StringBuilder("");
		uds_insert.append("INSERT INTO PATIENT_VISIT (");
		uds_insert.append(HOSPITAL_NUMBER).append(", ");
		uds_insert.append(PATIENT_CLASS).append(", ");
		uds_insert.append(PATIENT_LOCATION).append(", ");
		// hospital_service, ");
		//uds_insert.append("readmission_indicator, 
		uds_insert.append(ADMISSION_DATE).append(", ");
		uds_insert.append(DISCHARGE_DATE).append(", ");
		uds_insert.append(LAST_UPDATED);
		uds_insert.append(") VALUES (");
		uds_insert.append(dict.get(HOSPITAL_NUMBER)).append(", ");
		uds_insert.append("'").append(dict.get(PATIENT_CLASS)).append("'").append(", ");
		uds_insert.append("'").append(dict.get(PATIENT_LOCATION)).append("'").append(", ");
		//uds_insert.append("NULL").append(", "); // service
		//uds_insert.append("NULL").append(", "); // readmission_indicator

		// We want to enclose timestamps within '' but NOT a string like NULL or null::timestamp
		String admit = dict.get(ADMISSION_DATE);
		if (admit.equals(NULL_TIMESTAMP)) {
			uds_insert.append(admit).append(", ");
		}
		else {
			uds_insert.append("'").append(admit).append("', ");
		}
		String discharge = dict.get(DISCHARGE_DATE);
		if (discharge.equals(NULL_TIMESTAMP)) {
			uds_insert.append(discharge).append(", ");
		}
		else {
			uds_insert.append("'").append(discharge).append("'").append(", ");
		}
		// This one should never be null so we don't perform the null check here:
		uds_insert.append("'").append(dict.get(MESSAGE_DATE_TIME)).append("'"); // already converted
		uds_insert.append(");");

		return uds_insert.toString();

	}


	/**
	 * Look up patient visits for this person (keyed on hospital_number)
	 * <p>
	 * NB what if there are multiple visits for the same patient with null discharge dates...?
	 * ... this would mean an error had occurred at some point. Maybe flag these up.
	 * 
	 * @param c Current connection to UDS
	 * @param dict Map of data items
	 * @return <code>visitid</code> if a current visit (i.e. admission but no discharge date) found, else 0
	 * @throws SQLException
	 */
	private static long get_current_visitid_for_person(Connection c, Map<String, String> dict) 
												throws SQLException {

		Statement st = c.createStatement();
		StringBuilder sb = new StringBuilder();
		//select visitid, dischargedate from patient_visit where hospitalnumber = '94006000';
		sb.append("select visitid, dischargedate from PATIENT_VISIT ");
		sb.append("where ").append(HOSPITAL_NUMBER).append(" = '").append(dict.get(HOSPITAL_NUMBER));
		sb.append("';");

		ResultSet rs = st.executeQuery(sb.toString());
		while (rs.next()) {
			String discharge_date = rs.getString(DISCHARGE_DATE);

			if (rs.wasNull()) {
				String visitid = rs.getString("visitid"); // this is autoincemented
				return Long.parseLong(visitid);
			}

		}

		return 0;
	}


	/**
	 * Update an existing patient_visit record:
	 * visitid | hospitalnumber | patientclass | patientlocation | 
	 * hospitalservice | readmissionindicator | admissiondate | 
	 * dischargedate | lastupdated
	 * 
	 * @param c Current connection to UDS
	 * @param dict The Map of values
	 * @param visitid The unique ID of this visit
	 * @throws SQLException
	 */
	private static void update_patient_visit(Connection c, Map<String,String> dict, long visitid) 
																throws SQLException {

		StringBuilder sb = new StringBuilder();
		sb.append("update patient_visit ");
		sb.append("set lastupdated = ");
		String update = dict.get(MESSAGE_DATE_TIME);
		if (update.equals(NULL_TIMESTAMP)) {
			sb.append(update);
		}
		else {
			sb.append("'").append(update).append("'");
		}

		// Update other values if relevant e.g. we get an ADT transfer message.
		String msgtype = dict.get(MESSAGE_TYPE);
		if (msgtype.equals("ADT^A02")) { // transfer
			sb.append(", patientlocation = ").append(dict.get(PATIENT_LOCATION));
		}
		else if (msgtype.equals("ADT^A03")) { // discharge - NB does this have a new location too?
			String ddate = dict.get(DISCHARGE_DATE);
			if (ddate.equals(NULL_TIMESTAMP)) {
				sb.append(", dischargedate = ").append(ddate);
			}
			else {
				sb.append(", dischargedate = '").append(ddate).append("'");
			}
		}
		else {
			///???
		}

		sb.append(" where visitid = '").append(visitid).append("' and lastupdated < '").append(dict.get(MESSAGE_DATE_TIME/*LAST_UPDATED*/)).append("';");

		System.out.println("** DEBUG: update_patient_visit: SQL = " + sb.toString());

		Statement st = c.createStatement();
		/*ResultSet rs = */ st.executeUpdate(sb.toString());

	}


	/**
	 * Update an existing PERSON record in UDS. 
	 * <p>
	 * Not yet implemented. This could well be quite complicated. Issue #9
	 * 
	 * @param c Current connection to UDS
	 * @throws SQLException
	 */
	private static void update_person_record(Connection c) throws SQLException {


	}


	/**
	 * See if this hospital_number is already in the PERSON table
	 * <p>
	 * NB does that necessarily mean they will also be in the patient_visit table?
	 * No they might be an outpatient. And if they ARE in the patient_visit table,
	 * they might be there multiple times.
	 * 
	 * @param c Current connection to UDS
	 * @param dict The Map of data items
	 * @return <code>true</code> if the hospital_number (stored in <code>dict</code>) is present,
	 * <code>false</code> otherwise
	 * @throws SQLException
	 */
	private static boolean already_in_person_table(Connection c, 
												Map<String, String> dict) throws SQLException {

		System.out.println("** DEBUG - already_in_person_table()");

		Statement st = c.createStatement();
		StringBuilder query = new StringBuilder("select * from person where ");
		query.append(HOSPITAL_NUMBER).append(" = '");
		query.append(dict.get(HOSPITAL_NUMBER)).append("';");
		//System.out.println("QUERY: " + query.toString());
		ResultSet rs = st.executeQuery(query.toString());
		if (rs.next()) {
			//System.out.println("already in there");
			return true;
		}
		else {
			//System.out.println("NOT already there");
			return false;
		}
	}


	/**
	 * Get the last time this person's record in the UDS was updated.
	 * In theory it should never be null but we take a safe approach.
	 * 
	 * 
	 * @param c Current connection to UDS
	 * @param dict The Map of key-value data pairs
	 * @return The last timestamp, or NULL if not available.
	 * @throws SQLException
	 */
	private static String get_last_timestamp_of_person(Connection c, Map<String,String> dict) throws SQLException {

		System.out.println("** DEBUG - get_last_timestamp_of_person fn");


		Statement st = c.createStatement();
		StringBuilder query = new StringBuilder("select ");
		query.append(LAST_UPDATED).append(" from person where ").append(HOSPITAL_NUMBER).append(" = '");
		query.append(dict.get(HOSPITAL_NUMBER)).append("';");
		System.out.println("QUERY: " + query.toString());
		ResultSet rs = st.executeQuery(query.toString());

		String timestamp = "NULL";
		if ( rs.next() ) {
			timestamp = rs.getString(LAST_UPDATED);
		}
		return timestamp;

	}


	/**
	 * Obtain the UNID most recently (last time) read from the IDS and processed;
	 * this is stored in the UDS.
	 * 
	 * @param c Current connection to UDS
	 * @return The UNID, which will be zero if none have been processed.
	 * @throws SQLException
	 * @see write_last_unid_to_UDS
	 */
	private static long read_last_unid_from_UDS (Connection c) throws SQLException {

		System.out.println("** DEBUG - read_last_unid_from_UDS fn");

		Statement st = c.createStatement();
		String query = "select latest from last_unid_processed ;";
		ResultSet rs = st.executeQuery(query);
		String last = "null";
		if (rs.next()) { 
		
			last = rs.getString(1);
			if (last.equals("null") || last.equals("")) { // unlikely
				return 0;
			}
			else {
				long l = 0;
				try {
					l = Long.parseLong(last);
				}
				catch (NumberFormatException e)  {
					System.out.println("** ERROR: 'long number' from UDS is " + last);
					l = 0;
				}
				return l; 
			}
		}
		else { // Nothing in result
			return 0;
		}
	}


	/**
	 * First we write new latest UNID processed (read this time).
	 * Then we delete the existing one.
	 * 
	 * @param c Current connection to UDS.
	 * @param latest_unid_read_this_time The new latest IDS UNID processed.
	 * @param unid_processed_last_time The existing value (in UDS) of last IDS UNID processed.
	 * @return 0 if OK, -1 if the new value is less than the old value, -2 if SQLException thrown.
	 * @throws SQLException
	 * @see read_last_unid_from_UDS
	 */
	private static int write_last_unid_to_UDS(Connection c, 
				long latest_unid_read_this_time,
				long unid_processed_last_time) throws SQLException {

		System.out.println("** DEBUG - write_last_unid_to_UDS fn");

		// Bail out if it's the trivial case.
		if (latest_unid_read_this_time == unid_processed_last_time) {
			return 0;
		}

		// If we get here someone probably needs to manually reset the UDS UNID table to 0
		if (latest_unid_read_this_time < unid_processed_last_time) {
			System.out.println("**ERROR: latest UNID read from IDS is less than that stored in UDS");
			return -1;
		}

		Statement st = c.createStatement();
		StringBuilder sb = new StringBuilder("");
		sb.append("INSERT INTO last_unid_processed (latest) values(");
		sb.append(latest_unid_read_this_time).append(");");
		sb.append("delete from last_unid_processed where latest='");
		sb.append(unid_processed_last_time).append("';");

		int ret = 0;
		try {
			ret = st.executeUpdate(sb.toString());
			//String res = new String("return value was " + ret);
			//System.out.println(res);

			// If we reach here it is likely our update was processed OK.
		}
		catch (SQLException e) {
			e.printStackTrace();
			// should we add a rollback here?
			ret = -2;
		}
		return ret;

	}


	/**
	 * Utility function to write an update to a database
	 * 
	 * @param str The SQL string
	 * @param s The current statement
	 * @return value of <code>executeUpdate()</code>
	 */
	private static int write_update_to_database (String str, Statement s) {

		System.out.println("** DEBUG - write_update_to_database()");


		System.out.println("******* statement: *****");
		System.out.println(str);
		int ret = 0;
		try {
			ret = s.executeUpdate(str);
			//String res = new String("return value was " + ret);
			//System.out.println(res);

			// If we reach here it is likely our update was processed OK.
		}
		catch (SQLException e) {
			e.printStackTrace();
			// should we add a rollback here?
		}

		return ret;
	}


	/**
	 * Convert HL7 format timestamp into a Postgres one.
	 * <p>
	 * NB some HL7 timestamps won't have the decimal part. And some will only
	 * be accurate to the day (so no hhmmss information)
	 * <p>
	 * Unfortunately, Ashish says the precision is the same as what he sent me
	 * in the test messages, which is to the nearest second. However we don't
	 * worry about that in this method.
	 * 
	 * @param hl7 An HL7 format timestamp e.g. "20181003141807.7618"
	 * @return Postgres-format timestamp e.g. "2018-10-03 14:18:07.7618"
	 */
	private static String convert_timestamp (String hl7) {

		System.out.println("** DEBUG - convert_timestamp()");


		System.out.println(hl7);

		// If it's NULL return NULL
		if (hl7 == null || hl7.equals("") || hl7.equals("NULL")) return "NULL";

		// First make sure this is not already in Postgres format (sanity check):
		String[] test = hl7.split("-");
		if (test.length >= 3) return hl7; // Assumes year, month and day all present!

		// HL7 timestamp format is YYYYMMDDHHMMSS[.S[S[S[S]]]] (with +/- offset if required)
		String[] bigparts = hl7.split("\\."); // We have to escape the period character.
		int len = bigparts.length; 
		String firstpart = bigparts[0];
		
		String year = firstpart.substring(0,4); 
		String month = firstpart.substring(4,6); 
		String day = firstpart.substring(6,8); 

		// Deal with hhmmss (or lack of them)
		String hours = "00";
		String minutes = "00";
		String seconds = "00";
		int stringlen = firstpart.length();
		if (stringlen >= 10) {
			hours = firstpart.substring(8,10); 
		}
		if (stringlen >= 12) {
			minutes = firstpart.substring(10,12);
		}
		if (stringlen >= 14) {
			seconds = firstpart.substring(12,14); 
		}


		StringBuilder postgres = new StringBuilder(year);
		postgres.append("-").append(month);
		postgres.append("-").append(day);
		postgres.append(" ").append(hours);
		postgres.append(":").append(minutes);
		postgres.append(":").append(seconds);

		if (len > 1) { // Decimal part exists. NB we don't perform rounding.
			String secondpart = bigparts[1];
			postgres.append(".").append(secondpart);
		}

		String result = postgres.toString(); 
		System.out.println("Now timestamp is " + result + " ************");

		return result;

	}
}

// EOF
