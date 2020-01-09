package uk.ac.ucl.rits.inform.tests;

/**
 * Merge two patients, only one of which was previously known about.
 *
 * @author Jeremy Stein
 */
public class TestMergeByIdImpliedRecord2 extends TestMergeById {

    public TestMergeByIdImpliedRecord2() {
        hl7StreamFileNames.clear();
        hl7StreamFileNames.add("GenericAdt/A01_b.txt");
        hl7StreamFileNames.add("GenericAdt/A40.txt");
    }

}
