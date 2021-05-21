package linda.shm;

import linda.Callback;
import linda.Tuple;

import java.util.concurrent.Semaphore;

/**
 * Callback implémenté avec un sémaphore
 * Utilisé en interne par la classe CentralizedLinda
 * Et par le client lorsqu'il appelle le serveur dans la version sans serveur de backup
 */
public class LockedCallback implements Callback {

    /**
     * Tuple sur lequel est appelé
     */
    private Tuple tuple;

    /**
     * Sémaphore initialisé à 0
     * Est release quand le callback est appelé
     */
    private Semaphore semaphore;

    /**
     * Initialise un LockedCallback avec Sémaphore à 0
     */
    public LockedCallback() {
        this.semaphore = new Semaphore(0);
    }

    /**
     * "Enregistre" le tuple et libère le callback
     * @param t tuple sur lequel le callback est appelé
     */
    @Override
    public void call(Tuple t) {
        this.tuple = t;
        this.semaphore.release();
    }

    /**
     * Retourne le tuple associé au callback
     * @return le tuple sur lequel le callback a été appelé
     */
    public Tuple getTuple() {
        return this.tuple;
    }

    /**
     * Attend que le callback ait été appelé
     * (Que le sémaphore ait été libéré)
     */
    public void await() {
        try {
            this.semaphore.acquire();
        } catch (InterruptedException e) {
        }
    }

}
