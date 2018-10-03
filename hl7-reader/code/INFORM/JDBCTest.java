/*
	export CLASSPATH=.:postgresql-42.2.5.jar
	javac JDBCTest.java
	java JDBCTest
*/


import java.sql.*; // Uses postgresql-42.2.5.jar driver

public class JDBCTest {

	public static void main(String[] args) {

		/*
		convert_timestamp("20181003141807.7618");
		convert_timestamp("20181003141807");
		convert_timestamp("201810031418");
		convert_timestamp("2018100314");
		convert_timestamp("20181003");
		convert_timestamp("2018-10-03 14:18:07.7618");
		System.exit(1);
		*/

		System.out.println("Trying to connect");

		String url = /*jdbc:postgresql:INFORM_SCRATCH";*/ 
				"jdbc:postgresql://localhost/INFORM_SCRATCH";
		Connection conn;
		Statement st;
		ResultSet rs;
		
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
		String ids_url = "jdbc:postgresql://localhost/DUMMY_IDS";
		String uds_url = "jdbc:postgresql://localhost/INFORM_SCRATCH";

		StringBuilder query = new StringBuilder("SELECT PatientName, PatientMiddleName, PatientSurname, ");
		query.append("DateOfBirth, HospitalNumber, PatientClass, PatientLocation, AdmissionDate, DischargeDate,");
		query.append("MessageType, MessageVersion, MessageDateTime ");
		query.append(" FROM TBL_IDS_MASTER;");

		StringBuilder patient_name = new StringBuilder("J. Doe"); // NB StringBuilder is not thread-safe.
		String dob = "unknown", hospital_number = "";
		char patient_class;
		String location, admit_date, discharge_date, msg_type, msg_version, msg_date_time;
		StringBuilder uds_insert = new StringBuilder("");

		// Extraction of data from IDS step. No HL7 parsing required.
		try {
			conn = DriverManager.getConnection(ids_url);
			st = conn.createStatement();
			rs = st.executeQuery(query.toString());

			Connection uds_conn = DriverManager.getConnection(uds_url);
			Statement uds_st = uds_conn.createStatement();
		
			while (rs.next()) // Iterate over records
			{
				patient_name = new StringBuilder(rs.getString(1));
				patient_name.append(" ").append(rs.getString(2));
				patient_name.append(" ").append(rs.getString(3));
				System.out.println(patient_name.toString());

				dob = new String(rs.getString(4));
				hospital_number = new String(rs.getString(5));
				patient_class = (new String(rs.getString(6))).charAt(0);
				location = new String(rs.getString(7));
				admit_date = new String(rs.getString(8));
				discharge_date = new String(rs.getString(9));
				msg_type = new String(rs.getString(10));
				msg_version = new String(rs.getString(11));
				msg_date_time = new String(rs.getString(12));

				// Now insert this record into the UDS PERSON table.
				// This really needs more logic i.e. do we want to use a UNID as PK?
				// Really we should check to see if this patient is already in the UDS; if
				// so, update their record.
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
		
				// Now we write the PERSON data to the UDS.
				write_update_to_database(uds_insert, uds_st);
		
				// Prepare the statement to insert data into the PATIENT_VISIT table


			}
			uds_st.close();
			uds_conn.close();
			rs.close();
			st.close();	
			conn.close();
			query.delete(0, query.length()); // Clear query StringBuilder.
		}
		catch (SQLException e) {
			e.printStackTrace();
		}	
		
	

	}	// End (main)

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
			String res = new String("return value was " + ret);
			System.out.println(res);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}

		// Reset StringBuilder
		b.delete(0, b.length());

		return ret;
	}


	///////////////////////////////////////////
	//
	// convert_timestamp();
	//
	// ARGS: String hl7: an HL7 format timestamp e.g. "20181003141807.7618"
	// Returns: Postgres-format timestamp e.g. "2018-10-03 14:18:07.7618"
	// NB some HL7 timestamps won't have the decimal part. And some will only
	// be accurate to the day (so no hhmmss information)
	//
	///////////////////////////////////////////
	private static String convert_timestamp (String hl7) {

		System.out.println(hl7);

		// First make sure this is not already in Postgres format (sanity check):
		String[] test = hl7.split("-");
		if (test.length >= 3) return hl7;

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
		if (firstpart.length() >= 10) {
			hours = firstpart.substring(8,10); //System.out.println(hours);
		}
		if (firstpart.length() >= 12) {
			minutes = firstpart.substring(10,12); //System.out.println(minutes);
		}
		if (firstpart.length() >= 14) {
			seconds = firstpart.substring(12,14); //System.out.println(seconds);
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
		System.out.println(result); System.out.println("************");

		return result;

	}
}
