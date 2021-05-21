package linda.server;

import linda.Linda;
import linda.Tuple;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface LindaServer extends Remote {

    /** Adds a tuple t to the tuplespace. */
    void write(Tuple t) throws RemoteException;

    /** Returns a tuple matching the template and removes it from the tuplespace.
     * Blocks if no corresponding tuple is found. */
    Tuple take(Tuple template) throws RemoteException;

    /** Returns a tuple matching the template and leaves it in the tuplespace.
     * Blocks if no corresponding tuple is found. */
    Tuple read(Tuple template) throws RemoteException;

    /** Returns a tuple matching the template and removes it from the tuplespace.
     * Returns null if none found. */
    Tuple tryTake(Tuple template) throws RemoteException;

    /** Returns a tuple matching the template and leaves it in the tuplespace.
     * Returns null if none found. */
    Tuple tryRead(Tuple template) throws RemoteException;

    /** Returns all the tuples matching the template and removes them from the tuplespace.
     * Returns an empty collection if none found (never blocks).
     * Note: there is no atomicity or consistency constraints between takeAll and other methods;
     * for instance two concurrent takeAll with similar templates may split the tuples between the two results.
     */
    Collection<Tuple> takeAll(Tuple template) throws RemoteException;

    /** Returns all the tuples matching the template and leaves them in the tuplespace.
     * Returns an empty collection if none found (never blocks).
     * Note: there is no atomicity or consistency constraints between readAll and other methods;
     * for instance (write([1]);write([2])) || readAll([?Integer]) may return only [2].
     */
    Collection<Tuple> readAll(Tuple template) throws RemoteException;

    /**
     * Renvoie un tuple correspond à un template, en take ou en read, immédiatement ou dans le futur
     * Utilise un sémaphore pour attendre qu'un tuple ait été lu ou pris
     * @param mode read ou take
     * @param timing mode immediate ou future
     * @param template template de tuple à rechercher
     * @return un tuple lu / pris correspondant au template
     * @throws RemoteException
     */
    Tuple waitEvent(Linda.eventMode mode, Linda.eventTiming timing, Tuple template) throws RemoteException;

    /** To debug, prints any information it wants (e.g. the tuples in tuplespace or the registered callbacks), prefixed by <code>prefix</code. */
    void debug(String prefix) throws RemoteException;

    /**
     * Sauvegarder l'espace de tuples dans un fichier (sérialisation)
     * @param filePath chemin d'accès du fichier
     */
    void save(String filePath) throws RemoteException;

    /**
     * Lire l'espace de tuples à partir d'un fichier (désérialisation)
     * @param filePath chemin d'accès du fichier
     */
    void load(String filePath) throws RemoteException;

}
