package uk.ac.ucl.rits.inform.interchange.messaging;

import java.util.Objects;

/**
 * A simple paired value class.
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

    /**
     * @param first  item
     * @param second item
     */
    public Pair(T first, U second) {
        this.second = second;
        this.first = first;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return first.equals(pair.first)
                && second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
