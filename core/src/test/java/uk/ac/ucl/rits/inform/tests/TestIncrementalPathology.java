package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * Messages come through giving progressively more results.
 * New results should be added taking care to avoid duplicating existing ones.
 * @author Jeremy Stein
 */
public class TestIncrementalPathology extends InterchangeMessageEndToEndTestCase {
    public TestIncrementalPathology() {
        super();
        interchangeMessages.addAll(messageFactory.getPathologyOrders("incremental.yaml", "0000000042"));
    }

    @Test
    @Transactional
    public void testResultsExist() {
        System.out.println(emapStarTestUtils.prettyPrintEncounterFacts("123412341234"));

        List<PatientFact> order001 = patientFactRepo.findAllPathologyOrdersByOrderNumber("94000001");
        List<PatientFact> childFacts001 = order001.get(0).getChildFacts();
        List<PatientFact> order002 = patientFactRepo.findAllPathologyOrdersByOrderNumber("94000002");
        List<PatientFact> childFacts002 = order002.get(0).getChildFacts();

        // check that pathology results have not been duplicated
        List<PatientFact> allTestResults001 = childFacts001.stream().filter(pf -> pf.isValid() && pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        List<PatientFact> allTestResults002 = childFacts002.stream().filter(pf -> pf.isValid() && pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        assertEquals(2, allTestResults001.size());
        assertEquals(17, allTestResults002.size());
    }

    /**
     * Given that two messages give the monocyte result, the result from the last message to be encountered should be used
     */
    @Test
    @Transactional
    public void testResultsUpdate() {
        List<PatientFact> orders002 = patientFactRepo.findAllPathologyOrdersByOrderNumber("94000002");
        assertEquals(1, orders002.size());
        List<PatientFact> resultFacts = orders002.get(0).getChildFacts();
        Map<String, List<PatientFact>> resultFactsByTestCode = resultFacts.stream()
                .filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT))
                .collect(Collectors.groupingBy(
                        pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_CODE)
                                .get(0).getValueAsString()));
        List<PatientFact> monocyteResults = resultFactsByTestCode.get("MO");
        assertEquals(3, monocyteResults.size());

        Instant expectedOld = Instant.parse("2019-07-16T23:04:00Z");
        Instant expectedNew = Instant.parse("2019-07-16T23:05:00Z");
        List<PatientProperty> allProperties = monocyteResults.stream().map(mr -> mr.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_NUMERIC_VALUE).get(0)).collect(Collectors.toList());
        // first message has result of 0.35, second message has a result of 0.5 so this should be used
        emapStarTestUtils._testPropertyValuesOverTime(allProperties, 0.35, 0.5, expectedOld, expectedNew, expectedNew);

    }
}
