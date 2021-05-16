package linda.server;

import linda.AsynchronousCallback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

public class LindaServerImpl extends UnicastRemoteObject implements LindaServer {

    private Linda linda;
    private LindaServer otherServer;
    private boolean isBackup;

    public LindaServerImpl() throws RemoteException {
        this.linda = new CentralizedLinda();
    }

    @Override
    public void declareServer() throws RemoteException {
        try {
            Naming.bind("rmi://localhost:4000/LindaServer", this);
            this.isBackup = false;
        } catch (AlreadyBoundException abe) {
            try {
                Naming.bind("rmi://localhost:4000/LindaServerBackup", this);
                this.otherServer = (LindaServer) Naming.lookup("rmi://localhost:4000/LindaServer");
                this.isBackup = true;
                this.otherServer.registerBackup(this);
                new Thread(this::ping).start();
            } catch (AlreadyBoundException | MalformedURLException | NotBoundException e) {
                System.err.println(e);
            }
        } catch (MalformedURLException e) {
            System.err.println(e);
        }
    }

    @Override
    public void write(Tuple t) {
        this.linda.write(t);
        if (!this.isBackup && this.otherServer != null) {
            try {
                otherServer.writeBackup(t);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
    }

    @Override
    public void writeBackup(Tuple t) {
        ((CentralizedLinda) this.linda).writeWithoutCalling(t);
    }

    @Override
    public Tuple take(Tuple template) {
        if (!this.isBackup && this.otherServer != null) {
            try {
                otherServer.take(template);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
        return this.linda.take(template);
    }

    @Override
    public Tuple read(Tuple template) {
        if (!this.isBackup && this.otherServer != null) {
            try {
                otherServer.read(template);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
        return this.linda.read(template);
    }

    @Override
    public Tuple tryTake(Tuple template) {
        if (!this.isBackup && this.otherServer != null) {
            try {
                otherServer.tryTake(template);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
        return this.linda.tryTake(template);
    }

    @Override
    public Tuple tryRead(Tuple template) {
        if (!this.isBackup && this.otherServer != null) {
            try {
                otherServer.tryRead(template);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
        return this.linda.tryRead(template);
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        if (!this.isBackup && this.otherServer != null) {
            try {
                otherServer.takeAll(template);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
        return this.linda.takeAll(template);
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        if (!this.isBackup && this.otherServer != null) {
            try {
                otherServer.readAll(template);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
        return this.linda.readAll(template);
    }

    @Override
    public void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, RemoteCallback remoteCallback) {
        NestedCallback nestedCallback = new NestedCallback(remoteCallback);
        this.linda.eventRegister(mode, timing, template, new AsynchronousCallback(nestedCallback));
        if (!this.isBackup && this.otherServer != null) {
            try {
                this.otherServer.eventRegister(mode, timing, template, remoteCallback);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
    }

    @Override
    public void save(String filePath) {
        ((CentralizedLinda) this.linda).save(filePath);
    }

    @Override
    public void load(String filePath) {
        ((CentralizedLinda) this.linda).load(filePath);
    }

    @Override
    public void registerBackup(LindaServer backup) {
        try {
            this.otherServer = backup;
            for (Tuple tuple : ((CentralizedLinda) this.linda).getAllTuples()) {
                this.otherServer.write(tuple);
            }
        } catch (RemoteException e) {
            System.err.println(e);
        }
    }

    @Override
    public void unregisterBackup() {
        this.otherServer = null;
    }

    @Override
    public void ping() {
        while (this.isBackup) {
            try {
                System.out.println("Ping...");
                this.otherServer.pong();
                System.out.println("...Pong!");
                Thread.sleep(2000);
            } catch (RemoteException re) {
                try {
                    try {
                        LocateRegistry.createRegistry(4000);
                    } catch (java.rmi.server.ExportException e) {
                        System.out.println("A registry is already running, proceeding...");
                    }
                    Naming.rebind("rmi://localhost:4000/LindaServer", this);
                    this.unregisterBackup();
                    this.isBackup = false;
                } catch (RemoteException | MalformedURLException e) {
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void pong() {

    }

    @Override
    public void debug(String prefix) {
        this.linda.debug(prefix);
    }

}
