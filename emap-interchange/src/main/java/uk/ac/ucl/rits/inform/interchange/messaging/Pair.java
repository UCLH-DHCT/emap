package uk.ac.ucl.rits.inform.interchange.messaging;

/**
 * A simple paired value class.
 *
 * @param <T> first item
 * @param <U> second item
 */
public final class Pair<T, U> {
    /**
     * @param T first item
     */
    public final T first;

    /**
     * @param U second item
     */
    public final U second;


    public Pair(T first, U second) {
        this.second = second;
        this.first = first;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", first, second);
    }
}
