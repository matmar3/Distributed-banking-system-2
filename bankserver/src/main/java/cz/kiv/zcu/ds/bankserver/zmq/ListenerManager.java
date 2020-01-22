package cz.kiv.zcu.ds.bankserver.zmq;

import cz.kiv.zcu.ds.bankserver.Account;
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

public class ListenerManager {

    private static Logger logger = LoggerFactory.getLogger(Listener.class);

    private int selfNodeNumber;

    private HashMap<Integer, LocalStateLogger> lslMap;

    private int lslCounter;

    private HashMap<Integer, Boolean> printed;

    public ListenerManager(int nodeNumber) {
        this.selfNodeNumber = nodeNumber;
        this.lslMap = new HashMap<>();
        this.printed = new HashMap<>();
        this.lslCounter = 0;
    }

    public Listener createListener(int port) {
        return new Listener(port);
    }

    private synchronized int startAlgorithm(int[] neighbours) {
        logger.debug("Starting Chandy-Lamport algorithm with ID {}.", lslCounter);

        int result = startStateLogger(lslCounter, neighbours);
        if (result == -1) {
            return result;
        }

        lslCounter++;
        return lslCounter - 1;
    }

    private synchronized int startStateLogger(int lslID, int[] neighbours) {
        if (lslMap.containsKey(lslID)) return -1;

        printed.put(lslID, false);
        LocalStateLogger lsl = new LocalStateLogger(selfNodeNumber, Account.getInstance());
        lsl.startLogging(neighbours);
        lslMap.put(lslID, lsl);

        logger.debug("Starting logging node state for ID {} ...", lslID);

        return lslID;
    }

    private synchronized void stopLoggingChannelByNodeID(int lslID, int nodeID) {
        lslMap.get(lslID).stopLogging(nodeID);
        logger.debug("Stopping logging messages from node {} for ID {}.", nodeID, lslID);
    }

    private synchronized void deleteLocalState(int lslID) {
        logger.debug("Deleting deleteLocal state for ID {}.", lslID);
        printed.remove(lslID);
        lslMap.remove(lslID);
    }

    private synchronized void finishAlgorithm(int lslID) {
        LocalStateLogger lsl = lslMap.get(lslID);
        if (lsl.isLoggingDone() && !printed.get(lslID)) {
            logger.debug("Final state for Node-{} with Global state ID {}, balance: {}.", selfNodeNumber, lslID, Account.getInstance().getBalance());
            Sender.sendLocalState(selfNodeNumber, lsl.toString());
            // reset local state
            deleteLocalState(lslID);
        }
    }

    public class Listener extends Thread {

        private int port;

        private Listener(int port) {
            this.port = port;
        }

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

        private void handleReceivedMessage(Message message) {
            logger.debug("Processing message - type: {}, from: {}",
                    message.getType(), message.getFrom());

            MessageType type = MessageType.resolve(message.getType());
            if (type == null) {
                logger.error("Bank request operation missing.");
                return;
            }

            int receivedGlobalStateID = message.getNumData();

            if (type == MessageType.MARKER) {
                if (message.getFrom() < 0) {
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
                else if (!lslMap.containsKey(receivedGlobalStateID)) {
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
                else {
                    // Stop logging messages from sender
                    stopLoggingChannelByNodeID(receivedGlobalStateID, message.getFrom());
                }

                finishAlgorithm(receivedGlobalStateID);
            }
            else if (type == MessageType.GLOBAL_STATE) {
                logger.info(message.getStrData());
            }
            else {
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

        private void performDebit(Message message) {
            if (!Account.getInstance().debit(message.getNumData())) {
                return;
            }
            Sender.send(selfNodeNumber, message.getFrom(), message.getNumData(), MessageType.CREDIT.toString());

            // Not logging failed debit requests
            for (LocalStateLogger lsl: lslMap.values()) {
                if (lsl.isLogging(message.getFrom())) {
                    lsl.saveMessage(message);
                }
            }
        }

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
