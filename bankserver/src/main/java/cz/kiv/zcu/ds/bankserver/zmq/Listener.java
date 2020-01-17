package cz.kiv.zcu.ds.bankserver.zmq;

import cz.kiv.zcu.ds.bankserver.Account;
import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.BankOperation;
import cz.kiv.zcu.ds.bankserver.domain.BankRequest;
import cz.kiv.zcu.ds.bankserver.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Listener extends Thread {

    private static Logger logger = LoggerFactory.getLogger(Listener.class);

    private int port;

    public Listener(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.PAIR);
        socket.bind("tcp://*:" + port);

        logger.info("Start listening on port " + port + " ...");

        while (!isInterrupted()) {
            performRequest(Utils.deserialize(socket.recvStr()));
        }
    }

    private void performRequest(BankRequest bankRequest) {
        logger.debug("Processing bank request - operation: {}, amount: {}, from: {}",
                bankRequest.getOperation(), bankRequest.getAmount(), bankRequest.getSender());

        if (bankRequest.getOperation() == null) {
            logger.error("Bank request operation missing.");
            return;
        }

        if (bankRequest.getAmount() < Config.MIN_AMOUNT || bankRequest.getAmount() > Config.MAX_AMOUNT) {
            logger.error("Invalid bank request amount.");
            return;
        }

        if (BankOperation.CREDIT.toString().equals(bankRequest.getOperation())) {
            Account.getInstance().credit(bankRequest.getAmount());
            // nothing to do, sender already made debit
        }
        else {
            Account.getInstance().debit(bankRequest.getAmount());
            Sender.send(bankRequest.getSender(), bankRequest.getAmount(), BankOperation.CREDIT.toString());
        }

        logger.debug("Bank request successfully performed.");
        logger.debug("Account balance: {}", Account.getInstance().getBalance());
    }

}
