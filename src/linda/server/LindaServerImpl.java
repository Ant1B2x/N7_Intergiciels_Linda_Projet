package linda.server;

import linda.AsynchronousCallback;
import linda.Linda;
import linda.Tuple;
import linda.TupleFormatException;
import linda.shm.CentralizedLinda;
import linda.shm.LockedCallback;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

public class LindaServerImpl extends UnicastRemoteObject implements LindaServer {

    private Linda linda;

    public LindaServerImpl() throws RemoteException {
        this.linda = new CentralizedLinda();
    }

    @Override
    public void write(Tuple t) {
        this.linda.write(t);
    }

    @Override
    public Tuple take(Tuple template) {
        return this.linda.take(template);
    }

    @Override
    public Tuple read(Tuple template) {
        return this.linda.read(template);
    }

    @Override
    public Tuple tryTake(Tuple template) {
        return this.linda.tryTake(template);
    }

    @Override
    public Tuple tryRead(Tuple template) {
        return this.linda.tryRead(template);
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        return this.linda.takeAll(template);
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        return this.linda.readAll(template);
    }

    @Override
    public Tuple waitEvent(Linda.eventMode mode, Linda.eventTiming timing, Tuple template) {
        LockedCallback lc = new LockedCallback();
        this.linda.eventRegister(mode, timing, template, new AsynchronousCallback(lc));
        lc.await();
        return lc.getTuple();
    }

    @Override
    public void debug(String prefix) {
        this.linda.debug(prefix);
    }

    @Override
    public void save(String filePath) {
        this.linda.save(filePath); // Obliger de caster ici, c'est moche
    }

    @Override
    public void load(String filePath) {
        this.linda.load(filePath); // Obliger de caster ici, c'est moche
    }

}
