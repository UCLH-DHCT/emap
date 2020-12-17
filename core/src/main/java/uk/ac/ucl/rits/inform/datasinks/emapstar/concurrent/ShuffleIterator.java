package uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * Lazy shuffling iterator. Creates all possible orderings of the given list.
 *
 * @author Roma Klapaukh
 *
 * @param <T> Type of the items to shuffle
 */
public class ShuffleIterator<T> implements Iterator<List<T>>, Iterable<List<T>> {

    /**
     * Processing stack.
     */
    private Stack<ShuffleStackFrame> stack;

    /**
     * Create a new iterator that lazily produces permutations of the list.
     *
     * Note that the original list is only shallow cloned. This means that changes
     * to data items will be reflected in the iterator.
     *
     * @param data
     */
    public ShuffleIterator(List<T> data) {
        this.stack = new Stack<>();
        stack.push(new ShuffleStackFrame(new ArrayList<>(data), new ArrayList<>()));
        // Nothing to do if there isn't anything to shuffle
        if (data.isEmpty()) {
            return;
        }
        this.process();
    }

    @Override
    public Iterator<List<T>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public List<T> next() {
        if (stack.isEmpty()) {
            throw new NoSuchElementException();
        }
        // The next answer is always ready and waiting.
        ShuffleStackFrame frame = stack.pop();
        if (!frame.isLeaf()) {
            throw new IllegalStateException("First stack frame must be a leaf. Bug in ShuffleIterator.");
        }
        // Prepare the next ordering
        this.process();
        return frame.getOrdering();
    }

    /**
     * Step through the shuffling algorithm until a solution is found.
     */
    private void process() {
        // If the stack runs out, all options have been exhausted
        while (!stack.isEmpty()) {
            ShuffleStackFrame frame = stack.pop();
            // this cannot be a leaf
            if (frame.isLeaf()) {
                throw new IllegalStateException("Two adjacent leaves in stack. Bug in ShuffleIterator.");
            }
            // Run the frame
            ShuffleStackFrame child = frame.process();
            if (child != null) {
                stack.push(frame);
                stack.push(child);
                // If you found a solution stop.
                if (child.isLeaf()) {
                    return;
                }
            } // If no child, then discard the frame and continue (i.e. return in the
              // recusion).
        }
    }

    /**
     * Hold the transient state of the shuffling algorithm.
     *
     * @author Roma Klapaukh
     *
     */
    private class ShuffleStackFrame {
        private final List<T> soFar;
        private final List<T> data;
        private int           index;

        ShuffleStackFrame(List<T> data, List<T> soFar) {
            this.data = data;
            this.soFar = soFar;
            this.index = 0;
        }

        /**
         * Check if this node is a final solution or not.
         *
         * @return True if it a solution.
         */
        boolean isLeaf() {
            return this.data.isEmpty();
        }

        /**
         * If this is a leaf node, return the permutation described.
         *
         * @return The permutation of the original list described by this stack frame.
         */
        List<T> getOrdering() {
            return this.soFar;
        }

        /**
         * Return the next stack frame.
         *
         * @return Child stack frame. Returns null if it should return.
         */
        ShuffleStackFrame process() {
            if (index >= data.size()) {
                return null;
            }
            List<T> newData = new ArrayList<T>(data);
            List<T> newSoFar = new ArrayList<T>(soFar);
            T item = newData.remove(index++);
            newSoFar.add(item);
            return new ShuffleStackFrame(newData, newSoFar);
        }
    }

}
