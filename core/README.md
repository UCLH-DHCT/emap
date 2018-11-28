# HL7_java

This contains two related sets of code, both to do with HL7 messages.

Consumer.java and Engine.java parse HL7 messages using the open-source HAPI parser. They handle
possibly different versions of HL7 by using HAPI's ability to set ll to the same version (2.7).
See  https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html

JDBCTest.java is a separate program. It was written for the internal D-Day (14/11/18). It pulls
data from the IDS, but for the demo does not perform any HL7 parsing as some of that has already
been done by Atos and relevant data fields extracted to IDS columns. It then updates the UDS database.

These need to be combined in some way, e.g. JDBCTest will have to do some HL7 parsing to extract
certain data items not in the IDS (such as a patient's address). We also need to use JPA
instead of JDBC.  
  