package linda.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
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

        LindaServer linda = new LindaServerImpl();

        if (args.length > 1) {
            System.err.println("Usage: java StartServer [filepath]");
            System.exit(1);
        }

        if (args.length == 1 && Files.exists(Paths.get(args[0]))) {
            String filePath = args[0];
            System.out.println("Loading tuples from " + filePath + "...");
            try {
                linda.load(filePath);
            } catch (RemoteException e) {
            }
            System.out.println("Tuples loaded.");
        }

        // Enregistrement de linda dans le serveur de nom
        Naming.rebind("rmi://" + SERVER_HOST + ":" + SERVER_PORT + "/LindaServer", linda);

        // Intercept CTRL+C to save tuples to file
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutdown requested, proceeding...");

                if (args.length == 1) {
                    String filePath = args[0];
                    System.out.println("Saving tuples to " + filePath + "...");
                    try {
                        linda.save(filePath);
                    } catch (RemoteException e) {
                    }
                    System.out.println("Tuples saved.");
                }

                Runtime.getRuntime().halt(0);
            }
        });

        // Service prêt : attente d'appels
        System.out.println ("The system is ready on port: " + SERVER_PORT + ".");
    }

}
