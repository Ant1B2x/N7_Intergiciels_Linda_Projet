package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shared memory implementation of Linda.
 */
public class CentralizedLinda implements Linda {

    /**
     * Classe interne représentant un couple template / callback
     */
    private class Event {
        /**
         * Template recherché par le callback
         */
        private final Tuple template;

        /**
         * Callback à appeler
         */
        private final Callback callback;

        /**
         * Crée un event
         * @param template le template de tuple à rechercher
         * @param callback le callback à appeler quand on aura trouvé un tuple
         */
        public Event(Tuple template, Callback callback) {
            this.template = template;
            this.callback = callback;
        }

        /**
         * Renvoie vrai quand tuple correspond au motif recherché a été trouvé
         * @param tuple le tuple dont on cherche la correspondance
         * @return vrai quand tuple correspond au motif recherché a été trouvé
         */
        public boolean isMatching(Tuple tuple) {
            return tuple.matches(this.template);
        }

        /**
         * Appelle le callback
         * @param t le tuple à envoyer au callback
         */
        public void call(Tuple t) {
            this.callback.call(t);
        }
    }

    /**
     * L'espace de tuples
     * Version thread-safe de l'ArrayList
     * Gère l'accès concurrent
     */
    private Collection<Tuple> tuples;

    /**
     * Événements de lecture
     */
    private Collection<Event> readEvents;

    /**
     * Événements de retirage (mot inventé)
     */
    private Collection<Event> takeEvents;

    /**
     * Crée un Linda centralisé
     */
    public CentralizedLinda() {
        this.tuples = new CopyOnWriteArrayList<>();
        this.readEvents = new CopyOnWriteArrayList<>();
        this.takeEvents = new CopyOnWriteArrayList<>();
    }

    @Override
    public void write(Tuple t) {
        // Appelle et retirer les callbacks read en priorité
        for (Event readEvent : this.readEvents) {
            if (readEvent.isMatching(t)) {
                readEvent.call(t);
                this.readEvents.remove(readEvent);
            }
        }

        // Appelle et retire au plus un callback take en attente
        for (Event takeEvent : this.takeEvents) {
            if (takeEvent.isMatching(t)) {
                takeEvent.call(t);
                this.takeEvents.remove(takeEvent);
                return; // si un take a été fait, on quitte la fonction, pas d'écriture
            }
        }

        // Ajoute le tuple à l'espace partagé (pas de take n'a été fait)
        this.tuples.add(t);
    }

    /**
     * Fait la même chose que write, sans appeler les callback
     * Utile notamment pour le serveur de backup
     * Voir doc de write
     */
    public void writeWithoutCalling(Tuple t) {
        for (Event readEvent : this.readEvents) {
            if (readEvent.isMatching(t)) {
                this.readEvents.remove(readEvent);
            }
        }

        for (Event takeEvent : this.takeEvents) {
            if (takeEvent.isMatching(t)) {
                this.takeEvents.remove(takeEvent);
                return;
            }
        }

        this.tuples.add(t);
    }

    @Override
    public Tuple take(Tuple template) {
        // Créer un callback et l'enregister
        LockedCallback lc = new LockedCallback();
        this.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, template, lc);

        // Attendre que le callback ait été appelé
        lc.await();

        // Retourner le tuple pris
        return lc.getTuple();
    }

    @Override
    public Tuple read(Tuple template) {
        // Créer un callback et l'enregister
        LockedCallback lc = new LockedCallback();
        this.eventRegister(eventMode.READ, eventTiming.IMMEDIATE, template, lc);

        // Attendre que le callback ait été appelé
        lc.await();

        // Retourner le tuple lu
        return lc.getTuple();
    }

    @Override
    public Tuple tryTake(Tuple template) {
        for (Tuple t : this.tuples) {
            if (t.matches(template)) {
                this.tuples.remove(t);
                return t;
            }
        }
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        for (Tuple t : this.tuples) {
            if (t.matches(template)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> result = new ArrayList<>();
        Tuple tuple;
        while ((tuple = tryTake(template)) != null) {
            result.add(tuple);
        }
        return result;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> result = new ArrayList<>();
        for (Tuple tuple : this.tuples) {
            if (tuple.matches(template)) {
                result.add(tuple);
            }
        }
        return result;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        // Initialise le tuple potentiellement lu ou pris à null
        Tuple tuple = null;

        // Si on veut appeler le callback directement si un tuple correspond
        if (timing == eventTiming.IMMEDIATE) {
            // Si on est en mode read, appeler tryRead
            if (mode == eventMode.READ) {
                tuple = this.tryRead(template);
            // Sinon (mode take), appeler tryTake
            } else if (mode == eventMode.TAKE) {
                tuple = this.tryTake(template);
            }
        }

        // Si un tuple a été lu ou pris, appeler le callback
        if (tuple != null) {
            callback.call(tuple);
        // Si aucun tuple trouvé ou qu'on est pas en mode immediate, ajouter le callback à la liste idoine
        } else {
            if (mode == eventMode.READ) {
                this.readEvents.add(new Event(template, callback));
            } else if (mode == eventMode.TAKE) {
                this.takeEvents.add(new Event(template, callback));
            }
        }
    }

    /**
     * Renvoie l'ensemble des tuples de l'espace de tuples
     * @return la liste des tuples
     */
    public Collection<Tuple> getAllTuples() {
        Collection<Tuple> tuplesClone = new ArrayList<>();
        for (Tuple tuple : this.tuples) {
            tuplesClone.add(tuple);
        }
        return tuplesClone;
    }

    /**
     * Sauvegarder l'espace de tuples dans un fichier (sérialisation)
     * @param filePath chemin d'accès du fichier
     */
    public void save(String filePath) {
        try {
            FileOutputStream fileWriter = new FileOutputStream(filePath);
            ObjectOutputStream objectWriter = new ObjectOutputStream(fileWriter);
            objectWriter.writeObject(this.tuples);
            objectWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Fatal IO error with " + filePath);
        }
    }

    /**
     * Lire l'espace de tuples à partir d'un fichier (désérialisation)
     * @param filePath chemin d'accès du fichier
     */
    public void load(String filePath) {
        try {
            FileInputStream fileReader = new FileInputStream(filePath);
            ObjectInputStream objectReader = new ObjectInputStream(fileReader);
            this.tuples = (CopyOnWriteArrayList<Tuple>) objectReader.readObject();
            objectReader.close();
            fileReader.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Fatal IO error with " + filePath);
        }
    }

    @Override
    public void debug(String prefix) {
        /*System.out.println("####### START DEBUG #######");

        System.out.println(prefix);
        System.out.println("TUPLES SPACE ("+this.tuples.size()+")");
        for (Tuple t : this.tuples) {
            System.out.println(t);
        }

        System.out.println("####### END DEBUG #######");*/
    }

}
