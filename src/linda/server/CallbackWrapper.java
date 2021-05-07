package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CallbackWrapper implements Callback {

    private RemoteCallback remoteCallback;

    public CallbackWrapper(RemoteCallback remoteCallback) {
        this.remoteCallback = remoteCallback;
    }

    @Override
    public void call(Tuple t) {
        try {
            System.out.println("je call remote callback");
            this.remoteCallback.call(t);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        try {
            this.remoteCallback.getCalled().acquire();
            this.remoteCallback.dispose();
        } catch (Exception e) {
        }

        this.remoteCallback = null;
    }

}
