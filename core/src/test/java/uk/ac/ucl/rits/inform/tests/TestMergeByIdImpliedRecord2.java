package uk.ac.ucl.rits.inform.tests;

/**
 * Merge two patients, only one of which was previously known about.
 *
 * @author Jeremy Stein
 */
public class TestMergeByIdImpliedRecord2 extends TestMergeById {

    public TestMergeByIdImpliedRecord2() {
        interchangeMessages.clear();
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A01_b.yaml", "0000000042"));
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A40.yaml", "0000000042"));
    }

}
