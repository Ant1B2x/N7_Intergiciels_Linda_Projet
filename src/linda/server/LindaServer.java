package linda.server;

import linda.Linda;
import linda.Tuple;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface LindaServer extends Remote {

    void declareServer(String host, int port) throws RemoteException;

    void write(Tuple t) throws RemoteException;

    void writeBackup(Tuple t) throws RemoteException;

    Tuple take(Tuple template) throws RemoteException;

    Tuple read(Tuple template) throws RemoteException;

    Tuple tryTake(Tuple template) throws RemoteException;

    Tuple tryRead(Tuple template) throws RemoteException;

    Collection<Tuple> takeAll(Tuple template) throws RemoteException;

    Collection<Tuple> readAll(Tuple template) throws RemoteException;

    void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, RemoteCallback remoteCallback) throws RemoteException;

    void save(String filePath) throws RemoteException;

    void load(String filePath) throws RemoteException;

    void registerBackup(LindaServer backup) throws RemoteException;

    void ping() throws RemoteException;

    void pong() throws RemoteException;

    void debug(String prefix) throws RemoteException;

}
