import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


public class TestRunner {
   public static void main(String[] args) {
      Result result = JUnitCore.runClasses(JunitTestSuite.class/*TestHL7Processor.class*/);
		
      for (Failure failure : result.getFailures()) {
         System.out.println("FAILED: " + failure.toString());
      }
		
      if (result.wasSuccessful()) {
         System.out.println("\nALL TESTS PASSED\n");
      }
      else {
         System.out.println("\nAT LEAST ONE TEST ** FAILED **\n");
      }

   }
}  
