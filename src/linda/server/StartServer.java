package linda.server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/** Création d'un serveur de nom intégré et d'un objet accessible à distance.
 *  Si la création du serveur de nom échoue, on suppose qu'il existe déjà (rmiregistry) et on continue. */
public class StartServer {

    public static void main (String args[]) throws Exception {
        // Création du serveur de noms
        try {
            LocateRegistry.createRegistry(4000);
        } catch (java.rmi.server.ExportException e) {
            System.out.println("A registry is already running, proceeding...");
        }

        // Création de l'objet Carnet,
        // et enregistrement du carnet dans le serveur de nom
        LindaServer linda = new LindaServerImpl();
        Naming.rebind("rmi://localhost:4000/LindaServer", linda);

        // Service prêt : attente d'appels
        System.out.println ("The system is ready.");
    }
}
