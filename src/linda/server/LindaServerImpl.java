package linda.server;

import linda.AsynchronousCallback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import linda.shm.LockedCallback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

public class LindaServerImpl extends UnicastRemoteObject implements LindaServer {

    /**
     * L'espace de tuple en mémoire partagé à utiliser
     */
    private Linda linda;

    /**
     * Crée un serveur Linda et l'initialise avec un Linda en mémoire partagée
     * @throws RemoteException si il y a un problème de réseau
     */
    public LindaServerImpl() throws RemoteException {
        this.linda = new CentralizedLinda();
    }

    @Override
    public void write(Tuple t) {
        this.linda.write(t);
    }

    @Override
    public Tuple take(Tuple template) {
        return this.linda.take(template);
    }

    @Override
    public Tuple read(Tuple template) {
        return this.linda.read(template);
    }

    @Override
    public Tuple tryTake(Tuple template) {
        return this.linda.tryTake(template);
    }

    @Override
    public Tuple tryRead(Tuple template) {
        return this.linda.tryRead(template);
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        return this.linda.takeAll(template);
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        return this.linda.readAll(template);
    }

    @Override
    public Tuple waitEvent(Linda.eventMode mode, Linda.eventTiming timing, Tuple template) {
        // Créer un LockedCallback (Callback implémenté avec un sémaphore)
        LockedCallback lc = new LockedCallback();
        // Enregistre le callback sur le Linda en mémoire partagée
        this.linda.eventRegister(mode, timing, template, new AsynchronousCallback(lc));
        // Attend qu'un tuple ait été lu ou pris
        lc.await();
        // Retourne le tuple lu ou pris
        return lc.getTuple();
    }

    @Override
    public void debug(String prefix) {
        this.linda.debug(prefix);
    }

    @Override
    public void save(String filePath) {
        ((CentralizedLinda) this.linda).save(filePath);
    }

    @Override
    public void load(String filePath) {
        ((CentralizedLinda) this.linda).load(filePath);
    }

}
