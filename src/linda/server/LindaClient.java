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

    private LindaServer lindaServer;

    /** Initializes the Linda implementation.
     *  @param serverURI the URI of the server, e.g. "rmi://localhost:4000/LindaServer" or "//localhost:4000/LindaServer".
     */
    public LindaClient(String serverURI) {
        //  Connexion au serveur de noms (obtention d'un handle)
        try {
            this.lindaServer = (LindaServer) Naming.lookup("rmi://"+serverURI+"/LindaServer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(Tuple t) {
        try {
            this.lindaServer.write(t);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Tuple take(Tuple template) {
        try {
            return this.lindaServer.take(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Tuple read(Tuple template) {
        try {
            return this.lindaServer.read(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        try {
            return this.lindaServer.tryTake(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        try {
            return this.lindaServer.tryRead(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        try {
            return this.lindaServer.takeAll(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        try {
            return this.lindaServer.readAll(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        try {
            final RemoteCallback remoteCallback = new RemoteCallbackImpl(callback);
            this.lindaServer.eventRegister(mode, timing, template, remoteCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void debug(String prefix) {

    }

}
