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
		StringBuilder query = new StringBuilder("SELECT PatientName, PatientMiddleName, PatientSurname, ");
		query.append("DateOfBirth, HospitalNumber, PatientClass, PatientLocation, AdmissionDate, DischargeDate,");
		query.append("MessageType, MessageVersion ");
		query.append(" FROM TBL_IDS_MASTER;");
		try {
			conn = DriverManager.getConnection(ids_url);
			st = conn.createStatement();
			rs = st.executeQuery(query.toString());
			while (rs.next())
			{
				StringBuilder patient_name = new StringBuilder(rs.getString(1));
				patient_name.append(" ").append(rs.getString(2));
				patient_name.append(" ").append(rs.getString(3));
				System.out.println(patient_name.toString());
			}
			rs.close();
			st.close();	
			conn.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
	}	

}
