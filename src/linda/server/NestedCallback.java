package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;

/**
 * Implémentation de callback contenant un callback RMI
 * Créé côté serveur et envoyé au Linda centralisé (qui ne sait pas traiter les callbacks RMI)
 * Il s'agit donc d'une implémentation de l'interface Callback
 */
public class NestedCallback implements Callback {

    /**
     * Le callback RMI à encapsuler, qui sera appelé lors de l'appel du callback
     */
    private final RemoteCallback remoteCallback;

    /**
     * Crée un callback à partir d'un callback RMI
     * @param remoteCallback le callback RMI à utiliser
     */
    public NestedCallback(RemoteCallback remoteCallback) {
        this.remoteCallback = remoteCallback;
    }

    /**
     * Appelle le callback avec un tuple
     * Le callback RMI est ensuite appelé avec ce même tuple
     * @param t le tuple avec lequel le callback est appelé
     */
    @Override
    public void call(Tuple t) {
        try {
            this.remoteCallback.call(t);
        } catch (RemoteException e) {
            System.err.println(e);
        }
    }

}
