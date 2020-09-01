package uk.ac.ucl.rits.inform.tests;

/**
 * Merge two patients, only one of which was previously known about.
 *
 * @author Jeremy Stein
 */
public class TestMergeByIdImpliedRecord1 extends TestMergeById {

    public TestMergeByIdImpliedRecord1() {
        interchangeMessages.clear();
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A01.yaml", "0000000042"));
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A40.yaml", "0000000042"));
    }

}
