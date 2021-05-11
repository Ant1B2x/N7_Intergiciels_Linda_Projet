package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface LindaServer extends Remote {

    void write(Tuple t) throws RemoteException;

    Tuple take(Tuple template) throws RemoteException;

    Tuple read(Tuple template) throws RemoteException;

    Tuple tryTake(Tuple template) throws RemoteException;

    Tuple tryRead(Tuple template) throws RemoteException;

    Collection<Tuple> takeAll(Tuple template) throws RemoteException;

    Collection<Tuple> readAll(Tuple template) throws RemoteException;

    Tuple waitEvent(Linda.eventMode mode, Linda.eventTiming timing, Tuple template) throws RemoteException;

    void debug(String prefix) throws RemoteException;

    void save(String filePath) throws RemoteException;

    void load(String filePath) throws RemoteException;

}
