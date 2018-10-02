/*
	export CLASSPATH=.:postgresql-42.2.5.jar
	javac JDBCTest.java
	java JDBCTest
*/


import java.sql.*; // Uses postgresql-42.2.5.jar driver
//import org.postgresql;

public class JDBCTest {

	public static void main(String[] args) {
		System.out.println("Trying to connect");
		/*try {
			Class.forName("org.postgresql.Driver");
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}*/

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
		query.append("MessageType, MessageVersion ");
		query.append(" FROM TBL_IDS_MASTER;");

		StringBuilder patient_name = new StringBuilder("J. Doe"); // NB StringBuilder is not thread-safe.
		String dob = "unknown", hospital_number = "";
		char patient_class;
		String location, admit_date, discharge_date, msg_type, msg_version;

		// Extraction of data from IDS step. No HL7 parsing required.
		try {
			conn = DriverManager.getConnection(ids_url);
			st = conn.createStatement();
			rs = st.executeQuery(query.toString());
			while (rs.next())
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
			}
			rs.close();
			st.close();	
			conn.close();
			query.delete(0, query.length()); // Clear query StringBuilder.
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		// Now we write the data to the UDS.
		query.append("INSERT INTO PERSON (");
		query.append("set_ID, patient_ID_list, patient_name, birth_date_time, sex, patient_address, ");
		query.append("patient_death_date_time, patient_death_indicator, identity_unknown_indicator, last_update_date_time");

		// NB May need to parse timestamps to get into correct format for Postgres.
		query.append(") VALUES (");
		query.append("NULL,"); // set_ID
		query.append(hospital_number).append(", ");
		query.append("'").append(patient_name).append("', ");	
		query.append("'").append(/*dob*/ "20090304").append("', "); // example: 2018-10-01 14:57:41.090449
		query.append("'").append("U").append("', "); // sex unknown without parsing HL7
		query.append("'").append("Address Unknown").append("', "); // address unknown without parsing HL7
		query.append("NULL").append(", "); // patient death time unknown without parsing HL7 (PID-29)
		query.append("''").append(", "); // patient death indicator unknown without parsing HL7 (PID-30)
		query.append("''").append(", "); // identity unknown unknown without parsing HL7 (PID-31)
		query.append("NULL")/*.append(", ")*/; // last update date/time unknown without parsing HL7 (PID-33)

		query.append(");");

		System.out.println("******* insert statement: *****");
		System.out.println(query.toString());


		try {
			conn = DriverManager.getConnection(uds_url);
			st = conn.createStatement();
			int ret = st.executeUpdate(query.toString());
			String res = new String("return value was " + ret);
			System.out.println(res);
			//rs = st.executeQuery(query.toString());
			/*while (rs.next())
			{
		
			}*/
			//rs.close();
			st.close();	
			conn.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}	

}
