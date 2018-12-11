
import java.sql.*; // Uses postgresql-42.2.5.jar driver
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject; 
import org.json.simple.parser.*;

import java.io.FileNotFoundException;
import java.io.FileReader; 
import java.io.PrintWriter;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter; //https://stackoverflow.com/questions/8746084/string-to-localdate/22538939#22538939
import java.util.Locale;

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
	private static final String NULL = "NULL";

	public static void main(String[] args) {

		/*String adate = "2018-12-10 13:05:34.015681";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnn");
		formatter=formatter.withLocale(Locale.en_GB);
		LocalDate ld = LocalDate.parse(adate, formatter);*/

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
			// Passwords - can we store the MD5 hashes (rather than plain text) in the Json file so not exposed?
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
		// PERSON_SCRATCH - (setID), patient_ID_list NOT NULL, patient_name NOT NULL, birth_date_time,
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
		String uds_url = "jdbc:postgresql://" + udshost + "/INFORM_SCRATCH?stringtype=unspecified"; // UDS (dummy)

		// Extraction of data from IDS step. No HL7 parsing required.
		try {

			System.out.println("Trying to connect");
			
			Connection uds_conn = DriverManager.getConnection(uds_url, udsusername, udspassword);
			////uds_conn.setAutoCommit(false); // http://weinan.io/2017/05/21/jdbc-part5.html


			// For debug/testing ONLY
			drop_my_UDS_tables(uds_conn);




			create_UDS_tables_if_necessary(uds_conn);

			Statement uds_st = uds_conn.createStatement();
			last_unid_processed_last_time = read_last_unid_from_UDS(uds_conn);
			System.out.println("AT START, LAST UNID STORED = " + last_unid_processed_last_time);

			//String ids_query = get_IDS_query_string(last_unid_processed_last_time);
			conn = DriverManager.getConnection(ids_url, idsusername, idspassword);
			rs = query_IDS(last_unid_processed_last_time, conn);
			
			//st = conn.createStatement();
			//rs = st.executeQuery(ids_query);
		
			long latest_unid_read_this_time = 0; // last one read from IDS this time

			while (rs.next()) // Iterate over records received from the IDS query (if any)
			{

				dict.clear();

				// We don't want this stored in the per-person dict
				latest_unid_read_this_time = rs.getLong("UNID"); // use below if all successful.
				System.out.println("** DEBUG: latest_unid_read_this_time = " + latest_unid_read_this_time);
				extract_fields_from_IDS(rs, dict);
				
				// Now insert this record into the UDS PERSON_SCRATCH table.

				// Check to see if this person is already in the PERSON_SCRATCH table - obtain latest update time
				// if they are, and our update time is later than that stored, update table as appropriate.
				String person_entry_last_updated = "NULL";
				String who = dict.get(PATIENT_FULL_NAME); // debug
				if (already_in_person_table(uds_conn, dict)) {
			
					System.out.println("** DEBUG: " + who + " is already in UDS");

					// obtain timestamp last updated - are we more recent?
					////person_entry_last_updated = get_last_timestamp_of_person(uds_conn, dict);
					///System.out.println("Stored timestamp is " + person_entry_last_updated);

					// Now we need to update the PERSON_SCRATCH record. Need to check for null values and also outdated values
					// eg they change their name

					// Also need to find current PATIENT_VISIT record and update as appropriate
					// e.g. we get a discharge or transfer message.
					/*long visitid = get_current_visitid_for_person(uds_conn, dict);
					System.out.println("** DEBUG: current visit id is " + visitid);
					// It will be a NEW visit if they were discharged last visit:
					if ( 0 == visitid )  {
						String patient_visit_insert = get_UDS_insert_patient_visit_string(dict);
						write_update_to_database (patient_visit_insert, uds_st);
					}
					else { // Found a "live" patient_visit entry. Update if necessary.
						update_patient_visit(uds_conn, dict, visitid);
					}*/
					

				}
				else { // It's a completely new PERSON entry - and therefore a new patient_visit entry.
					
					System.out.println("** DEBUG: " + who + " is NOT already in UDS");

					// Now we write the PERSON_SCRATCH data to the UDS
					boolean result = insert_person_UDS(dict, uds_conn);
					if ( ! result ) {
						continue; // we don't bother with this person e.g. null hospital number.
					}

					// As this person has only just been added to UDS, he/she will have a new
					// PATIENT_VISIT entry:
					//String patient_visit_insert = get_UDS_insert_patient_visit_string(dict);
					//write_update_to_database (patient_visit_insert, uds_st);
					// Not necessarily. They could be an outpatient.

				}

				//////////////////////////////////////////
				// Take action based on HL7 message type.
				//////////////////////////////////////////

				long visitid = get_current_visitid_for_person(uds_conn, dict);
				String msgtype = dict.get(MESSAGE_TYPE);
				//System.out.println("** In main(): msgtype is " + msgtype);
				if (msgtype.equals(NULL)) {
					continue;
				}
				else if (msgtype.equals("ADT^A01")) { // admit

					// The current visit id is probably 0 (i.e. no entry in the
					// patient_visit table) but we double-check
					System.out.println("** DEBUG: current visit id is " + visitid);
					
					// It will be a NEW visit if they were discharged last visit,
					// or this is their first inpatient visit:
					if ( 0 == visitid )  {
						// Insert new PATIENT_VISIT entry:
						//String patient_visit_insert = get_UDS_insert_patient_visit_string(dict);
						//write_update_to_database (patient_visit_insert, uds_st);
						insert_patient_visit_uds(dict, uds_conn);

						visitid = get_current_visitid_for_person(uds_conn, dict);
						if ( 0 == visitid) {
							continue;
						}
						update_bedvisit_table(uds_conn, dict, visitid);
						// what if it failed?
					}
					else {
						/// ??? insert_bedvisit_entry(dict, visitid); ???
					}
					
				}
				else if (msgtype.equals("ADT^A02")) { // transfer
					
					// Error here if no current patient_visit entry???

					// Update bed location details.
					update_bedvisit_table(uds_conn, dict, visitid); // what if false?
				}
				else if (msgtype.equals("ADT^A03")) { // discharge

					// update pv table
					update_patient_visit(uds_conn, dict, visitid);

					// Update bed location details
					update_bedvisit_table(uds_conn, dict, visitid); // what if false?
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
			//st.close();	
			conn.close();

			
		}
		catch (SQLException e) {
			System.out.println("GOT AN ERROR");
			e.printStackTrace();
		}		

	}	// End (main)


	private static void create_UDS_tables_if_necessary (Connection c) throws SQLException {

		//Statement st = c.createStatement();
		StringBuffer sql = new StringBuffer(300);

		// The following table is based on the HL7 PID segment.
		sql.append("CREATE TABLE IF NOT EXISTS PERSON_SCRATCH (");
		sql.append("hospitalnumber char(8), PatientFullName	varchar(200) NOT NULL, DateOfBirth	timestamp, ");
		sql.append("Sex	char(1), PatientAddress	varchar(200), PatientDeathDate timestamp, LastUpdated timestamp);");

		// Based on the HL7 PV1 segment definition.
		sql.append("CREATE TABLE IF NOT EXISTS PATIENT_VISIT (");
		sql.append("VISITID SERIAL PRIMARY KEY, HospitalNumber char(8), PatientClass char(1) NOT NULL, HospitalService char(3), ");
		sql.append("ReadmissionIndicator char(1), AdmissionDate	timestamp, DischargeDate timestamp, LastUpdated timestamp);");

		// A period of time, during a patient_visit, spent in a specific bed.
		sql.append("CREATE TABLE IF NOT EXISTS BEDVISIT (");
		sql.append("BED_VISIT_ID BIGSERIAL PRIMARY KEY, patient_visit_id char(8) NOT NULL, ");
		sql.append("location varchar(30), start_time timestamp, end_time timestamp);");

		// Table to keep track of last IDS UNID processed successfully (data written to UDS).
		sql.append("CREATE TABLE IF NOT EXISTS LAST_UNID_PROCESSED (");
		sql.append("LATEST INT PRIMARY KEY);");

		PreparedStatement st = c.prepareStatement(sql.toString());
		st.execute();
		//st.executeUpdate(sql.toString());

	}


	// For debug/testing only
	private static void drop_my_UDS_tables (Connection c) throws SQLException {

		StringBuffer sql = new StringBuffer(150);
		sql.append("drop table bedvisit;");
		sql.append("drop table person_scratch;");
		sql.append("drop table last_unid_processed;");
		sql.append("drop table patient_visit;");

		PreparedStatement st = c.prepareStatement(sql.toString());
		st.execute();
	}

	/**
	 * Build the string used to query the IDS for records since
	 * last_unid_processed_last_time.
	 * 
	 * @param last_unid_processed_last_time last UNID processed
	 * @param c Current connection to IDS.
	 * @return ResultSet if successsful, otherwise SQLException thrown
	 */
	private static ResultSet query_IDS(long last_unid_processed_last_time, Connection c) 
	throws SQLException {

		//System.out.println("** DEBUG - get_IDS_query_string fn");

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
		//query.append(" where ").append(UNID).append(" > ").append(last_unid_processed_last_time).append(";");
		//query.append(" where  > ? ;");
		query.append(" where ").append(UNID).append(" >  ? ;");//.append(last_unid_processed_last_time).append(";");
		
		ResultSet rs;

		PreparedStatement st = c.prepareStatement(query.toString());
		st.setLong(1, last_unid_processed_last_time);
		rs = st.executeQuery();

		return rs;

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
			value = NULL;
		}
		dict.put(PATIENT_NAME, value);
		
		value = rs.getString(PATIENT_MIDDLE_NAME);
		if (rs.wasNull()) {
			value = NULL;
		}
		dict.put(PATIENT_MIDDLE_NAME, value);
		
		value = rs.getString(PATIENT_SURNAME);
		if (rs.wasNull()) {
			value = NULL;
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
		if (rs.wasNull() || value.equals("") || value.isEmpty()) {
			value = NULL;
		}
		/*else {
			System.out.println("value is *" + value + "*");
		}*/
		dict.put(HOSPITAL_NUMBER, value);

		value = Character.toString(rs.getString(PATIENT_CLASS).charAt(0));
		if (rs.wasNull() || value.equals("") || value.isEmpty()) {
			value = NULL;
		}
		dict.put(PATIENT_CLASS, value);

		value = rs.getString(PATIENT_LOCATION);
		if (rs.wasNull() || value.equals("") || value.isEmpty()) {
			value = NULL;
		}
		dict.put(PATIENT_LOCATION, value);

		value = rs.getString(ADMISSION_DATE);
		if (rs.wasNull() || value.equals("") || value.isEmpty()) {
			value = NULL_TIMESTAMP;
		}
		else {
			value = convert_timestamp(value);
		}
		dict.put(ADMISSION_DATE, value);

		value = rs.getString(DISCHARGE_DATE);
		if (rs.wasNull() || value.equals("") || value.isEmpty()) {
			value = NULL_TIMESTAMP;
		}
		else {
			value = convert_timestamp(value);
		}
		dict.put(DISCHARGE_DATE, value);

		value = rs.getString(MESSAGE_TYPE);
		if (rs.wasNull() || value.equals("") || value.isEmpty()) {
			value = NULL_TIMESTAMP;
		}
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
	 * Insert a new person_scratch record into the UDS.
	 * 
	 * @param dict Map of data items from IDS
	 * @param c The current connection to the UDS.
	 * @return false if unable to insert, true otherwise
	 */
	private static boolean insert_person_UDS(Map<String,String> dict, Connection c) 
	throws SQLException {

		StringBuilder sb = new StringBuilder(); 
		sb.append("INSERT INTO PERSON_SCRATCH ("); 
		sb.append(HOSPITAL_NUMBER).append(", "); 
		sb.append(PATIENT_FULL_NAME).append(", ");
		sb.append(DATE_OF_BIRTH).append(","); 
		sb.append(SEX).append(","); 
		sb.append(PATIENT_ADDRESS).append(",");
		sb.append(PATIENT_DEATH_DATE).append(","); 
		// here: patient death indicator
		// here: identity unknown
		sb.append(LAST_UPDATED); 
		//sb.append("patient_death_date_time, patient_death_indicator, identity_unknown_indicator, last_update_date_time");

		// NB Need to parse HL7 timestamps to get into correct format for Postgres.
		sb.append(") VALUES (?,?,?,?,?,?,?);");

		try {
			PreparedStatement st = c.prepareStatement(sb.toString());

			String value = dict.get(HOSPITAL_NUMBER);
			if (value.equals(NULL)) {
				return false;
			}
			st.setString(1, value);

			value = dict.get(PATIENT_FULL_NAME);
			if (value.equals(NULL)) {
				st.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				st.setString(2, dict.get(PATIENT_FULL_NAME));
			}
					
			value = dict.get(DATE_OF_BIRTH);
			if (value.equals(NULL_TIMESTAMP)) {
				st.setNull(3, java.sql.Types.DATE);//st.setString(3, dict.get(DATE_OF_BIRTH));
			}
			else {
				st.setTimestamp(3, Timestamp.valueOf(value));
			}

			value = dict.get(SEX);
			if (value.equals(NULL)) {
				st.setNull(4, java.sql.Types.VARCHAR);
			}
			else {
				st.setString(4, value);
			}
			
			value = dict.get(PATIENT_ADDRESS);
			if (value.equals(NULL)) {
				st.setNull(5, java.sql.Types.VARCHAR);
			}
			else {
				st.setString(5, value);
			}

			value = dict.get(PATIENT_DEATH_DATE);
			if (value.equals(NULL_TIMESTAMP)) {
				st.setNull(6, java.sql.Types.DATE);//st.setString(6, dict.get(PATIENT_DEATH_DATE));
			}
			else {
				st.setTimestamp(6, Timestamp.valueOf(value));
			}
			value = dict.get(MESSAGE_DATE_TIME); // Should never be null
			if (value.equals(NULL_TIMESTAMP)) {
				System.out.println("** ERROR got null timestamp from IDS! **");
				// should we refuse to commit this record? Probably
				st.setNull(7, java.sql.Types.DATE);
				return false;
			}
			else {
				st.setTimestamp(7, Timestamp.valueOf(value));
			}
			
				//st.setObject/*Timestamp*/(7, LocalDate.now()/*dict.get(MESSAGE_DATE_TIME)*/); // NB w and w/o null timestamp
			//st.setTimestamp(7, Timestamp.valueOf(dict.get(MESSAGE_DATE_TIME)));
			//st.setNull(7, java.sql.Types.DATE);

			//http://www.java2s.com/Tutorials/Java/JDBC/Insert/Set_NULL_date_value_to_database_in_Java.htm

			st.execute();

			////c.commit();
		}
		catch (SQLException e) {
			System.out.println("ERROR in insert_person_UDS()");
			e.printStackTrace();
			return false;
			//c.rollback(); // raises an SQLException itself.
		}
		
		return true;
	}


	/**
	 * Insert a patient_visit record into the UDS.
	 * 
	 * @param dict - The Map of String values.
	 * @param c The current connection to the UDS.
	 */
	private static void insert_patient_visit_uds (Map<String,String> dict, Connection c) 
	throws SQLException {

		StringBuilder uds_insert = new StringBuilder("");
		uds_insert.append("INSERT INTO PATIENT_VISIT (");
		uds_insert.append(HOSPITAL_NUMBER).append(", ");
		uds_insert.append(PATIENT_CLASS).append(", ");
		// hospital_service, ");
		//uds_insert.append("readmission_indicator, 
		uds_insert.append(ADMISSION_DATE).append(", ");
		/////uds_insert.append(DISCHARGE_DATE).append(", ");
		uds_insert.append(LAST_UPDATED);
		uds_insert.append(") VALUES (?,?,?,?);");

		try {
			PreparedStatement st = c.prepareStatement(uds_insert.toString());
			String value = dict.get(HOSPITAL_NUMBER);
			if (value.equals(NULL)) {
				// Not sure what to do about this.
				// We could try parsing the HL7 message but Atos should arlready have done this.
				// Maybe we shouldn't bother storing this record?
				st.setNull(1, java.sql.Types.VARCHAR);
				// I think we should not commit this record  but bail out. See #21
			} 
			else {
				st.setString(1, value);
			}

			value = dict.get(PATIENT_CLASS);
			if (value.equals(NULL)) {
				st.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				st.setString(2, value);
			}

			value = dict.get(ADMISSION_DATE);
			if (value.equals(NULL_TIMESTAMP)){
				st.setNull(3, java.sql.Types.DATE);
			}
			else {
				st.setTimestamp(3, Timestamp.valueOf(value));
			}

			value = dict.get(PERSIST_DATE_TIME);
			if (value.equals(NULL_TIMESTAMP)) {
				// should never happen
				st.setNull(4, java.sql.Types.DATE);
			}
			else {
				st.setTimestamp(4, Timestamp.valueOf(value));
			}

			st.execute();
			// st.commit();
		}
		catch (SQLException e) {
			System.out.println("ERROR in insert_person_UDS()");
			e.printStackTrace();
			//c.rollback(); // raises an SQLException itself.
		}


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

		String hospnum = dict.get(HOSPITAL_NUMBER);
		if (hospnum.equals(NULL)) {
			return 0;
		}
													
		
		StringBuilder sb = new StringBuilder();
		//select visitid, dischargedate from patient_visit where hospitalnumber = '94006000';
		sb.append("select visitid, dischargedate from PATIENT_VISIT ");
		sb.append("where ").append(HOSPITAL_NUMBER).append(" = ? ;");

		PreparedStatement st = c.prepareStatement(sb.toString());
		st.setString(1, hospnum);

		ResultSet rs = st.executeQuery();
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
	 * visitid | hospitalnumber | patientclass |  
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
		//if (msgtype.equals("ADT^A02")) { // transfer
			//sb.append(", patientlocation = ").append(dict.get(PATIENT_LOCATION));
		//}
		if (msgtype.equals("ADT^A03")) { // discharge - NB does this have a new location too?
			String ddate = dict.get(DISCHARGE_DATE);
			/*if (ddate.equals(NULL_TIMESTAMP)) {
				sb.append(", dischargedate = ").append(ddate);
			}
			else {*/
				sb.append(", dischargedate = '").append(ddate).append("'");
			//}
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
	 * Insert new record and/or update existing record in BEDVISIT table.
	 * 
	 * @param c Current connection to UDS
	 * @param dict The Map of values
	 * @param visitid Primary key in patient_visit table
	 * @return false if unable to update, true otherwise
	 * @throws SQLException
	 * 
	 */
	private static boolean update_bedvisit_table(Connection c, Map<String,String> dict, long visitid) 
	throws SQLException {
		
		Statement st = c.createStatement();

		// If admit message, add new entry to table; start time but no end time
		// If discharge message, add end time to existing entry.
		// If transfer message, add end time to existing entry and start time to a new entry.
		String msgtype = dict.get(MESSAGE_TYPE);
		if (msgtype.equals(NULL)) {
			return false;
		}

		// bed_visit_id | patient_visit_id | location | start_time | end_time
		StringBuilder sb = new StringBuilder(100);

		//boolean ret;

		// For admit messages we assume there is no current BEDVISIT entry for this patient.
		// is that a safe assumption? What if two admit messages come through in
		// quick succession?
		if (msgtype.equals("ADT^A01")) { // We don't insert an end_time.
			sb.append("INSERT INTO BEDVISIT (PATIENT_VISIT_ID, LOCATION, START_TIME) VALUES(");
			sb.append(visitid).append(", ");
			String value = dict.get(PATIENT_LOCATION);
			if (value.equals(NULL)) {
				return false;
			}
			sb.append("'").append(value).append("',");

			value = dict.get(ADMISSION_DATE);
			if (value.equals(NULL_TIMESTAMP)) {
				return false;
			}
			sb.append("'").append(value).append("'");
			sb.append(");");

			System.out.println("** DEBUG - " + sb.toString());
			st.executeUpdate(sb.toString());

		}
		// Transfer or discharge:
		else if (msgtype.equals("ADT^A02") || msgtype.equals("ADT^A03")) {
			long current_bedvisit_id = 0;
			String current_location = "";
			String new_location = dict.get(PATIENT_LOCATION);
	
			// In theory there should only be one currently-open bedvisit,
			// but we select all just in case and then just take the ID of the latest one.
			sb.append("SELECT bed_visit_id, location FROM BEDVISIT WHERE patient_visit_id = '");
			sb.append(visitid).append("' AND end_time IS NULL ORDER BY bed_visit_id DESC;" );
			ResultSet rs = st.executeQuery(sb.toString());
			if (rs.next()) {
				// Take the first (i.e. most recent) one.
				current_bedvisit_id = rs.getLong("bed_visit_id");
				current_location = rs.getString("location");
				//System.out.println("** DEBUG: current_bedvisit_id = " + current_bedvisit_id);
			}
			
			// We only update if the new location differs from the existing one.
			// Sometimes transfer messages arise when just the consultant has changed, not the bed.
			// But if it's a discharge message we do want to update the table regardless.
			if ( msgtype.equals("ADT^A03") || ! new_location.equals(current_location) ) { 
				sb.setLength(0);
				sb.append("UPDATE BEDVISIT "); // We assume here it is never null. Dangerous?
				sb.append("set end_time = '").append(dict.get(MESSAGE_DATE_TIME)).append("' ");
				sb.append("WHERE bed_visit_id = '").append(current_bedvisit_id).append("';");

				// If it's a transfer we also need to create a new BEDVISIT entry (no end time):
				if (msgtype.equals("ADT^A02") && visitid > 0) {
					sb.append("INSERT INTO BEDVISIT (PATIENT_VISIT_ID, LOCATION, START_TIME) VALUES(");
					sb.append(visitid).append(", ");
					sb.append("'").append(new_location /*dict.get(PATIENT_LOCATION)*/).append("',");
					sb.append("'").append(dict.get(MESSAGE_DATE_TIME)).append("'");
					sb.append(");");
				}

				st.executeUpdate(sb.toString());
			}

		}
		
		/*int ret = 0;
		ret = */ //st.executeUpdate(sb.toString());
		return true;
		
	}
	

	/**
	 * Update an existing PERSON_SCRATCH record in UDS. 
	 * <p>
	 * Not yet implemented. This could well be quite complicated. Issue #9
	 * 
	 * @param c Current connection to UDS
	 * @throws SQLException
	 */
	private static void update_person_record(Connection c) throws SQLException {


	}


	/**
	 * See if this hospital_number is already in the PERSON_SCRATCH table
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

		//System.out.println("** DEBUG - already_in_person_table()");

		String hospnum = dict.get(HOSPITAL_NUMBER);
		if (hospnum.equals(NULL)) {
			return false;
		}

		StringBuilder query = new StringBuilder("select * from PERSON_SCRATCH where ");
		query.append(HOSPITAL_NUMBER).append(" = ? ;");
	
		PreparedStatement st = c.prepareStatement(query.toString());
		st.setString(1, hospnum);
		//System.out.println("QUERY: " + query.toString());
		ResultSet rs = st.executeQuery();
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
	 * @return The last timestamp, or NULL_TIMESTAMP if not available.
	 * @throws SQLException
	 */
	private static String get_last_timestamp_of_person(Connection c, Map<String,String> dict) throws SQLException {

		//System.out.println("** DEBUG - get_last_timestamp_of_person fn");

		String hospnum = dict.get(HOSPITAL_NUMBER);
		if (hospnum.equals(NULL)) {
			return NULL_TIMESTAMP;
		}

		Statement st = c.createStatement();
		StringBuilder query = new StringBuilder("select ");
		query.append(LAST_UPDATED).append(" from PERSON_SCRATCH where ").append(HOSPITAL_NUMBER).append(" = '");
		query.append(dict.get(HOSPITAL_NUMBER)).append("';");
		System.out.println("QUERY: " + query.toString());
		ResultSet rs = st.executeQuery(query.toString());

		String timestamp = NULL_TIMESTAMP;
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
