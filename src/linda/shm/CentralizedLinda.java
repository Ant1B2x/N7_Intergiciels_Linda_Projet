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

    private class Event {
        private final Tuple template;
        private final Callback callback;

        public Event(Tuple template, Callback callback) {
            this.template = template;
            this.callback = callback;
        }

        public boolean isMatching(Tuple tuple) {
            return tuple.matches(this.template);
        }

        public void call(Tuple t) {
            this.callback.call(t);
        }
    }

    private Collection<Tuple> tuples;
    private Collection<Event> readEvents;
    private Collection<Event> takeEvents;

    public CentralizedLinda() {
        this.tuples = new CopyOnWriteArrayList<>();
        this.readEvents = new CopyOnWriteArrayList<>();
        this.takeEvents = new CopyOnWriteArrayList<>();
    }

    @Override
    public void write(Tuple t) {
        for (Event readEvent : this.readEvents) {
            if (readEvent.isMatching(t)) {
                readEvent.call(t);
                this.readEvents.remove(readEvent);
            }
        }

        for (Event takeEvent : this.takeEvents) {
            if (takeEvent.isMatching(t)) {
                takeEvent.call(t);
                this.takeEvents.remove(takeEvent);
                return;
            }
        }

        this.tuples.add(t);
    }

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
        Tuple tuple = null;
        if (timing == eventTiming.IMMEDIATE) {
            if (mode == eventMode.READ) {
                tuple = this.tryRead(template);
            } else if (mode == eventMode.TAKE) {
                tuple = this.tryTake(template);
            }
        }

        if (tuple != null) {
            callback.call(tuple);
        } else {
            if (mode == eventMode.READ) {
                this.readEvents.add(new Event(template, callback));
            } else if (mode == eventMode.TAKE) {
                this.takeEvents.add(new Event(template, callback));
            }
        }
    }

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
