/*
	export CLASSPATH=.:postgresql-42.2.5.jar:json-simple-1.1.1.jar
	javac JDBCTest.java
	java JDBCTest


	psql INFORM_SCRATCH
	select p.hospital_number, p.patient_name, pv.assigned_location 
	from person p, patient_visit pv 
	where p.hospital_number = pv.hospital_number;

	Typical location is T11S^B11S^T11S-32

	select latest from last_unid_processed ;
	INSERT INTO last_unid_processed (latest) values(1);
	select latest from last_unid_processed ;
	INSERT INTO last_unid_processed (latest) values(200);
	delete from last_unid_processed where latest='1';

	Need to add logic to stop person and visit records being added to db if already there.
	But of course we need to update the records.
	Also a main loop and sleep

*/


import java.sql.*; // Uses postgresql-42.2.5.jar driver


public class JDBCTest {

	//private static long last_unid_processed_this_time = 0; // Last UNID from IDS read and processed successfully.
	private static long last_unid_processed_last_time = 0; // Last UNID currently stored in UDS


	// TODO: break this up a bit
	public static void main(String[] args) {

		//convert_timestamp("200902110022"); //2009-02-11 00:22:00
		//System.exit(1);
		/* // Timestamp conversion tests
		convert_timestamp("20181003141807.7618");
		convert_timestamp("20181003141807");
		convert_timestamp("201810031418");
		convert_timestamp("2018100314");
		convert_timestamp("20181003");
		convert_timestamp("2018-10-03 14:18:07.7618");
		System.exit(1);
		*/


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
		String ids_url = "jdbc:postgresql://localhost/DUMMY_IDS"; // IDS (dummy)
		String uds_url = "jdbc:postgresql://localhost/INFORM_SCRATCH"; // UDS (dummy)
						// jdbc:postgresql:INFORM_SCRATCH

		// Extraction of data from IDS step. No HL7 parsing required.
		try {

			System.out.println("Trying to connect");
			Connection uds_conn = DriverManager.getConnection(uds_url);

			// Testing
			//lready_in_person_table(uds_conn, "94006000");
			//already_in_person_table(uds_conn, "Fred Bloggs");
			//System.exit(1);



			last_unid_processed_last_time = read_last_unid_from_UDS(uds_conn);
			System.out.println("AT START, LAST UNID STORED = " + last_unid_processed_last_time);

			//System.exit(1);


			conn = DriverManager.getConnection(ids_url);
			st = conn.createStatement();

			// Build the query - select all messages later than last_unid_processed_last_time:
			StringBuilder query = new StringBuilder("SELECT UNID, PatientName, PatientMiddleName, PatientSurname, ");
			query.append("DateOfBirth, HospitalNumber, PatientClass, PatientLocation, AdmissionDate, DischargeDate,");
			query.append("MessageType, MessageVersion, MessageDateTime ");
			query.append(" FROM TBL_IDS_MASTER ");
			query.append(" where unid > ").append(last_unid_processed_last_time).append(";");
	
			long latest_unid_processed = 0;
			StringBuilder uds_insert = new StringBuilder("");

			rs = st.executeQuery(query.toString()); // move below

			Statement uds_st = uds_conn.createStatement();
			long latest_unid_read_this_time = 0; // last one read from IDS this time

			while (rs.next()) // Iterate over records received from the query (if any)
			{

				// TO DO: be able to handle null values of all which are allowed to be null

				latest_unid_read_this_time = rs.getLong("UNID"); // use below if all successful.

				// Dummy values in case we get empty data returns. Plus defaults as we aren't parsing the HL7 messages in this demo.
				StringBuilder patient_name = new StringBuilder("J. Doe"); // NB StringBuilder is not thread-safe.
				String dob = "unknown", hospital_number = "", sex = "U"; // sex unknown without parsing HL7
				String address = "Address unknown"; // address unknown without parsing HL7
				String deathtime = "null::timestamp"; //"NULL"; // patient death time unknown without parsing HL7 (PID-29)
				char patient_class;
				String location, admit_date, discharge_date, msg_type, msg_version, msg_date_time;
			
				patient_name = new StringBuilder(rs.getString("PatientName"));
				patient_name.append(" ").append(rs.getString("PatientMiddleName"));
				patient_name.append(" ").append(rs.getString("PatientSurname"));
				System.out.println(patient_name.toString());

				dob = new String(rs.getString("DateOfBirth"));
				hospital_number = new String(rs.getString("HospitalNumber"));
				patient_class = (new String(rs.getString("PatientClass"))).charAt(0);
				location = new String(rs.getString("PatientLocation"));
				admit_date = new String(rs.getString("AdmissionDate"));

				discharge_date = rs.getString("DischargeDate");
				if (rs.wasNull()) {
					discharge_date = "NULL";
				}

				msg_type = new String(rs.getString("MessageType")); // e.g. "ADT^A28"
				msg_version = new String(rs.getString("MessageVersion")); // e.g. "2.2" (HL7 version)	
				msg_date_time = new String(rs.getString("MessageDateTime"));

				
				// Now insert this record into the UDS PERSON table.

				// Check to see if this person is already in the PERSON table - obtain latest update time
				// if they are, and our update time is later than that stored, update table as appropriate.
				String person_entry_last_updated = "NULL";
				if (already_in_person_table(uds_conn, hospital_number)) {

					// obtain timestamp last updated - are we more recent?
					person_entry_last_updated = get_last_timestamp_of_person(uds_conn, hospital_number);
					System.out.println("Stored timestamp is " + person_entry_last_updated);

				}
				else { // It's a completely new PERSON entry - and possibly a new patient_visit entry?

				}

				//
				// This could go in its own function
				uds_insert.append("INSERT INTO PERSON ("); // patient_ID_list,set_ID,
				uds_insert.append("hospital_number, patient_name, birth_date_time, sex, patient_address, ");
				uds_insert.append("patient_death_date_time, last_update_date_time");
				//uds_insert.append("patient_death_date_time, patient_death_indicator, identity_unknown_indicator, last_update_date_time");
		
				// NB Need to parse HL7 timestamps to get into correct format for Postgres.
				uds_insert.append(") VALUES (");
				uds_insert.append(hospital_number).append(", ");
				//uds_insert.append("NULL,"); // set_ID
				uds_insert.append("'").append(patient_name).append("', ");
				dob = convert_timestamp(dob);	
				uds_insert.append("'").append(dob /*"20090304"*/).append("', "); // example: 2018-10-01 14:57:41.090449
				uds_insert.append("'").append(sex).append("', "); // sex unknown without parsing HL7
				uds_insert.append("'").append(address).append("', "); // address unknown without parsing HL7
				uds_insert/*.append("'")*/.append(deathtime)/*.append("'")*/.append(", "); // patient death time unknown without parsing HL7 (PID-29)
				//uds_insert.append("''").append(", "); // patient death indicator unknown without parsing HL7 (PID-30)
				//uds_insert.append("''").append(", "); // identity unknown unknown without parsing HL7 (PID-31)
				msg_date_time = convert_timestamp(msg_date_time);
				uds_insert.append("'").append(msg_date_time).append("'")/*.append(", ")*/; // last update date/time unknown without parsing HL7 (PID-33)	
				uds_insert.append(");");

		
				// Now we write the PERSON data to the UDS (clears uds_insert)
				write_update_to_database(uds_insert, uds_st);
		
				// Prepare the statement to insert data into the PATIENT_VISIT table
				// Could move this to its own function as well.
				uds_insert.append("INSERT INTO PATIENT_VISIT (");
				uds_insert.append("hospital_number, patient_class, assigned_location, hospital_service, ");
				uds_insert.append("readmission_indicator, admit_datetime, discharge_date_time, last_updated");
				uds_insert.append(") VALUES (");
				uds_insert.append(hospital_number).append(", ");
				uds_insert.append("'").append(patient_class).append("'").append(", ");
				uds_insert.append("'").append(location).append("'").append(", ");
				uds_insert.append("NULL").append(", "); // service
				uds_insert.append("NULL").append(", "); // readmission_indicator
				uds_insert.append("'").append(convert_timestamp(admit_date)).append("'").append(", ");
				uds_insert/*.append("'")*/.append(convert_timestamp(discharge_date))/*.append("'")*/.append(", ");
				uds_insert.append("'").append(msg_date_time).append("'"); // already converted
				uds_insert.append(");");

				// Now we write the PATIENT_VISIT data to the UDS (clears uds_insert)
				write_update_to_database(uds_insert, uds_st);

			} // end (while)

			if (latest_unid_read_this_time > last_unid_processed_last_time) {
				//latest_unid_processed = latest_unid_read_this_time;
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


	// See if this hospital_number is already in the PERSON table
	// NB does that necssarily mean they will also be in the patient_visit table? No they might
	// be an outpatient. And if they ARE in the patient_visit table, they might be there
	// multiple times. TODO
	// We could add a current location field to person?
	private static boolean already_in_person_table(Connection c, String hospital_number) throws SQLException {

		Statement st = c.createStatement();
		StringBuilder query = new StringBuilder("select * from person where hospital_number = '");
		query.append(hospital_number).append("';");
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


	// Get the last time this person's record in the UDS was updated.
	// In theory it should never be null but we take a safe approach.
	private static String get_last_timestamp_of_person(Connection c, String hospital_number) throws SQLException {

		Statement st = c.createStatement();
		StringBuilder query = new StringBuilder("select last_update_date_time from person where hospital_number = '");
		query.append(hospital_number).append("';");
		System.out.println("QUERY: " + query.toString());
		ResultSet rs = st.executeQuery(query.toString());

		String timestamp = "NULL";
		if ( rs.next() ) {
			timestamp = rs.getString("last_update_date_time");
		}
		return timestamp;

	}


	// Obtain the UNID most recently (last time) read from the IDS and processed;
	// this is stored in the UDS.
	private static long read_last_unid_from_UDS (Connection c) throws SQLException {

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


	// First we write new latest UNID processed. Then we delete the existing one.
	// NB what if they are the same?
	/* INSERT INTO last_unid_processed (latest) values(200);
	delete from last_unid_processed where latest='1';
	write_last_unid_to_UDS(uds_conn, latest_unid_read_this_time, last_unid_processed_last_time);*/
	private static int write_last_unid_to_UDS(Connection c, 
				long latest_unid_read_this_time,
				long unid_processed_last_time) throws SQLException {

		// Bail out if it's the trivial case.
		if (latest_unid_read_this_time == unid_processed_last_time) {
			return 0;
		}

		// If we get here someone probaboky needs to manually reset the UDS UNID table to 0
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


	// Obtain the latest timestamp stored in the UDS. Then we know that data up to that
	// point was written OK and so we can extract new info from the IDS starting with that timestamp.
	// NB Ideally it would be immediately AFTER the timestamp but as messages only have a timestamp
	// precision to the nearest second we might find we miss messages if we jump to the next second.
	// Not very likely but best to be safe.
	//
	// NB now Ashish says: "IT could be with missing seconds for some fields and only date for some fields. 
	// The idea is to convert the format received from Carecast to postgres database and insert it in IDS database."
	// So this approach (check latest UDS time before querying IDS) may not work.
	//
	// NB obviously at start up the UDS will be empty so we expect a null result
	/*private static String obtain_latest_UDS_time (Connection c) throws SQLException {

		// Test using sample tables in test db
		// select persist_date_time from cities order by persist_date_time desc limit 1;
		// probably better: select max(persist_date_time) from cities;

		Statement st = c.createStatement();
		String query = //"SELECT MAX(last_update_date_time) from PERSON;";
			"select max(persist_date_time) from cities;";
		ResultSet rs = st.executeQuery(query);
		String latest_timestamp = "null";

		while (rs.next()) // Iterate over records. If there aren't any the timestamp is "null". (Tested with empty table)
		{
			latest_timestamp = rs.getString(1);
			break;
		}
		System.out.println ("LATEST UDS TIMESTAMP: " + latest_timestamp);

		return latest_timestamp;
	}*/


	///////////////////////////////////////////
	//
	// write_update_to_database()
	// 
	// ARGS: StringBuilder b (the SQL), Statement s
	// 
	// Performs write to database. Side-effect: reset builder to empty.
	//
	///////////////////////////////////////////
	private static int write_update_to_database (StringBuilder b, Statement s) {

		System.out.println("******* statement: *****");
		System.out.println(b.toString());
		int ret = 0;
		try {
			ret = s.executeUpdate(b.toString());
			//String res = new String("return value was " + ret);
			//System.out.println(res);

			// If we reach here it is likely our update was processed OK.
		}
		catch (SQLException e) {
			e.printStackTrace();
			// should we add a rollback here?
		}
		finally {
			// Reset StringBuilder
			b.delete(0, b.length());
		}

		return ret;
	}


	///////////////////////////////////////////
	//
	// convert_timestamp()
	//
	// ARGS: String hl7: an HL7 format timestamp e.g. "20181003141807.7618"
	//
	// Returns: Postgres-format timestamp e.g. "2018-10-03 14:18:07.7618"
	// NB some HL7 timestamps won't have the decimal part. And some will only
	// be accurate to the day (so no hhmmss information)
	//
	// Unfortunately, Ashish says the precision is the same as what he sent me 
	// in the test messages, which is to the nearest second. However we don't
	// worry about that in this method.
	//
	///////////////////////////////////////////
	private static String convert_timestamp (String hl7) {

		System.out.println(hl7);

		// If it's NULL return NULL
		//if (hl7.equals("NULL")) return "NULL";
		if (hl7 == null || hl7.equals("") || hl7.equals("NULL")) return "NULL";

		// First make sure this is not already in Postgres format (sanity check):
		String[] test = hl7.split("-");
		if (test.length >= 3) return hl7; // Assumes year, month and day all present!

		// HL7 timestamp format is YYYYMMDDHHMMSS[.S[S[S[S]]]] (with +/- offset if required)
		String[] bigparts = hl7.split("\\."); // We have to escape the period character.
		//for (String a : bigparts) 
				//System.out.println(a); 
		int len = bigparts.length; //System.out.println(len);
		String firstpart = bigparts[0];
		///System.out.println(firstpart);
		String year = firstpart.substring(0,4); //System.out.println(year);
		String month = firstpart.substring(4,6); //System.out.println(month);
		String day = firstpart.substring(6,8); //System.out.println(day);

		// Deal with hhmmss (or lack of them)
		String hours = "00";
		String minutes = "00";
		String seconds = "00";
		//String fractional = "0000";
		int stringlen = firstpart.length();
		if (stringlen >= 10) {
			hours = firstpart.substring(8,10); //System.out.println(hours);
		}
		if (stringlen >= 12) {
			minutes = firstpart.substring(10,12); //System.out.println(minutes);
		}
		if (stringlen >= 14) {
			seconds = firstpart.substring(12,14); //System.out.println(seconds);
		}

		// Deal with any fractional parts of a second.
		/*
		String secondpart = bigparts[1];
		stringlen = secondpart.length();
		if (stringlen >= 1) {
			fractional = secondpart;
			System.out.print ("frac:");
			System.out.print(fractional);
		}*/

		StringBuilder postgres = new StringBuilder(year);
		postgres.append("-").append(month);
		postgres.append("-").append(day);
		postgres.append(" ").append(hours);
		postgres.append(":").append(minutes);
		postgres.append(":").append(seconds);
		//postgres.append(".").append(fractional);

		if (len > 1) { // Decimal part exists. NB we don't perform rounding.
			String secondpart = bigparts[1];
			postgres.append(".").append(secondpart);
		}

		String result = postgres.toString(); 
		System.out.println(result); System.out.println("************");

		return result;

	}
}
