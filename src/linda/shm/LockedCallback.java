package linda.shm;

import linda.Callback;
import linda.Tuple;

import java.util.concurrent.Semaphore;

public class LockedCallback implements Callback {

    private Tuple tuple;
    private Semaphore semaphore;

    public LockedCallback() {
        this.semaphore = new Semaphore(0);
    }

    @Override
    public void call(Tuple t) {
        this.tuple = t;
        this.semaphore.release();
    }

    public Tuple getTuple() {
        return this.tuple;
    }

    public void await() {
        try {
            this.semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
