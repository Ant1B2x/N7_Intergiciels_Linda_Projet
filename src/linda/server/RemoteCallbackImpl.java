package linda.server;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Tuple;

import jdk.jshell.spi.ExecutionControl.NotImplementedException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Semaphore;

public class RemoteCallbackImpl extends UnicastRemoteObject implements RemoteCallback {

    private Callback callback;
    private Semaphore called;

    private RemoteCallbackImpl() throws RemoteException, NotImplementedException {
        throw new NotImplementedException("");
    }

    public RemoteCallbackImpl(Callback callback) throws RemoteException {
        this.callback = callback;
        this.called = new Semaphore(0);
    }

    @Override
    public void call(Tuple t) {

        new Thread() {
            public void run() {
                RemoteCallbackImpl.this.callback.call(t);
            }
        }.start();

        //this.callback.call(t);
        System.out.println("remote callback a été call avec t = " + t);
        //System.exit(0);
        this.called.release();
    }

    @Override
    public Semaphore getCalled() {
        return this.called;
    }

    @Override
    public void dispose() {
        System.exit(0);
    }
}
