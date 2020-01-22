package cz.kiv.zcu.ds.bankserver.zmq;

import cz.kiv.zcu.ds.bankserver.domain.Account;
import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.Message;
import cz.kiv.zcu.ds.bankserver.domain.MessageType;
import cz.kiv.zcu.ds.bankserver.util.LocalStateLogger;
import cz.kiv.zcu.ds.bankserver.util.Utils;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;

/**
 * Provides methods to handling Chandy-Lamport algorithm flow. Creates listeners of type {@link Listener} for specified
 * port.
 */
public class ListenerManager {

    private static Logger logger = LoggerFactory.getLogger(ListenerManager.class);

    /**
     * Hosting node ID
     */
    private int selfNodeNumber;

    /**
     * Map of state loggers for running algorithm
     */
    private HashMap<Integer, LocalStateLogger> lslMap;

    /**
     * Store free ID for new global state snapshot algorithm
     */
    private int lslCounter;

    /**
     * Map of flags that defines if algorithm ends and prints report or not. Flags are separated for each
     * running algorithm.
     */
    private HashMap<Integer, Boolean> printed;

    /**
     * Initializes manager.
     * @param nodeNumber - hosting node ID
     */
    public ListenerManager(int nodeNumber) {
        this.selfNodeNumber = nodeNumber;
        this.lslMap = new HashMap<>();
        this.printed = new HashMap<>();
        this.lslCounter = 0;
    }

    /**
     * Creates listener.
     * @param port - listening port
     * @return - instance of listener worker
     */
    public Listener createListener(int port) {
        return new Listener(port);
    }

    /**
     * Initialize CL algorithm and defines his ID. Thread safe.
     * @param neighbours - array of channels that must be logged
     * @return - global state ID or error (-1)
     */
    private synchronized int startAlgorithm(int[] neighbours) {
        logger.debug("Starting Chandy-Lamport algorithm with ID {}.", lslCounter);

        int result = startStateLogger(lslCounter, neighbours);
        if (result == -1) {
            return result;
        }

        lslCounter++;
        return lslCounter - 1;
    }

    /**
     * Spreads MARKER message to neighbours and starts logging in specified channels. Thread safe.
     * @param lslID - global state ID
     * @param neighbours - array of channels that must be logged
     * @return - global state ID
     */
    private synchronized int startStateLogger(int lslID, int[] neighbours) {
        if (lslMap.containsKey(lslID)) return -1;

        printed.put(lslID, false);
        LocalStateLogger lsl = new LocalStateLogger(selfNodeNumber, Account.getInstance());
        lsl.startLogging(neighbours);
        lslMap.put(lslID, lsl);

        logger.debug("Starting logging node state for ID {} ...", lslID);

        return lslID;
    }

    /**
     * Stop logging for specified copy of running algorithm for specified node. Thread safe.
     * @param lslID - global state ID
     * @param nodeID - node ID
     */
    private synchronized void stopLoggingChannelByNodeID(int lslID, int nodeID) {
        lslMap.get(lslID).stopLogging(nodeID);
        logger.debug("Stopping logging messages from node {} for ID {}.", nodeID, lslID);
    }

    /**
     * Removes global state logger and flag for given global state ID.
     * @param lslID - global state ID
     */
    private synchronized void deleteLocalState(int lslID) {
        logger.debug("Deleting deleteLocal state for ID {}.", lslID);
        printed.remove(lslID);
        lslMap.remove(lslID);
    }

    /**
     * Thread safe method that ends algorithm and send report to specified queue.
     * @param lslID - global state ID
     */
    private synchronized void finishAlgorithm(int lslID) {
        LocalStateLogger lsl = lslMap.get(lslID);
        if (lsl.isLoggingDone() && !printed.get(lslID)) {
            logger.debug("Final state for Node-{} with Global state ID {}, balance: {}.", selfNodeNumber, lslID, Account.getInstance().getBalance());
            Sender.sendLocalState(selfNodeNumber, lsl.toString());
            // reset local state
            deleteLocalState(lslID);
        }
    }

    /**
     * Provide methods for handling received messages.
     */
    public class Listener extends Thread {

        /**
         * Listening port
         */
        private int port;

        /**
         * Creates listener
         * @param port - listening port
         */
        private Listener(int port) {
            this.port = port;
        }

        /**
         * Listening on defined port and handling received messages.
         */
        @Override
        public void run() {
            ThreadContext.put("nodeID", selfNodeNumber + "");

            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PAIR);
            socket.bind("tcp://*:" + port);

            logger.info("Start listening on port " + port + " ...");

            while (!isInterrupted()) {
                handleReceivedMessage(Utils.deserialize(socket.recvStr()));
            }

            socket.close();
        }

        /**
         * Handles received messages.
         * @param message - received custom ZeroMQ message
         */
        private void handleReceivedMessage(Message message) {
            logger.debug("Processing message - type: {}, from: {}",
                    message.getType(), message.getFrom());

            // resolve message type
            MessageType type = MessageType.resolve(message.getType());
            if (type == null) {
                logger.error("Bank request operation missing.");
                return;
            }

            if (type == MessageType.MARKER) { // handle CL algorithm flow
                // get global state ID from message
                int receivedGlobalStateID = message.getNumData();

                if (message.getFrom() < 0) { // starts algorithm
                    // Get array of connected nodes
                    int[] neighbours = Config.getNode(selfNodeNumber).getNeighbours();
                    // Start logging messages from all incoming connections
                    int lslID = startAlgorithm(neighbours);
                    if (lslID == -1) {
                        return;
                    }
                    // Send markers to all neighbours
                    Sender.sendMarkers(lslID, selfNodeNumber, neighbours);
                }
                else if (!lslMap.containsKey(receivedGlobalStateID)) { // first MARKER message
                    // Get array of connected nodes
                    int[] neighbours = Config.getNode(selfNodeNumber).getNeighbours();
                    // Start logging messages from all incoming connections
                    int result = startStateLogger(receivedGlobalStateID, neighbours);
                    if (result == -1) {
                        return;
                    }
                    // Stop logging messages from sender
                    stopLoggingChannelByNodeID(receivedGlobalStateID, message.getFrom());
                    // Send markers to all neighbours
                    Sender.sendMarkers(receivedGlobalStateID, selfNodeNumber, neighbours);
                }
                else { // second MARKER message
                    // Stop logging messages from sender
                    stopLoggingChannelByNodeID(receivedGlobalStateID, message.getFrom());
                }

                finishAlgorithm(receivedGlobalStateID);
            }
            else if (type == MessageType.GLOBAL_STATE) { // handle global state report printing
                logger.info(message.getStrData());
            }
            else { // handle bank request
                if (message.getNumData() < Config.MIN_AMOUNT || message.getNumData() > Config.MAX_AMOUNT) {
                    logger.error("Invalid bank request amount.");
                    return;
                }

                switch (type) {
                    case CREDIT:
                        performCredit(message);
                        break;
                    case DEBIT:
                        performDebit(message);
                        break;
                }

                logger.debug("Bank request, operation: {}, amount: {}.", message.getType(), message.getNumData());
                logger.debug("Account balance: {}", Account.getInstance().getBalance());
            }
        }

        /**
         * Performs debit operation based on received message details.
         * @param message - bank request
         */
        private void performDebit(Message message) {
            if (!Account.getInstance().debit(message.getNumData())) {
                return; // cannot perform debit, low balance
            }
            Sender.send(selfNodeNumber, message.getFrom(), message.getNumData(), MessageType.CREDIT.toString());

            // Not logging failed debit requests
            for (LocalStateLogger lsl: lslMap.values()) {
                if (lsl.isLogging(message.getFrom())) {
                    lsl.saveMessage(message);
                }
            }
        }

        /**
         * Performs credit operation based on received message details.
         * @param message - bank request
         */
        private void performCredit(Message message) {
            Account.getInstance().credit(message.getNumData());
            // nothing to do, sender already made debit

            for (LocalStateLogger lsl: lslMap.values()) {
                if (lsl.isLogging(message.getFrom())) {
                    lsl.saveMessage(message);
                }
            }
        }

    }

}
