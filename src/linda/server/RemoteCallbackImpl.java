package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implémentation de callback RMI contenant un callback
 * Créé côté client et envoyé au serveur dans eventRegister()
 */
public class RemoteCallbackImpl extends UnicastRemoteObject implements RemoteCallback {

    /**
     * Le callback à encapsuler, qui sera appelé lors de l'appel du callback RMI
     * Ne peut pas implémenter l'interface Callback car pas la même signature de call (levée d'exception)
     */
    private final Callback callback;

    /**
     * Crée un callback RMI à partir d'un callback
     * @param callback le callback à partir duquel créer le callback RMI
     * @throws RemoteException en cas d'erreur réseau
     */
    public RemoteCallbackImpl(Callback callback) throws RemoteException {
        this.callback = callback;
    }

    @Override
    public void call(Tuple t) throws RemoteException {
        this.callback.call(t);
    }

}
