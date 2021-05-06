package linda.test;

import linda.Linda;
import linda.Tuple;

import java.util.Collection;

public class TestTakeAll {

    public static void main(String[] a) {
        //final Linda linda = new linda.shm.CentralizedLinda();
        final Linda linda = new linda.server.LindaClient("localhost:4000");

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, String.class);
                Collection<Tuple> res = linda.takeAll(motif);
                if (res.isEmpty()) {
                    System.out.println("(0) Plus de tuples :(");
                }
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
                Collection<Tuple> res = linda.takeAll(motif);
                if (res.isEmpty()) {
                    System.out.println("(1) Plus de tuples :(");
                }
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

                Tuple t2 = new Tuple(3, "peut être");
                System.out.println("(2) write: " + t2);
                linda.write(t2);

                Tuple t3 = new Tuple(4, "pourquoi pas");
                System.out.println("(2) write: " + t3);
                linda.write(t3);

                linda.debug("(2)");

            }
        }.start();

    }
}
