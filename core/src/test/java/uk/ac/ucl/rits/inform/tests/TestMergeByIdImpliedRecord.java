package uk.ac.ucl.rits.inform.tests;

/**
 * Merge two patients, none of which was previously known about.
 *
 * @author Jeremy Stein
 */
public class TestMergeByIdImpliedRecord extends TestMergeById {

    public TestMergeByIdImpliedRecord() {
        hl7StreamFileNames.clear();
        hl7StreamFileNames.add("GenericAdt/A40.txt");
    }

}
