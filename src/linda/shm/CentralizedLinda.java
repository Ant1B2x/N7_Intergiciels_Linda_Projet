package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Shared memory implementation of Linda.
 */
public class CentralizedLinda implements Linda {

    /**
     * Callback used for read and take
     */
    private class InternalCallback implements Callback {
        private Tuple tuple;
        private Semaphore semaphore;

        public InternalCallback() {
            this.semaphore = new Semaphore(0);
        }

        @Override
        public void call(Tuple t) {
            this.tuple = t;
            this.semaphore.release();
        }
    }

    private class CallbackTriplet {
        private final eventMode mode;
        private final Tuple template;
        private final Callback callback;

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
        this.tuples = new LinkedList<>();
        this.callbacks = new LinkedList<>();
    }

    @Override
    public void write(Tuple t) {
        synchronized (MUTEX) {
            // Liste des callbacks appelés (à supprimer)
            Collection<CallbackTriplet> calledCallbacks = new LinkedList<>();

            // Ajouter le tuple à l'espace de tuple
            this.tuples.add(t);

            // Chercher un callback qui avait demandé un tel tuple
            for (CallbackTriplet ct : this.callbacks) {
                if (t.matches(ct.getTemplate())) {
                    // Ajouter le callback à la liste des callbacks appelés
                    calledCallbacks.add(ct);

                    // Transmettre le tuple au callback
                    ct.getCallback().call(t);

                    // Si on est en mode take, on ne veut pas garder le tuple dans l'espace partagé
                    if (ct.getMode() == eventMode.TAKE) {
                        this.tuples.remove(t);
                        break;
                    }
                }
            }

            // Supprimer les callbacks appelés de la liste des callbacks courants
            calledCallbacks.forEach(ct -> this.callbacks.remove(ct));
        }
    }

    @Override
    public Tuple take(Tuple template) {
        // Créer un callback et l'enregister
        InternalCallback cb = new InternalCallback();
        this.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, template, cb);

        // Attendre que le callback ait été appelé
        try {
            cb.semaphore.acquire();
        } catch (InterruptedException e) {
        }

        // Retourner le tuple pris
        return cb.tuple;
    }

    @Override
    public Tuple read(Tuple template) {
        // Créer un callback et l'enregister
        InternalCallback cb = new InternalCallback();
        this.eventRegister(eventMode.READ, eventTiming.IMMEDIATE, template, cb);

        // Attendre que le callback ait été appelé
        try {
            cb.semaphore.acquire();
        } catch (InterruptedException e) {
        }

        // Retourner le tuple lu
        return cb.tuple;
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
        Collection<Tuple> result = new LinkedList<>();
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
        Collection<Tuple> result = new LinkedList<>();
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

        if (timing == eventTiming.IMMEDIATE) {
            if (mode == eventMode.READ) {
                t = this.tryRead(template);
            } else if (mode == eventMode.TAKE) {
                t = this.tryTake(template);
            }
        }

        if (t != null) {
            callback.call(t);
        }
        else {
            synchronized (MUTEX) {
                this.callbacks.add(new CallbackTriplet(mode, template, callback));
            }
        }
    }

    @Override
    public void debug(String prefix) {

    }

}
