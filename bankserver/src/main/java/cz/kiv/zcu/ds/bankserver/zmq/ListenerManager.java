package cz.kiv.zcu.ds.bankserver.zmq;

import cz.kiv.zcu.ds.bankserver.Account;
import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.BankRequest;
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

    private boolean printed = false;

    public ListenerManager(int nodeNumber) {
        this.selfNodeNumber = nodeNumber;
        this.lsl = null;
    }

    public Listener createListener(int port) {
        return new Listener(port);
    }

    private synchronized boolean startStateLogger(int[] neighbours) {
        if (lsl != null) return false;

        lsl = new LocalStateLogger(selfNodeNumber, Account.getInstance());
        lsl.startLogging(neighbours);
        logger.debug("Starting logging global state ...");

        return true;
    }

    private synchronized void stopLoggingChannelByNodeID(int nodeID) {
        lsl.stopLogging(nodeID);
        logger.debug("Stopping logging messages from node {}.", nodeID);
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

        private void handleReceivedMessage(BankRequest bankRequest) {
            logger.debug("Processing message - type: {}, from: {}",
                    bankRequest.getOperation(), bankRequest.getSender());

            MessageType type = MessageType.resolve(bankRequest.getOperation());
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
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Send markers to all neighbours
                    Sender.sendMarkers(selfNodeNumber, neighbours);
                }
                else {
                    // Stop logging messages from sender
                    stopLoggingChannelByNodeID(bankRequest.getSender());

                    if (lsl.isLoggingDone() && !printed) {
                        // TODO posli svuj stav do spolecne fronty pro vysledky
                        logger.info(lsl.toString());
                        logger.info("FINAL STATE for NODE-{} is {}.", selfNodeNumber, Account.getInstance().getBalance());
                        printed = true;
                    }
                }
            }
            else {
                if (bankRequest.getAmount() < Config.MIN_AMOUNT || bankRequest.getAmount() > Config.MAX_AMOUNT) {
                    logger.error("Invalid bank request amount.");
                    return;
                }

                switch (type) {
                    case CREDIT:
                        performCredit(bankRequest);
                        break;
                    case DEBIT:
                        performDebit(bankRequest);
                        break;
                }

                logger.debug("Bank request, operation: {}, amount: {}.", bankRequest.getOperation(), bankRequest.getAmount());
                logger.debug("Account balance: {}", Account.getInstance().getBalance());
            }
        }

        private void performDebit(BankRequest bankRequest) {
            if (!Account.getInstance().debit(bankRequest.getAmount())) {
                return;
            }
            Sender.send(selfNodeNumber, bankRequest.getSender(), bankRequest.getAmount(), MessageType.CREDIT.toString());

            // Not logging failed debit requests
            if (lsl != null && lsl.isLogging(bankRequest.getSender())) {
                lsl.saveMessage(bankRequest);
            }
        }

        private void performCredit(BankRequest bankRequest) {
            Account.getInstance().credit(bankRequest.getAmount());
            // nothing to do, sender already made debit

            if (lsl != null && lsl.isLogging(bankRequest.getSender())) {
                lsl.saveMessage(bankRequest);
            }
        }

    }

}
