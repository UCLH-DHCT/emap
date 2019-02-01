# HL7_java

This contains two related sets of code, both to do with HL7 messages.

Consumer.java and Engine.java parse HL7 messages using the open-source HAPI parser. They handle
possibly different versions of HL7 by using HAPI's ability to set them to the same version (2.7).
See  https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html

HL7Processor.java (formerly JDBCTest.java) was originally written (but sadly never used) for the internal D-Day (14/11/18). It pulls
data from the IDS, but for the demo did not perform any HL7 parsing as some of that was already
been done by Atos and relevant data fields extracted to IDS columns. However, moving forward we may well be extracting
all the data items ourselves by parsing. HL7Processor then updates the UDS database.

These need to be combined in some way, e.g. HL7Processor will have to do some HL7 parsing to extract
certain data items not in the IDS (such as a patient's address). We also need to use JPA
instead of JDBC.  

Basic instructions for running the code are in [instructions.md](./instructions.md).  
