package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.Semaphore;

public interface RemoteCallback extends Remote {

    void call(Tuple t) throws RemoteException;

    Semaphore getCalled() throws RemoteException;

    void dispose() throws RemoteException;

}
