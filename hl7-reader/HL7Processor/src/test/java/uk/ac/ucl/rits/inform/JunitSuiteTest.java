package uk.ac.ucl.rits.inform;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
   TestConvertTimestamp.class,
   TestNull.class,
   TestJson.class
})

public class JunitSuiteTest {   
}
