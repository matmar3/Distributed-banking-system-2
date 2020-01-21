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

public class ListenerManager {

    private static Logger logger = LoggerFactory.getLogger(Listener.class);

    private int selfNodeNumber;

    private LocalStateLogger lsl;

    private boolean printed;

    public ListenerManager(int nodeNumber) {
        this.selfNodeNumber = nodeNumber;
        this.lsl = null;
    }

    public Listener createListener(int port) {
        return new Listener(port);
    }

    private synchronized boolean startStateLogger(int[] neighbours) {
        if (lsl != null) return false;

        printed = false;
        lsl = new LocalStateLogger(selfNodeNumber, Account.getInstance());
        lsl.startLogging(neighbours);
        logger.debug("Starting logging global state ...");

        return true;
    }

    private synchronized void stopLoggingChannelByNodeID(int nodeID) {
        lsl.stopLogging(nodeID);
        logger.debug("Stopping logging messages from node {}.", nodeID);
    }

    private synchronized void resetGlobalState() {
        printed = true;
        lsl = null;
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

            if (type == MessageType.MARKER) {
                if (lsl == null) {
                    // Get array of connected nodes
                    int[] neighbours = Config.getNode(selfNodeNumber).getNeighbours();
                    // Start logging messages from all incoming connections
                    if (!startStateLogger(neighbours)) {
                        return;
                    }
                    try {
                        sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Stop logging messages from sender
                    if (message.getFrom() >= 0) {
                        stopLoggingChannelByNodeID(message.getFrom());
                    }
                    // Send markers to all neighbours
                    Sender.sendMarkers(selfNodeNumber, neighbours);
                }
                else {
                    // Stop logging messages from sender
                    stopLoggingChannelByNodeID(message.getFrom());
                }

                if (lsl.isLoggingDone() && !printed) {
                    logger.debug("Final state for Node-{}, balance: {}.", selfNodeNumber, Account.getInstance().getBalance());
                    Sender.sendGlobalState(lsl.toString());
                    // reset global state
                    resetGlobalState();
                }
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
            if (lsl != null && lsl.isLogging(message.getFrom())) {
                lsl.saveMessage(message);
            }
        }

        private void performCredit(Message bankRequest) {
            Account.getInstance().credit(bankRequest.getNumData());
            // nothing to do, sender already made debit

            if (lsl != null && lsl.isLogging(bankRequest.getFrom())) {
                lsl.saveMessage(bankRequest);
            }
        }

    }

}
