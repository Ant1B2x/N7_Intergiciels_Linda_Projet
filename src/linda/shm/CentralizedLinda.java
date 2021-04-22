package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared memory implementation of Linda.
 */
public class CentralizedLinda implements Linda {

    private static final Object MUTEX = new Object();

    private Collection<Tuple> tuples;

    public CentralizedLinda() {
        this.tuples = new HashSet<>();
    }

    @Override
    public void write(Tuple t) {
        synchronized (MUTEX) {
            this.tuples.add(t);
        }
    }

    @Override
    public Tuple take(Tuple template) {
        Tuple result = this.read(template);


        this.tuples.remove(result);
        return result;
    }

    @Override
    public Tuple read(Tuple template) {
        while (true) {
            synchronized (MUTEX) {
                for (Tuple t : this.tuples) {
                    if (t.matches(template)) {
                        return t;
                    }
                }
            }
        }
    }

    @Override
    public Tuple tryTake(Tuple template) {
        Tuple result = this.tryRead(template);

        this.tuples.remove(result);
        return result;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        for (Tuple t : this.tuples)
            if (t.matches(template))
                return t;

        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> result = this.readAll(template);

        result.forEach(t -> this.tuples.remove(t));
        return result;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> result = new HashSet<>();
        for (Tuple t : this.tuples)
            if (t.matches(template))
                result.add(t);

        return result;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {

    }

    @Override
    public void debug(String prefix) {

    }

    // TO BE COMPLETED

}
