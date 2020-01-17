package cz.kiv.zcu.ds.bankserver;

import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.zmq.Listener;
import cz.kiv.zcu.ds.bankserver.zmq.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws SocketException, UnknownHostException {

        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            System.out.println(socket.getLocalAddress().getHostAddress());
        }

        ExecutorService executor = Executors.newFixedThreadPool(Config.THREADS_C);

        logger.info("Starting servers");

        Runnable l = new Listener(5000);
        Runnable s = new Sender();

        executor.execute(l);
        executor.execute(s);
        executor.shutdown();

        while (!executor.isTerminated()) {
            // wait until all tasks finish
        }

        logger.info("Stopping servers");

        /*try (ZContext context = new ZContext()) {
            System.out.println("Connecting to hello world server");

            //  Socket to talk to server
            ZMQ.Socket socket = context.createSocket(SocketType.PAIR);
            socket.connect("tcp://10.0.1.12:5000");

            for (int requestNbr = 0; requestNbr != 10; requestNbr++) {
                BankRequest a = new BankRequest();
                a.setAmount(15000);
                a.setOperation("CREDIT");
                a.setBank("a");
                String msg = Utils.serialize(a);
                socket.send(msg.getBytes(ZMQ.CHARSET), 0);

                byte[] reply = socket.recv(0);
                System.out.println(
                        "Received " + new String(reply, ZMQ.CHARSET) + " " +
                                requestNbr
                );
            }
        }*/

    }

}
