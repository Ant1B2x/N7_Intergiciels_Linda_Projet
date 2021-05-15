package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;

public class NestedCallback implements Callback {

    private final RemoteCallback remoteCallback;

    public NestedCallback(RemoteCallback remoteCallback) {
        this.remoteCallback = remoteCallback;
    }

    @Override
    public void call(Tuple t) {
        try {
            this.remoteCallback.call(t);
        } catch (RemoteException e) {
            System.err.println(e);
        }
    }

}
