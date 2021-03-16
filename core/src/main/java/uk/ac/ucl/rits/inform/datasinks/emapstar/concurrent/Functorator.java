package uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public interface Functorator<T> extends Iterator<List<T>>, Iterable<List<T>> {

    default <R> Functorator<R> map(Function<? super T, ? extends R> mapper) {
        return new IteratorFunctor<>(this, mapper);
    }

    @Override
    default Iterator<List<T>> iterator() {
        return this;
    }
}
