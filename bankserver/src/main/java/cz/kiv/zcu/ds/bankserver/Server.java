package cz.kiv.zcu.ds.bankserver;

import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.zmq.Listener;
import cz.kiv.zcu.ds.bankserver.zmq.Sender;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {

        // Determines self node number

        Options options = new Options();

        Option nodeNumber = new Option("n", "node", true, "Node configuration number.");
        nodeNumber.setRequired(true);
        options.addOption(nodeNumber);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        int selfNodeNumber = 0;

        try {
            cmd = parser.parse(options, args);
            selfNodeNumber = Integer.parseInt(cmd.getOptionValue("node"));
            logger.debug("Detected node configuration index: {}", selfNodeNumber);
        } catch (Exception e) {
            formatter.printHelp("bankServer", options);
            System.exit(-1);
        }

        // Runs listener and sender instances

        ExecutorService executor = Executors.newFixedThreadPool(Config.THREADS_C);

        logger.info("Starting servers");

        Listener l = new Listener(selfNodeNumber,5000);
        Sender s = new Sender(selfNodeNumber);

        executor.execute(l);
        executor.execute(s);
        executor.shutdown();

        while (!executor.isTerminated()) {
            // wait until all tasks finish
        }

        // Closing opened sockets
        s.closeConnections();

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
                a.setSender(2);
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
