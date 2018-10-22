/*
	export CLASSPATH=.:postgresql-42.2.5.jar:json-simple-1.1.1.jar
	javac JDBCTest.java
	java JDBCTest


	psql INFORM_SCRATCH
	select p.hospital_number, p.patient_name, pv.assigned_location 
	from person p, patient_visit pv 
	where p.hospital_number = pv.hospital_number;

	Typical location is T11S^B11S^T11S-32

	JSON page https://www.geeksforgeeks.org/parse-json-java/

*/


import java.sql.*; // Uses postgresql-42.2.5.jar driver
//import org.json.simple.*;
import org.json.simple.JSONObject; 
import org.json.simple.parser.*;

import java.io.FileNotFoundException;
import java.io.FileReader; 
import java.io.PrintWriter;
import java.io.File;


public class JDBCTest {

	private static long last_unid = 0; // Last UNID from IDS read and processed successfully.

	private static String filename = "UNID.json"; 

	public static void main(String[] args) {

		/* // Timestamp conversion tests
		convert_timestamp("20181003141807.7618");
		convert_timestamp("20181003141807");
		convert_timestamp("201810031418");
		convert_timestamp("2018100314");
		convert_timestamp("20181003");
		convert_timestamp("2018-10-03 14:18:07.7618");
		System.exit(1);
		*/

		last_unid = read_last_unid_from_file();
		System.out.println("LAST UNID STORED = " + last_unid);
		 
		// do something - now last_unid is 27 - need to check written ok to db and file though
		//boolean res = write_unid_to_file(27);

		//System.exit(1);

		System.out.println("Trying to connect");

		////String url = /*jdbc:postgresql:INFORM_SCRATCH";*/ 
				////"jdbc:postgresql://localhost/INFORM_SCRATCH";
		Connection conn;
		Statement st;
		ResultSet rs;
		/*
		try {
			conn = DriverManager.getConnection(url);
			st = conn.createStatement();
			rs = st.executeQuery("SELECT * FROM weather;");
			while (rs.next())
			{
    			System.out.print("Column 1 returned ");
				System.out.println(rs.getString(1));
			}
			rs.close();
			st.close();	
			conn.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		*/

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

		StringBuilder query = new StringBuilder("SELECT UNID, PatientName, PatientMiddleName, PatientSurname, ");
		query.append("DateOfBirth, HospitalNumber, PatientClass, PatientLocation, AdmissionDate, DischargeDate,");
		query.append("MessageType, MessageVersion, MessageDateTime ");
		query.append(" FROM TBL_IDS_MASTER ");
		query.append(" where unid > ").append(last_unid).append(";");

		StringBuilder patient_name = new StringBuilder("J. Doe"); // NB StringBuilder is not thread-safe.
		String dob = "unknown", hospital_number = "";
		char patient_class;
		String location, admit_date, discharge_date, msg_type, msg_version, msg_date_time;
		long latest_unid_processed = 0;
		StringBuilder uds_insert = new StringBuilder("");

		// Extraction of data from IDS step. No HL7 parsing required.
		try {
			conn = DriverManager.getConnection(ids_url);
			st = conn.createStatement();
			rs = st.executeQuery(query.toString()); // move below

			Connection uds_conn = DriverManager.getConnection(uds_url);
			///String latest_uds_time = obtain_latest_UDS_time(uds_conn);


			Statement uds_st = uds_conn.createStatement();

			while (rs.next()) // Iterate over records
			{
				int index = 1;
				long a_unid = rs.getLong(index++);

				patient_name = new StringBuilder(rs.getString(index++)); //was 1
				patient_name.append(" ").append(rs.getString(index++));  // was 2 
				patient_name.append(" ").append(rs.getString(index++)); // etc
				System.out.println(patient_name.toString());

				dob = new String(rs.getString(index++));
				hospital_number = new String(rs.getString(index++));
				patient_class = (new String(rs.getString(index++))).charAt(0);
				location = new String(rs.getString(index++));
				admit_date = new String(rs.getString(index++));
				discharge_date = new String(rs.getString(index++));
				
				msg_type = new String(rs.getString(index++)); // e.g. "ADT^A28"
				msg_version = new String(rs.getString(index++)); // e.g. "2.2" (HL7 version)
				msg_date_time = new String(rs.getString(index++));

				// Deal with any missing timestamps (especially discharge_date)
				//if (discharge_date == null || discharge_date.equals("")) discharge_date = "NULL";
				//if (admit_date == null || admit_date.equals("")) admit_date = "NULL"; // shouldn't be needed
				//if (msg_date_time == null || msg_date_time.equals("")) msg_date_time = "NULL"; // shouldn't be needed

				// Now insert this record into the UDS PERSON table.
				// This really needs more logic i.e. do we want to use a UNID as PK?
				//
				// Really we should check to see if this patient is already in the UDS; if
				// so, update their record (based on message type)
				// But this is just a first attempt.
				uds_insert.append("INSERT INTO PERSON ("); // patient_ID_list,set_ID,
				uds_insert.append("hospital_number, patient_name, birth_date_time, sex, patient_address, ");
				uds_insert.append("patient_death_date_time, patient_death_indicator, identity_unknown_indicator, last_update_date_time");
		
				// NB May need to parse timestamps to get into correct format for Postgres.
				uds_insert.append(") VALUES (");
				uds_insert.append(hospital_number).append(", ");
				//uds_insert.append("NULL,"); // set_ID
				uds_insert.append("'").append(patient_name).append("', ");
				dob = convert_timestamp(dob);	
				uds_insert.append("'").append(dob /*"20090304"*/).append("', "); // example: 2018-10-01 14:57:41.090449
				uds_insert.append("'").append("U").append("', "); // sex unknown without parsing HL7
				uds_insert.append("'").append("Address Unknown").append("', "); // address unknown without parsing HL7
				uds_insert.append("NULL").append(", "); // patient death time unknown without parsing HL7 (PID-29)
				uds_insert.append("''").append(", "); // patient death indicator unknown without parsing HL7 (PID-30)
				uds_insert.append("''").append(", "); // identity unknown unknown without parsing HL7 (PID-31)
				msg_date_time = convert_timestamp(msg_date_time);
				uds_insert.append("'").append(msg_date_time/*"NULL"*/).append("'")/*.append(", ")*/; // last update date/time unknown without parsing HL7 (PID-33)	
				uds_insert.append(");");

		
				// Now we write the PERSON data to the UDS (clears uds_insert)
				write_update_to_database(uds_insert, uds_st);
		
				// Prepare the statement to insert data into the PATIENT_VISIT table
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

				latest_unid_processed = a_unid;

			}
			uds_st.close();
			uds_conn.close();
			rs.close();
			st.close();	
			conn.close();
			query.delete(0, query.length()); // Clear query StringBuilder.
			boolean res = write_unid_to_file(latest_unid_processed);

		}
		catch (SQLException e) {
			e.printStackTrace();
		}	
		
	

	}	// End (main)


	// Attempt to locate and open file stroing last UNID.
	// If file not found, or other error, set unid to 0.
	// Obviously file will not be found the first time this is run.
	private static long read_last_unid_from_file() {

		Object obj;
		try {
			obj = new JSONParser().parse(new FileReader(filename)); 
		}
		catch (Exception e) { // FileNotFoundException or IOException
			e.printStackTrace();
			System.out.println("*** DEBUG: file not found or IO exception; setting last unid stored to 0 ***");
			return 0; // do we really want to do this?
		}

		// typecasting obj to JSONObject 
		JSONObject jo = (JSONObject) obj;

		// JSON reads it as a long even though stored as an integer
		//Integer newInt = new Integer(oldLong.intValue());
		//last_unid = (int) jo.get("unid");
		//last_unid = long_last_unid.intValue(); 
		last_unid = (long) jo.get("unid");
		
		return last_unid;

	}

	private static boolean write_unid_to_file(/*int*/ long unid) {

		JSONObject jo = new JSONObject();
		jo.put("unid", unid);

		// Try and write to the file. Create it if it doesn't exist
		// (this will happen the first time)
		File f = new File(filename);
		if ( ! f.exists()) {

			try {
				f.createNewFile();
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}

		}
		if (f.exists() && !f.isDirectory()) { 
    	
			PrintWriter pw;
			try {
				pw = new PrintWriter(filename);
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;

			} 

			pw.write(jo.toJSONString()); 
			
			pw.flush(); 
			pw.close(); 			
		
		}

		return true;
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
	private static String obtain_latest_UDS_time (Connection c) throws SQLException {

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
	}


	///////////////////////////////////////////
	//
	// write_update_to_database()
	// 
	// ARGS: StringBuilder b, Statement s
	// 
	// Performs write to database. Side-effect: reset builder to empty.
	//
	///////////////////////////////////////////
	private static int write_update_to_database (StringBuilder b, Statement s) {

		System.out.println("******* insert statement: *****");
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
		if (hl7 == null || hl7.equals("")) return "NULL";

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
