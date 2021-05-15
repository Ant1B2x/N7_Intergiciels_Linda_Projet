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

    private static final int BACKUP_WAIT = 3000;

    private String serverURI;
    private LindaServer lindaServer;

    /** Initializes the Linda implementation.
     *  @param serverURI the URI of the server, e.g. "rmi://localhost:4000/LindaServer" or "//localhost:4000/LindaServer".
     */
    public LindaClient(String serverURI) {
        System.out.println("Client called with URI: " + serverURI);
        this.serverURI = serverURI;
        this.joinServer();
    }

    private void joinServer() {
        //  Connexion au serveur de noms (obtention d'un handle)
        try {
            this.lindaServer = (LindaServer) Naming.lookup(this.serverURI);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println(e);
        }
    }

    private void rejoinServer() {
        try {
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
        new Thread(() -> {
            try {
                RemoteCallback remoteCallback = new RemoteCallbackImpl(callback);
                this.lindaServer.eventRegister(mode, timing, template, remoteCallback);
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
            try {
                Thread.sleep(BACKUP_WAIT);
                this.debug(prefix);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
    }

}
