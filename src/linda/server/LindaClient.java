package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.concurrent.Semaphore;

/** Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it is connected to.
 * */
public class LindaClient implements Linda {

    private static final int BACKUP_WAIT = 3000;

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
        } catch (RemoteException re) {
            try {
                Thread.sleep(BACKUP_WAIT);
                this.write(t);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
    }

    @Override
    public Tuple take(Tuple template) {
        try {
            return this.lindaServer.take(template);
        } catch (RemoteException re) {
            try {
                Thread.sleep(BACKUP_WAIT);
                return this.take(template);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        return null;
    }

    @Override
    public Tuple read(Tuple template) {
        try {
            return this.lindaServer.read(template);
        } catch (RemoteException re) {
            try {
                Thread.sleep(BACKUP_WAIT);
                return this.read(template);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        return null;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        try {
            return this.lindaServer.tryTake(template);
        } catch (RemoteException re) {
            try {
                Thread.sleep(BACKUP_WAIT);
                return this.tryTake(template);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        try {
            return this.lindaServer.tryRead(template);
        } catch (RemoteException re) {
            try {
                Thread.sleep(BACKUP_WAIT);
                return this.tryRead(template);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        try {
            return this.lindaServer.takeAll(template);
        } catch (RemoteException re) {
            try {
                Thread.sleep(BACKUP_WAIT);
                return this.takeAll(template);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        try {
            return this.lindaServer.readAll(template);
        } catch (RemoteException re) {
            try {
                Thread.sleep(BACKUP_WAIT);
                return this.readAll(template);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        return null;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        new Thread(() -> {
            try {
                RemoteCallback remoteCallback = new RemoteCallbackImpl(callback);
                Tuple tuple = this.lindaServer.eventRegister(mode, timing, template, remoteCallback);
                callback.call(tuple);
            } catch (RemoteException re) {
                try {
                    Thread.sleep(BACKUP_WAIT);
                    this.eventRegister(mode, timing, template, callback);
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
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
