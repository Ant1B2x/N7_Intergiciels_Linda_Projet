package linda.server;

import linda.AsynchronousCallback;
import linda.Linda;
import linda.Tuple;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface LindaServer extends Remote {

    /**
     * Déclare le serveur courant comme serveur Linda sur l'URI composée d'un hôte et d'un port
     * Si un serveur est déjà déclaré à cette URI, s'enregistre en tant que serveur de backup
     * @param host l'hôte de l'URI
     * @param port le port de l'URI
     * @throws RemoteException en cas d'erreur réseau
     */
    void declareServer(String host, int port) throws RemoteException;

    /** Adds a tuple t to the tuplespace. */
    void write(Tuple t) throws RemoteException;

    /**
     * Ajoute un tuple t à l'espace de tuples sans appeler les callbacks en attente
     * Utilisé par le backup pour "suivre l'évolution" du serveur principal
     */
    void writeBackup(Tuple t) throws RemoteException;

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

    /** Registers a callback which will be called when a tuple matching the template appears.
     * If the mode is Take, the found tuple is removed from the tuplespace.
     * The callback is fired once. It may re-register itself if necessary.
     * If timing is immediate, the callback may immediately fire if a matching tuple is already present; if timing is future, current tuples are ignored.
     * Beware: a callback should never block as the calling context may be the one of the writer (see also {@link AsynchronousCallback} class).
     * Callbacks are not ordered: if more than one may be fired, the chosen one is arbitrary.
     * Beware of loop with a READ/IMMEDIATE re-registering callback !
     * Replace waitEvent() from previous version
     *
     * @param mode read or take mode.
     * @param timing (potentially) immediate or only future firing.
     * @param template the filtering template.
     * @param remoteCallback the RMI callback to call if a matching tuple appears.
     * @throws RemoteException if network error occurs
     */
    void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, RemoteCallback remoteCallback) throws RemoteException;

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

    /**
     * Enregistre un serveur Linda comme serveur de backup pour le serveur courant
     * Et lui envoie tous les tuples déjà présents sur le serveur courant afin qu'il "rattrape" son état
     * @param backupServer le serveur de backup à enregistrer comme backup
     * @throws RemoteException en cas d'erreur réseau
     */
    void registerBackup(LindaServer backupServer) throws RemoteException;

    /**
     * Appelle la méthode keepAlive() du serveur principal pour voir si il répond puis attend un certain temps
     * Si une erreur réseau se produit lors de l'appel à keepAlive(), reprend la main en tant que serveur principal
     * Uniquement utilisé par le serveur de backup
     * @throws RemoteException en cas d'erreur réseau
     */
    void pollPrimary() throws RemoteException;

    /**
     * Appelé sur le serveur principal par le serveur de backup dans pollPrimary()
     * Ne fait rien, permet juste de vérifier si une erreur réseau se produit lors de l'appel à cette méthode
     * @throws RemoteException en cas d'erreur réseau, permet au serveur de backup de reprendre la main
     */
    void keepAlive() throws RemoteException;

    /** To debug, prints any information it wants (e.g. the tuples in tuplespace or the registered callbacks), prefixed by <code>prefix</code. */
    void debug(String prefix) throws RemoteException;

}
