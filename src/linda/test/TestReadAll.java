package linda.test;

import linda.Linda;
import linda.Tuple;

import java.util.Collection;

public class TestReadAll {

    public static void main(String[] a) {
        //final Linda linda = new linda.shm.CentralizedLinda();
        final Linda linda = new linda.server.LindaClient("rmi://localhost:4000/LindaServer");

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, String.class);
                Collection<Tuple> res = linda.readAll(motif);
                for (Tuple t : res) {
                    System.out.println("(0) Resultat:" + t);
                }
                linda.debug("(0)");
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, String.class);
                Collection<Tuple> res = linda.readAll(motif);
                for (Tuple t : res) {
                    System.out.println("(1) Resultat:" + t);
                }
                linda.debug("(1)");
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Tuple t1 = new Tuple(4, "oui");
                System.out.println("(2) write: " + t1);
                linda.write(t1);

                Tuple t11 = new Tuple(6, "non");
                System.out.println("(2) write: " + t11);
                linda.write(t11);

                Tuple t2 = new Tuple(3, "peut_être");
                System.out.println("(2) write: " + t2);
                linda.write(t2);

                Tuple t3 = new Tuple(4, "pourquoi_pas");
                System.out.println("(2) write: " + t3);
                linda.write(t3);

                linda.debug("(2)");

            }
        }.start();

    }
}
