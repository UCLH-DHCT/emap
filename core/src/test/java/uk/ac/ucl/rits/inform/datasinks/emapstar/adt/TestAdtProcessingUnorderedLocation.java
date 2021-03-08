package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent.ShuffleIterator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class TestAdtProcessingUnorderedLocation extends MessageProcessingBase {
    private String[] adtFilenames;

    @Autowired
    private OrderPermutationTestProducer orderPermutationTestProducer;

    /**
     * Presentation, admit, move A- B -A -B, discharge
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    Stream<DynamicTest> testUnorderedMoves() {
        adtFilenames = new String[]{"02_A01", "03_A02", "04_A02", "05_A02", "06_A02", "07_A06", "08_A03"};
        orderPermutationTestProducer.setMessagePath("Location/Moves");
        orderPermutationTestProducer.setInitialAdmissionTime(Instant.parse("2013-02-11T11:00:52Z"));
        orderPermutationTestProducer.setLocations(new String[]{
                "ED^UCHED RAT CHAIR^RAT-CHAIR",
                "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
                "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
                "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
                "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
                "EAU^UCH T00 EAU BY02^BY02-08",
        });

        Iterable<List<String>> permutationIterator = new ShuffleIterator<>(List.of(adtFilenames));

        return StreamSupport.stream(permutationIterator.spliterator(), false)
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> orderPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }

    /**
     * Admit, cancel admit, admit, transfer, discharge, cancel admit
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    Stream<DynamicTest> testDuplicateCancelAdmit() {
        adtFilenames = new String[]{"01_A01", "02_A11", "03_A01", "04_A02", "05_A03"};
        List<String> duplicateCancel = new ArrayList<>(List.of(adtFilenames));
        duplicateCancel.add("02_A11");
        orderPermutationTestProducer.setMessagePath("Location/CancelAdmit");
        orderPermutationTestProducer.setInitialAdmissionTime(Instant.parse("2013-02-11T13:00:52Z"));
        orderPermutationTestProducer.setLocations(new String[]{
                "ED^UCHED RAT CHAIR^RAT-CHAIR",
                "ED^NON COVID MAJORS 05^05-NON COVID MAJORS"
        });

        Iterable<List<String>> permutationIterator = new ShuffleIterator<>(duplicateCancel);

        return StreamSupport.stream(permutationIterator.spliterator(), false)
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> orderPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }

    /**
     * admit, transfer, transfer, discharge, cancel discharge, discharge, cancel discharge
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    Stream<DynamicTest> testDuplicateCancelDischarge() {
        adtFilenames = new String[]{"01_A01", "02_A02", "03_A03", "04_A13", "05_A03"};
        List<String> duplicateCancel = new ArrayList<>(List.of(adtFilenames));
        duplicateCancel.add("04_A13");
        orderPermutationTestProducer.setMessagePath("Location/CancelDischarge");
        orderPermutationTestProducer.setInitialAdmissionTime(Instant.parse("2013-02-11T11:00:52Z"));
        orderPermutationTestProducer.setLocations(new String[]{
                "ED^UCHED RAT CHAIR^RAT-CHAIR",
                "ED^NON COVID MAJORS 05^05-NON COVID MAJORS"
        });


        Iterable<List<String>> permutationIterator = new ShuffleIterator<>(duplicateCancel);

        return StreamSupport.stream(permutationIterator.spliterator(), false)
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> orderPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }

    /**
     * admit, transfer, transfer, cancel transfer, transfer, discharge,  cancel transfer
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    Stream<DynamicTest> testUnorderedCancelTransfer() {
        adtFilenames = new String[]{"01_A01", "02_A02", "03_A02", "04_A12", "05_A02", "06_A03"};
        List<String> duplicateCancel = new ArrayList<>(List.of(adtFilenames));
        duplicateCancel.add("04_A12");
        orderPermutationTestProducer.setMessagePath("Location/CancelTransfer");
        orderPermutationTestProducer.setInitialAdmissionTime(Instant.parse("2013-02-11T11:00:52Z"));
        orderPermutationTestProducer.setLocations(new String[]{
                "ED^UCHED RAT CHAIR^RAT-CHAIR",
                "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
                "ED^NON COVID MAJORS 04^04-NON COVID MAJORS"
        });


        Iterable<List<String>> permutationIterator = new ShuffleIterator<>(List.of(adtFilenames));

        return StreamSupport.stream(permutationIterator.spliterator(), false)
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> orderPermutationTestProducer.buildTestFromPermutation(duplicateCancel)));
    }


    private List<String> duplicateAt(String[] strings, int duplicateIndex) {
        List<String> output = new ArrayList<>(List.of(strings));
        output.add(strings[duplicateIndex]);
        return output;
    }

    /**
     * update patient info (infer admission possible), register, admit, transfer, discharge
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    Stream<DynamicTest> testDuplicateRegisterAdmitTransferDischarge() {
        adtFilenames = new String[]{"01_A04", "02_A01", "03_A02", "04_A03"};
        orderPermutationTestProducer.setMessagePath("Location/DuplicateSimple");
        orderPermutationTestProducer.setInitialAdmissionTime(Instant.parse("2013-02-11T11:00:52Z"));
        orderPermutationTestProducer.setLocations(new String[]{
                "ED^UCHED RAT CHAIR^RAT-CHAIR",
                "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
                "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
        });


        List<Iterable<List<String>>> duplicatedNames = new ArrayList<>();
        for (int i = 0; i < adtFilenames.length; i++) {
            List<String> filesWithOneDuplicate = duplicateAt(adtFilenames, i);
            duplicatedNames.add(new ShuffleIterator<>(filesWithOneDuplicate));
        }


        return duplicatedNames.stream()
                .flatMap(pi -> StreamSupport.stream(pi.spliterator(), false))
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> orderPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }


}
