package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Collection;

/** Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it is connected to.
 * */
public class LindaClient implements Linda {

    /**
     * Le linda serveur auquel on est connecté
     */
    private LindaServer lindaServer;

    /** Initializes the Linda implementation.
     *  @param serverURI the URI of the server, e.g. "rmi://localhost:4000/LindaServer" or "//localhost:4000/LindaServer".
     */
    public LindaClient(String serverURI) {
        //  Connexion au serveur de noms (obtention d'un handle)
        try {
            System.out.println("Client called with URI: " + serverURI);
            this.lindaServer = (LindaServer) Naming.lookup(serverURI);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    @Override
    public void write(Tuple t) {
        try {
            this.lindaServer.write(t);
        } catch (RemoteException e) {
            System.err.println(e);
        }
    }

    @Override
    public Tuple take(Tuple template) {
        try {
            return this.lindaServer.take(template);
        } catch (RemoteException e) {
            System.err.println(e);
        }
        return null;
    }

    @Override
    public Tuple read(Tuple template) {
        try {
            return this.lindaServer.read(template);
        } catch (RemoteException e) {
            System.err.println(e);
        }
        return null;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        try {
            return this.lindaServer.tryTake(template);
        } catch (RemoteException e) {
            System.err.println(e);
        }
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        try {
            return this.lindaServer.tryRead(template);
        } catch (RemoteException e) {
            System.err.println(e);
        }
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        try {
            return this.lindaServer.takeAll(template);
        } catch (RemoteException e) {
            System.err.println(e);
        }
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        try {
            return this.lindaServer.readAll(template);
        } catch (RemoteException e) {
            System.err.println(e);
        }
        return null;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        // Création d'un thread pour ne pas bloquer le client
        new Thread(() -> {
            try {
                // Ici, waitEvent fait attendre jusqu'à ce qu'un tuple soit trouvé à l'aide d'un sémaphore
                // Ça évite d'avoir un callback en remote, du moins pour l'instant :)
                Tuple tuple = LindaClient.this.lindaServer.waitEvent(mode, timing, template);
                callback.call(tuple);
            } catch (RemoteException e) {
                System.err.println(e);
            }
        }).start();
    }

    @Override
    public void debug(String prefix) {
        try {
            this.lindaServer.debug(prefix);
        } catch (RemoteException e) {
            System.err.println(e);
        }
    }

}
