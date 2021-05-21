package linda.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/** Création d'un serveur de nom intégré et d'un objet accessible à distance.
 *  Si la création du serveur de nom échoue, on suppose qu'il existe déjà (rmiregistry) et on continue. */
public class StartServer {

    /**
     * Hôte de l'URI RMI
     */
    private static final String SERVER_HOST = "localhost";
    /**
     * Port de l'URI RMI
     */
    private static final int SERVER_PORT = 4000;

    public static void main (String args[]) throws Exception {
        // Création du serveur de noms
        try {
            LocateRegistry.createRegistry(SERVER_PORT);
        } catch (java.rmi.server.ExportException e) {
            System.out.println("A registry is already running, proceeding...");
        }

        // Créer le serveur linda
        LindaServer lindaServer = new LindaServerImpl();

        // Si plus d'un argument, erreur
        if (args.length > 1) {
            System.err.println("Usage: java StartServer [filepath]");
            System.exit(1);
        }

        // Gérer l'argument en ligne de commande
        final String filePath;
        if (args.length == 1) {
            filePath = args[0];
        } else {
            filePath = null;
        }

        // Charger les tuples du fichier (si le fichier existe)
        if (filePath != null && Files.exists(Paths.get(filePath))) {
            System.out.println("Loading tuples from " + filePath + "...");
            try {
                lindaServer.load(filePath);
            } catch (RemoteException e) {
            }
            System.out.println("Tuples loaded.");
        }

        // Enregistrement de linda dans le serveur de nom
        Naming.rebind("rmi://" + SERVER_HOST + ":" + SERVER_PORT + "/LindaServer", lindaServer);

        // Intercepter CTRL+C pour sauvegarder les tuples dans le fichier
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutdown requested, proceeding...");

                // Si fichier passé en ligne de commande
                if (filePath != null) {
                    System.out.println("Saving tuples to " + filePath + "...");
                    try {
                        lindaServer.save(filePath);
                    } catch (RemoteException e) {
                    }
                    System.out.println("Tuples saved.");
                }

                Runtime.getRuntime().halt(0);
            }
        });

        // Service prêt : attente d'appels
        System.out.println("The system is ready on port: " + SERVER_PORT + ".");
    }

}
