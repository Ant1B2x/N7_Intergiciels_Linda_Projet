package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;

/** Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it is connected to.
 * */
public class LindaClient implements Linda {

    /**
     * Le temps fixe d'attente avant de tenter de se reconnecter à un serveur
     * lorsque le serveur auquel on est connecté ne répond plus
     */
    private static final int BACKUP_WAIT = 1000;

    /**
     * URI du serveur auquel on est connecté
     * Exemple : rmi://localhost:4000/LindaServer
     */
    private String serverURI;

    /**
     * Le linda serveur auquel on est connecté
     */
    private LindaServer lindaServer;

    /** Initializes the Linda implementation.
     *  @param serverURI the URI of the server, e.g. "rmi://localhost:4000/LindaServer" or "//localhost:4000/LindaServer".
     */
    public LindaClient(String serverURI) {
        System.out.println("Client called with URI: " + serverURI);
        this.serverURI = serverURI; // "sauvegarder" l'URI du serveur
        this.joinServer();
    }

    /**
     * Se connecter à un LindaServer à l'aide d'une URI
     */
    private void joinServer() {
        //  Connexion au serveur de noms (obtention d'un handle)
        try {
            this.lindaServer = (LindaServer) Naming.lookup(this.serverURI);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println(e);
        }
    }

    /**
     * Se reconnecter à un LindaServer à l'aide d'une URI
     * (attend 1s et appelle joinServer())
     */
    private void rejoinServer() {
        try {
            System.out.println("Main server dead, switching...");
            Thread.sleep(BACKUP_WAIT);
            this.joinServer();
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    @Override
    public void write(Tuple t) {
        try {
            this.lindaServer.write(t);
        } catch (RemoteException re) {
            this.rejoinServer();
            this.write(t);
        }
    }

    @Override
    public Tuple take(Tuple template) {
        try {
            return this.lindaServer.take(template);
        } catch (RemoteException re) {
            this.rejoinServer();
            return this.take(template);
        }
    }

    @Override
    public Tuple read(Tuple template) {
        try {
            return this.lindaServer.read(template);
        } catch (RemoteException re) {
            this.rejoinServer();
            return this.read(template);
        }
    }

    @Override
    public Tuple tryTake(Tuple template) {
        try {
            return this.lindaServer.tryTake(template);
        } catch (RemoteException re) {
            this.rejoinServer();
            return this.tryTake(template);
        }
    }

    @Override
    public Tuple tryRead(Tuple template) {
        try {
            return this.lindaServer.tryRead(template);
        } catch (RemoteException re) {
            this.rejoinServer();
            return this.tryRead(template);
        }
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        try {
            return this.lindaServer.takeAll(template);
        } catch (RemoteException re) {
            this.rejoinServer();
            return this.takeAll(template);
        }
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        try {
            return this.lindaServer.readAll(template);
        } catch (RemoteException re) {
            this.rejoinServer();
            return this.readAll(template);
        }
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        // Création d'un thread pour ne pas bloquer le client
        new Thread(() -> {
            try {
                // Crée un remoteCallback encapsulant callback et l'envoie au serveur dans eventRegister()
                this.lindaServer.eventRegister(mode, timing, template, new RemoteCallbackImpl(callback));
            } catch (RemoteException re) {
                this.rejoinServer();
                this.eventRegister(mode, timing, template, callback);
            }
        }).start();
    }

    @Override
    public void debug(String prefix) {
        try {
            this.lindaServer.debug(prefix);
        } catch (RemoteException re) {
            this.rejoinServer();
            this.debug(prefix);
        }
    }

}
