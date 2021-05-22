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

    /**
     * L'espace de tuple en mémoire partagé à utiliser
     */
    private Linda linda;

    /**
     * Hôte de l'URI RMI sur laquelle le serveur Linda est déclaré
     */
    private String host;

    /**
     * Port de l'URI RMI sur laquelle le serveur Linda est déclaré
     */
    private int port;

    /**
     * Serveur Linda secondaire ("de backup") pour le serveur principal
     * Ou serveur principal pour le serveur secondaire
     */
    private LindaServer otherServer;

    /**
     * Vrai si le serveur courant est un serveur secondaire ("de backup")
     * Faux sinon, c'est un serveur principal
     */
    private boolean isBackup;

    /**
     * Crée un serveur Linda et l'initialise avec un Linda en mémoire partagée
     * @throws RemoteException si il y a un problème de réseau
     */
    public LindaServerImpl() throws RemoteException {
        this.linda = new CentralizedLinda();
    }

    @Override
    public void declareServer(String host, int port) throws RemoteException {
        // "Sauvegarde" l'adresse et le port de l'URI
        this.host = host;
        this.port = port;
        try {
            // Enregistre le serveur courant en tant que serveur principal
            Naming.bind("rmi://"+this.host+":"+this.port+"/LindaServer", this);
            this.isBackup = false;
        } catch (AlreadyBoundException abe) { // Si un serveur est déjà déclaré
            try {
                // Enregistre le serveur courant en tant que serveur secondaire
                this.otherServer = (LindaServer) Naming.lookup("rmi://"+this.host+":"+this.port+"/LindaServer");
                this.isBackup = true;
                // S'enregistre comme backup auprès du serveur principal
                this.otherServer.registerBackup(this);
                // Lance un thread qui s'occupera de vérifier l'état du serveur principal
                new Thread(this::pollPrimary).start();
            } catch (MalformedURLException | NotBoundException e) {
                System.err.println(e);
            }
        } catch (MalformedURLException e) {
            System.err.println(e);
        }
    }

    @Override
    public void write(Tuple t) {
        // Si un serveur de backup est présent, également écrire le tuple dessus
        // Mais ne re-déclenche pas l'appel des callbacks pour éviter qu'ils soient appelés deux fois
        if (!this.isBackup && this.otherServer != null) {
            try {
                otherServer.writeBackup(t);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
        this.linda.write(t);
    }

    @Override
    public void writeBackup(Tuple t) {
        // S'occupe d'appeler l'écriture sans appel de callbacks du Linda centralisé
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
        // Remplace waitEvent() de la version précédente

        // Créé un callback encapsulant le callback remote reçu et l'enregistre dans le noyau Linda
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
    public void registerBackup(LindaServer backupServer) {
        try {
            // Enregistre backupServer comme serveur de backup
            this.otherServer = backupServer;
            // Lui envoie tous les tuples courants
            for (Tuple tuple : ((CentralizedLinda) this.linda).getAllTuples()) {
                this.otherServer.write(tuple);
            }
        } catch (RemoteException e) {
            System.err.println(e);
        }
    }

    /**
     * Supprime le serveur de backup
     * Appelé par les méthodes de la classe quand celui-ci ne répond pas
     */
    private void unregisterBackup() {
        this.otherServer = null;
    }

    /**
     * Permet au serveur courant de devenir le serveur principal
     * Appelé par pollPrimary() quand le serveur de backup détecte que le serveur principal ne répond plus
     */
    private void becomePrimary() {
        try {
            // Recrée un registre au besoin
            try {
                LocateRegistry.createRegistry(4000);
            } catch (java.rmi.server.ExportException e) {
                System.out.println("A registry is already running, proceeding...");
            }
            // S'enregistre en tant que serveur principal, supprime le serveur de backup
            Naming.rebind("rmi://"+this.host+":"+this.port+"/LindaServer", this);
            this.unregisterBackup();
            this.isBackup = false;
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pollPrimary() {
        // Appelle keepAlive() puis attend 2s, et recommence
        while (this.isBackup) {
            try {
                System.out.println("Ping...");
                this.otherServer.keepAlive();
                System.out.println("...Pong!");
                Thread.sleep(2000);
            } catch (RemoteException re) {
                this.becomePrimary(); // Si une erreur réseau survient, devient le serveur principal
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
    }

    @Override
    public void keepAlive() {

    }

    @Override
    public void debug(String prefix) {
        this.linda.debug(prefix);
    }

}
