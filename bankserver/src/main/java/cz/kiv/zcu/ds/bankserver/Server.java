package cz.kiv.zcu.ds.bankserver;

import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.Node;
import cz.kiv.zcu.ds.bankserver.zmq.ListenerManager;
import cz.kiv.zcu.ds.bankserver.zmq.ListenerManager.Listener;
import cz.kiv.zcu.ds.bankserver.zmq.Sender;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bank server that manages dynamic pool of threads for {@link Listener} and {@link Sender} instances.
 */
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
            ThreadContext.put("nodeID", selfNodeNumber + "");

            logger.debug("Detected node configuration index: {}", selfNodeNumber);
        } catch (Exception e) {
            formatter.printHelp("bankServer", options);
            System.exit(-1);
        }

        // Runs listener and sender instances

        Node nodeInfo = Config.getNode(selfNodeNumber);
        ExecutorService executor = Executors.newCachedThreadPool();

        logger.info("Starting servers");

        // Listeners
        ListenerManager lm = new ListenerManager(selfNodeNumber);
        for (int port: nodeInfo.getPorts()) {
            ListenerManager.Listener l = lm.createListener(port);
            executor.execute(l);
        }

        // Sender
        Sender s = new Sender(selfNodeNumber);
        executor.execute(s);

        executor.shutdown();

        while (!executor.isTerminated()) {
            // wait until all tasks finish
        }

        // Closing opened sockets
        s.closeConnections();

        logger.info("Stopping servers");
    }

}
