package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.*;

/**
 * Shared memory implementation of Linda.
 */
public class CentralizedLinda implements Linda {

    private class CallbackTriplet {
        private eventMode mode;
        private Tuple template;
        private Callback callback;

        public CallbackTriplet(eventMode mode, Tuple template, Callback callback) {
            this.mode = mode;
            this.template = template;
            this.callback = callback;
        }

        public eventMode getMode() {
            return this.mode;
        }

        public Tuple getTemplate() {
            return this.template;
        }

        public Callback getCallback() {
            return this.callback;
        }
    }

    private static final Object MUTEX = new Object();

    private Collection<Tuple> tuples;
    private Collection<CallbackTriplet> callbacks;

    public CentralizedLinda() {
        this.tuples = new HashSet<>(); // TODO remplacer par autre chose qu'un set
        this.callbacks = new HashSet<>();
    }

    @Override
    public void write(Tuple t) {
        synchronized (MUTEX) {

            // Chercher un callback qui avait demandé un tel tuple
            for (CallbackTriplet ct : this.callbacks) {
                if (t.matches(ct.getTemplate())) {
                    // Transmettre le tuple au callback
                    ct.getCallback().call(t);

                    // Si on est en mode take, on ne veut pas ajouter le tuple dans l'espace partagé
                    if (ct.getMode() == eventMode.TAKE)
                        return;
                }
            }

            this.tuples.add(t);
        }
    }

    @Override
    public Tuple take(Tuple template) {
        while (true) {
            synchronized (MUTEX) {
                for (Tuple t : this.tuples) {
                    if (t.matches(template)) {
                        this.tuples.remove(t);
                        return t;
                    }
                }
            }
        }
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
        synchronized (MUTEX) {
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    this.tuples.remove(t);
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        synchronized (MUTEX) {
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> result = new HashSet<>(); // TODO changer type
        synchronized (MUTEX) {
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    result.add(t);
                }
            }
            result.forEach(t -> this.tuples.remove(t));
        }
        return result;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> result = new HashSet<>();
        synchronized (MUTEX) {
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        Tuple t = null;

        if (timing == eventTiming.IMMEDIATE)
            if (mode == eventMode.READ)
                t = this.tryRead(template);
            else if (mode == eventMode.TAKE)
                t = this.tryTake(template);

        if (t != null)
            callback.call(t);
        else
            this.callbacks.add(new CallbackTriplet(mode, template, callback));
    }

    @Override
    public void debug(String prefix) {

    }

}
