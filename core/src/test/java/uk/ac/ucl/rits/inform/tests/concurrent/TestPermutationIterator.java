package uk.ac.ucl.rits.inform.tests.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent.ShuffleIterator;

@Testable
public class TestPermutationIterator {

    @Test
    public void testSmallList() {
        List<Integer> items = List.of(1, 2, 3);

        List<String> sets = new ArrayList<>();
        for (List<Integer> set : new ShuffleIterator<>(items)) {
            assertEquals(items.size(), set.size());
            String text = set.stream().map(String::valueOf).reduce("", String::concat);
            sets.add(text);
        }
        int expectedSize = 1;
        for (int i = 2; i <= items.size(); i++) {
            expectedSize *= i;
        }
        assertEquals(expectedSize, sets.size(), "Wrong number of permutations generated");
        assertEquals(expectedSize, sets.stream().distinct().count(), "Some permutations were repeated");

    }

    @Test
    public void testEmptyList() {
        List<Integer> items = List.of();

        int size = 0;
        for (List<Integer> set : new ShuffleIterator<>(items)) {
            assertEquals(0, set.size());
            size++;
        }
        int expectedSize = 1;
        assertEquals(expectedSize, size, "Wrong number of permutations generated");

    }

    @Test
    public void testFunctor() {
        List<Integer> items = List.of(1, 2, 3);
        List<Integer> targets = List.of(2, 4, 6);

        int expectedSize = 1;
        for (int i = 2; i <= items.size(); i++) {
            expectedSize *= i;
        }

        for (List<Integer> set : new ShuffleIterator<>(items).map(v -> v * 2)) {
            assertEquals(items.size(), set.size());
            assertTrue(set.containsAll(targets));
            expectedSize--;
        }

        assertEquals(0, expectedSize, "Wrong number of permutations were generated");

    }

}
