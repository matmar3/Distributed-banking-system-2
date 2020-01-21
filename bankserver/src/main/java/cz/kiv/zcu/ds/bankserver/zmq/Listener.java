package cz.kiv.zcu.ds.bankserver.zmq;

import cz.kiv.zcu.ds.bankserver.Account;
import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.MessageType;
import cz.kiv.zcu.ds.bankserver.domain.BankRequest;
import cz.kiv.zcu.ds.bankserver.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Listener extends Thread {

    private static Logger logger = LoggerFactory.getLogger(Listener.class);

    private int selfNodeNumber;

    private int port;

    public Listener(int nodeNumber, int port) {
        this.selfNodeNumber = nodeNumber;
        this.port = port;
    }

    @Override
    public void run() {
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
        logger.debug("Processing bank request - operation: {}, amount: {}, from: {}",
                bankRequest.getOperation(), bankRequest.getAmount(), bankRequest.getSender());

        MessageType type = MessageType.resolve(bankRequest.getOperation());
        if (type == null) {
            logger.error("Bank request operation missing.");
            return;
        }

        if (type == MessageType.MARKER) {
            // TODO zpracuj marker
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

            logger.debug("Bank request successfully performed.");
            logger.debug("Account balance: {}", Account.getInstance().getBalance());
        }
    }

    private void performDebit(BankRequest bankRequest) {
        if (!Account.getInstance().debit(bankRequest.getAmount())) {
            return; // TODO pokud je zapnute logovani, mam tuto zpravu logovat?
        }
        Sender.send(selfNodeNumber, bankRequest.getSender(), bankRequest.getAmount(), MessageType.CREDIT.toString());
    }

    private void performCredit(BankRequest bankRequest) {
        Account.getInstance().credit(bankRequest.getAmount());
        // nothing to do, sender already made debit
    }

}
