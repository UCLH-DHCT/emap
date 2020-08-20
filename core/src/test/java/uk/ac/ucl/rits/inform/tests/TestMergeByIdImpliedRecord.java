package uk.ac.ucl.rits.inform.tests;

/**
 * Merge two patients, none of which was previously known about.
 *
 * @author Jeremy Stein
 */
public class TestMergeByIdImpliedRecord extends TestMergeById {

    public TestMergeByIdImpliedRecord() {
        interchangeMessages.clear();
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A40.yaml", "0000000042"));
    }

}
