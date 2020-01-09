package uk.ac.ucl.rits.inform.tests;

/**
 * Merge two patients, only one of which was previously known about.
 *
 * @author Jeremy Stein
 */
public class TestMergeByIdImpliedRecord1 extends TestMergeById {

    public TestMergeByIdImpliedRecord1() {
        hl7StreamFileNames.clear();
        hl7StreamFileNames.add("GenericAdt/A01.txt");
        hl7StreamFileNames.add("GenericAdt/A40.txt");
    }

}
