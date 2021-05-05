package linda.server;

import linda.shm.CentralizedLinda;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class LindaServerImpl extends UnicastRemoteObject implements LindaServer {

    private CentralizedLinda linda;

    public LindaServerImpl() throws RemoteException {
    }

}
