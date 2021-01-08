package uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IteratorFunctor<S, T> implements Functorator<T> {

    private final Iterator<List<S>> iterator;
    private final Function<? super S, ? extends T> mapper;

    public IteratorFunctor(Iterator<List<S>> iter, Function<? super S, ? extends T> mapper) {
        this.iterator = iter;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public List<T> next() {
        return this.iterator.next().stream().map(mapper).collect(Collectors.toList());
    }

}
