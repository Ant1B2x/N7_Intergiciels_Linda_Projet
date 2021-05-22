package linda.server;

import linda.Tuple;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Callback RMI contenant un callback
 * Créé côté client et envoyé au serveur dans eventRegister()
 * Ne peut pas étendre l'interface Callback car pas la même signature de call (levée d'exception)
 */
public interface RemoteCallback extends Remote {

    /**
     * Appelle un callback RMI avec un tuple
     * Le callback est ensuite appelé avec ce même tuple
     * @param t le tuple avec lequel le callback RMI est appelé
     * @throws RemoteException en cas d'erreur réseau
     */
    void call(Tuple t) throws RemoteException;

}
