package uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * An MrnLock allows us to lock either individual or groups of MRNs
 * individually. The implementation will seek to run in memory proportional to
 * the number of currently held locks, not the total number seen.
 * <p>
 * Holders must acquire all desired locks with a single call to acquire. Once
 * aquire is called it may not be called again until all aquired locks are
 * released. Unlike aquiring, locks can be released individually in any order.
 * <p>
 * Note that it may be possible for many locks for the same mrn to exist at the
 * same time and all to have granted access at the same time. The restriction is
 * that all except at most one, will have already called release() and therefore
 * will no longer be using that access.
 * @author Roma Klapaukh
 */
public class MrnLock {

    private final Map<String, MutableInteger> licences = new ConcurrentHashMap<>();

    /**
     * Create a new MrnLock. All permissions are available.
     */
    public MrnLock() {}

    /**
     * Block until you get an exclusive lock on a single mrn.
     * <p>
     * Locks are <b>not</b> reentrant. Moreover, if you hold already hold a lock,
     * you are not allowed to acquire more until you release all those you already
     * hold.
     * @param mrn The mrn to get a lock on
     * @throws InterruptedException If the thread is interrupted
     */
    public void acquire(String mrn) throws InterruptedException {
        if (mrn == null) {
            throw new NullPointerException("mrn cannot be null");
        }

        MutableInteger lock;
        synchronized (licences) {
            lock = licences.get(mrn);
            if (lock == null) {
                lock = new MutableInteger();
                licences.put(mrn, lock);
            } else {
                // A unheld lock should have been removed from the map
                if (lock.isUnheld()) {
                    throw new IllegalStateException("Lock with no holders was stored in licenses map");
                }
            }
            lock.increment();
        }
        lock.acquire();
    }

    /**
     * Block until you get an exclusive lock on a pair of mrns.
     * <p>
     * Locks are <b>not</b> reentrant. Moreover, if you hold already hold a lock,
     * you are not allowed to acquire more until you release all those you already
     * hold.
     * @param mrn1 One of the mrns to get a lock on
     * @param mrn2 The other mrn to get a lock on
     * @throws InterruptedException If the thread is interrupted
     */
    public void acquire(String mrn1, String mrn2) throws InterruptedException {
        if (mrn1 == null || mrn2 == null) {
            throw new NullPointerException(String.format("Neither of mrn1 (%s) nor mrn2 (%s) can be null", mrn1, mrn2));
        }

        int comparisson = mrn1.compareTo(mrn2);

        if (comparisson < 0) {
            this.acquire(mrn1);
            this.acquire(mrn2);
        } else if (comparisson > 0) {
            this.acquire(mrn2);
            this.acquire(mrn1);
        } else {
            throw new IllegalArgumentException("mrn1 must be different from mrn2");
        }
    }

    /**
     * Block until you get an exclusive lock on a list of mrns.
     * <p>
     * Locks are <b>not</b> reentrant. Moreover, if you hold already hold a lock,
     * you are not allowed to acquire more until you release all those you already
     * hold.
     * @param mrns The mrns to get a lock on
     * @throws InterruptedException If the thread is interrupted
     */
    public void acquire(List<String> mrns) throws InterruptedException {
        if (mrns.isEmpty()) {
            throw new IllegalArgumentException("mrns cannot be empty");
        }

        mrns.sort((a, b) -> {
            int value = a.compareTo(b);
            if (value == 0) {
                throw new IllegalArgumentException("All mrns must be unique");
            }
            return value;
        });

        for (String mrn : mrns) {
            this.acquire(mrn);
        }
    }

    /**
     * Release a previously acquired lock. You must not release a lock that you do
     * not already hold. This is not checked for.
     * @param mrn The mrn to release the lock for.
     */
    public void release(String mrn) {
        if (mrn == null) {
            throw new NullPointerException("mrn cannot be null");
        }

        MutableInteger lock;
        synchronized (licences) {
            lock = licences.get(mrn);
            if (lock == null) {
                throw new NoSuchElementException(String.format("No lock held for %s", mrn));
            }
            lock.decrement();
            if (lock.isUnheld()) {
                this.licences.remove(mrn);
            }
        }
        lock.release();
    }

    /**
     * Release two previously acquired locks. You must not release a lock that you
     * do not already hold. This is not checked for.
     * @param mrn1 One mrn to release the lock for.
     * @param mrn2 Another mrn to release the lock for.
     */
    public void release(String mrn1, String mrn2) {
        if (mrn1 == null || mrn2 == null || mrn1.equals(mrn2)) {
            throw new IllegalArgumentException(
                    String.format("mrn1 (%s) and mrn2 (%s) must be non-null and different", mrn1, mrn2));
        }

        this.release(mrn1);
        this.release(mrn2);
    }

    /**
     * Release a list of previously acquired locks. You must not release a lock that
     * you do not already hold. This is not checked for.
     * @param mrns Iterable of locks to release. Must be non-null, non-empty, and
     *             contain no duplicates.
     */
    public void release(Iterable<String> mrns) {
        if (mrns == null) {
            throw new IllegalArgumentException("mrns must not be null");
        }

        for (String mrn : mrns) {
            this.release(mrn);
        }
    }

    /**
     * Keep track of individual semaphores and how much interest there is in them.
     * @author Roma Klapaukh
     */
    private static class MutableInteger {

        private int holders = 0;
        private final Semaphore semaphore = new Semaphore(1);

        /**
         * Acquire a single access lock. Block until you can.
         * @throws InterruptedException
         */
        void acquire() throws InterruptedException {
            this.semaphore.acquire();
        }

        /**
         * Release the lock. This must only be called if you have previously acquired
         * the lock.
         */
        void release() {
            this.semaphore.release();
        }

        /**
         * Register interest in the lock.
         */
        void increment() {
            this.holders += 1;
        }

        /**
         * Register that interest in the lock is now complete.
         */
        void decrement() {
            if (this.holders == 0) {
                throw new IllegalStateException("Cannot decrement if there are no holders");
            }
            this.holders -= 1;
        }

        /**
         * Return true if there are no registered interests in this lock (so it can
         * potentially be deleted).
         * @return True if there are no registered interested holders.
         */
        boolean isUnheld() {
            return this.holders == 0;
        }
    }
}
