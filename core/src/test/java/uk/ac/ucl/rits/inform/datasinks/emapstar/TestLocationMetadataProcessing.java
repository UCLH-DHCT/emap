package uk.ac.ucl.rits.inform.datasinks.emapstar;

public class TestLocationMetadataProcessing extends MessageProcessingBase {

    // LOCATION
    /**
     * Given no locations exist in database
     * when a location metadata message is processed
     * then a new location should be created
     */

    /**
     * Given location alreay exists in database
     * when a location metadata message is processed with the same string
     * no new locations should be created
     */

    // DEPARTMENT
    /**
     * Given no departments exist in database
     * when a location metadata message is processed
     * then a new department and department state should be created
     */

    /**
     * Given department exists in database
     * when a location metadata message with the same department CSN (and same data) is processed
     * then there should be no changes to the department tables
     */

    /**
     * Given department exists in database
     * when a location metadata message with the same department CSN (and different data) is processed
     * then the department state should be updated
     */

    /**
     * Given department exists in database
     * when a location metadata message with matching hl7 string (different CSN and later time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given department exists in database
     * when a location metadata message with matching hl7 string (different CSN and same time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given department exists in database
     * when a location metadata message with matching hl7 string (different CSN and earlier time) is processed
     * then the previous temporal until data should be updated, processed message temporal until data should be set to the next message from times.
     */

    // ROOM
    /**
     * Given no rooms exist in database
     * when a location metadata message with room is processed
     * then a new room and room state should be created
     */

    /**
     * Given no beds exist in database
     * when a location metadata message with bed is processed
     * then a new bed and bed state should be created
     */

    /**
     * Given no beds exist in database
     * when two location metadata messages for pool beds are processed
     * a new pool bed should be created and the pool count should be 2
     */

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and later time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and same time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and earlier time) is processed
     * then the previous temporal until data should be updated, processed message temporal until data should be set to the next message from times.
     */

    /**
     * Given pool bed exist in database
     * when two location metadata messages for existing pool beds at a different contact time are processed
     * a new pool bed should be created and the pool count should be 2
     */


    // ROOM FACILITY
    /**
     * Given no beds exist in database
     * when a location metadata message with bed facility is processed
     * then a new bed facility should be created
     */

    /**
     * Given no beds exist in database
     * when two location metadata messages for the same CSN are processed with different bed facilities
     * then two new bed facilities should be created
     */



}
