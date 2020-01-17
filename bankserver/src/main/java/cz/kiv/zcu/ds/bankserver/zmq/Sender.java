package cz.kiv.zcu.ds.bankserver.zmq;

import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.BankRequest;
import cz.kiv.zcu.ds.bankserver.domain.Node;
import cz.kiv.zcu.ds.bankserver.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Sender extends Thread {

    private static Logger logger = LoggerFactory.getLogger(Sender.class);

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                // simulate some delay
                sleep(Utils.getUDRSleepTime());

                // send request
                send(Utils.getUDRNodeIdx(), Utils.getUDRAmount(), Utils.getUDRBankOperation());
            } catch (InterruptedException e) {
                logger.trace("Cannot perform thead sleep.");
            }
        }

    }

    static void send(int idx, int amount, String operation) {
        Node node = Config.getNode(idx);

        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.PAIR);
        socket.connect("tcp://" + node.getIp() + ":" + node.getPort());

        logger.debug("Connecting to {} on port {}.", node.getIp(), node.getPort());

        BankRequest a = new BankRequest();
        a.setAmount(15000);
        a.setOperation("CREDIT");
        a.setSender(0); // TODO jak zjistim odesilatele?

        String msg = Utils.serialize(a);
        socket.send(msg.getBytes(ZMQ.CHARSET), 0);

        logger.info("Sending bank request to {} on port {}.", node.getIp(), node.getPort());
    }
}
