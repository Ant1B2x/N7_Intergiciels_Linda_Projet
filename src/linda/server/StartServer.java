package linda.server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/** Création d'un serveur de nom intégré et d'un objet accessible à distance.
 *  Si la création du serveur de nom échoue, on suppose qu'il existe déjà (rmiregistry) et on continue. */
public class StartServer {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 4000;

    public static void main (String args[]) throws Exception {
        // Création du serveur de noms
        try {
            LocateRegistry.createRegistry(SERVER_PORT);
        } catch (java.rmi.server.ExportException e) {
            System.out.println("A registry is already running, proceeding...");
        }

        // Création de l'objet Carnet,
        // et enregistrement du carnet dans le serveur de nom
        LindaServer linda = new LindaServerImpl();
        Naming.rebind("rmi://" + SERVER_HOST + ":" + SERVER_PORT + "/LindaServer", linda);

        // Service prêt : attente d'appels
        System.out.println ("The system is ready on port: " + SERVER_PORT + ".");
    }

}
