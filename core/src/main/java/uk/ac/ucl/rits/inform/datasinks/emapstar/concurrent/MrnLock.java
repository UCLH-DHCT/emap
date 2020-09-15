package uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * An MrnLock allows us to lock either individual or groups of MRNs
 * individually. The implementation will seek to run in memory proportional to
 * the number of currently held locks, not the total number seen.
 *
 * Note that it may be possible for many locks for the same mrn to exist at the
 * same time and all to have granted access at the same time. The restriction is
 * that all except at most one, will have already called release() and therefore
 * will no longer be using that access.
 *
 * @author Roma Klapaukh
 *
 */
public class MrnLock {

    private Map<String, MutableInteger> licences = new ConcurrentHashMap<>();

    /**
     * Create a new MrnLock. All permissions are available.
     */
    public MrnLock() {}

    /**
     * Block until you get an exclusive lock on a single mrn.
     *
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
            }
            lock.increment();
        }
        lock.acquire();
    }

    /**
     * Release a previously acquired lock. You must not release a lock
     * that you do not already hold. This is not checked for.
     *
     * @param mrn The mrn to release the lock for.
     */
    public void release(String mrn) {
        if (mrn == null) {
            throw new NullPointerException("mrn cannot be null");
        }

        MutableInteger lock;
        synchronized (licences) {
            lock = licences.get(mrn);
            lock.decrement();
            if (lock.isUnheld()) {
                this.licences.remove(mrn);
            }
        }
        lock.release();
    }

    /**
     * Keep track of individual semaphores and how much interest there is in them.
     *
     * @author Roma Klapaukh
     *
     */
    private static class MutableInteger {

        private int       holders   = 0;
        private Semaphore semaphore = new Semaphore(1);

        /**
         * Acquire a single access lock. Block until you can.
         *
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
         *
         * @return True if there are no registered interested holders.
         */
        boolean isUnheld() {
            return this.holders == 0;
        }
    }
}
