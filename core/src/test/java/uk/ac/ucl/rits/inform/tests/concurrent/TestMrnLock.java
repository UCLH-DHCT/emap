package uk.ac.ucl.rits.inform.tests.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent.MrnLock;

/**
 * Tests for the Mrn Lock.
 *
 * @author Roma Klapaukh
 *
 */
@Testable
public class TestMrnLock {

    // Helpful shared memory variable to use.
    private volatile int count;

    /**
     * Test that sequenced events happen in the expected order.
     *
     * @throws InterruptedException shouldn't happen
     */
    @Test
    public void testSequence() throws InterruptedException {
        MrnLock lock = new MrnLock();

        String mrn1 = "1";
        lock.acquire(mrn1);
        count = 1;

        Runnable r = () -> {
            try {
                lock.acquire(mrn1);
                assertEquals(1, this.count);
                count = 3;
                assertEquals(3, this.count);
                lock.release(mrn1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread t = new Thread(r);
        t.start();

        Thread.sleep(200);
        assertEquals(1, this.count);

        lock.release(mrn1);

        t.join();
        assertEquals(3, this.count);
    }

    /**
     * Enabled to test that testSequence does actually fail without the locks.
     *
     * @throws InterruptedException Shouldn't happen
     */
    @Test
    @Disabled("Should fail")
    public void testSequenceFail() throws InterruptedException {
        count = 1;

        Runnable r = () -> {
            assertEquals(1, this.count);
            count = 3;
            assertEquals(3, this.count);

        };
        Thread t = new Thread(r);
        t.start();

        Thread.sleep(200);
        assertEquals(1, this.count);

        t.join();
        assertEquals(3, this.count);
    }

    /**
     * Test that order doesn't matter in passing mrns to locks.
     *
     * @throws InterruptedException Shouldn't happen
     */
    @Test
    public void testPair() throws InterruptedException {
        MrnLock lock = new MrnLock();

        String mrn1 = "1";
        String mrn2 = "2";
        lock.acquire(mrn2, mrn1);
        count = 1;

        Runnable r = () -> {
            try {
                lock.acquire(mrn1, mrn2);
                assertEquals(1, this.count);
                count = 3;
                assertEquals(3, this.count);
                lock.release(mrn1);
                lock.release(mrn2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread t = new Thread(r);
        t.start();

        Thread.sleep(200);
        assertEquals(1, this.count);

        lock.release(mrn1, mrn2);

        t.join();
        assertEquals(3, this.count);
    }

    /**
     * Enable to test that testPair does fail.
     *
     * @throws InterruptedException Shouldn't happen
     */
    @Test
    @Disabled("Should fail")
    public void testPair2() throws InterruptedException {
        count = 1;

        Runnable r = () -> {
            assertEquals(1, this.count);
            count = 3;
            assertEquals(3, this.count);
        };
        Thread t = new Thread(r);
        t.start();

        Thread.sleep(200);
        assertEquals(1, this.count);

        t.join();
        assertEquals(3, this.count);
    }

    /**
     * Test list locking.
     *
     * @throws InterruptedException Shouldn't happen
     */
    @Test
    public void testlist() throws InterruptedException {
        MrnLock lock = new MrnLock();

        List<String> mrns = new ArrayList<>();
        mrns.add("1");
        mrns.add("2");

        lock.acquire(mrns);
        count = 1;

        Runnable r = () -> {
            try {
                lock.acquire(mrns);
                assertEquals(1, this.count);
                count = 3;
                assertEquals(3, this.count);
                lock.release(mrns);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread t = new Thread(r);
        t.start();

        Thread.sleep(200);
        assertEquals(1, this.count);

        lock.release(mrns);

        t.join();
        assertEquals(3, this.count);
    }

    /**
     * Test that you can't release a lock that doesn't exist.
     *
     * @throws InterruptedException Shouldn't happen.
     */
    @Test
    public void testFalseRelease() throws InterruptedException {
        MrnLock lock = new MrnLock();

        String mrn1 = "1";

        assertThrows(NoSuchElementException.class, () -> lock.release(mrn1));

        lock.acquire(mrn1);
        lock.release(mrn1);
        assertThrows(NoSuchElementException.class, () -> lock.release(mrn1));

        lock.acquire(mrn1);
        assertThrows(NoSuchElementException.class, () -> {
            lock.release("2");
        });
        lock.release(mrn1);
    }


    /**
     * Test that trying to lock a duplicate throws an exception.
     */
    @Test
    public void testDuplicates() {
        MrnLock lock = new MrnLock();

        String mrn1 = "1";

        assertThrows(IllegalArgumentException.class, () -> lock.acquire(mrn1, mrn1));

        assertThrows(NullPointerException.class, () -> lock.acquire(mrn1, null));

        assertThrows(NullPointerException.class, () -> lock.acquire((String) null));
        assertThrows(NullPointerException.class, () -> lock.acquire((List<String>) null));

        List<String> mrns = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> lock.acquire(mrns));

        mrns.add(mrn1);
        mrns.add(mrn1);

        assertThrows(IllegalArgumentException.class, () -> lock.acquire(mrns));
    }
}
