package uk.ac.ucl.rits.inform.tests;

import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Merge two patients, only one of which was previously known about.
 *
 * @author Jeremy Stein
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TestMergeByIdImpliedRecord1 extends TestMergeById {

    public TestMergeByIdImpliedRecord1() {
        hl7StreamFileNames.clear();
        hl7StreamFileNames.add("GenericAdt/A01.txt");
        hl7StreamFileNames.add("GenericAdt/A40.txt");
    }

}
