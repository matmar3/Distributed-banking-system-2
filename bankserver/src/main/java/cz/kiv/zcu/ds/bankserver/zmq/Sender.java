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

import java.util.HashMap;

public class Sender extends Thread {

    private static Logger logger = LoggerFactory.getLogger(Sender.class);

    private int selfNodeNumber;

    private static volatile HashMap<Integer, ZMQ.Socket> sockets = new HashMap<>();

    public Sender(int nodeNumber) {
       this.selfNodeNumber = nodeNumber;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                // simulate some delay
                sleep(Utils.getUDRSleepTime());

                // send request
                send(selfNodeNumber, Utils.getUDRNodeIdx(selfNodeNumber), Utils.getUDRAmount(), Utils.getUDRBankOperation());
            } catch (InterruptedException e) {
                logger.trace("Cannot perform thead sleep.");
            }
        }

    }

    private static synchronized void createConnection(int senderIdx, String ip, int port) {
        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.PAIR);
        socket.connect("tcp://" + ip + ":" + port);

        logger.debug("Connecting to {} on port {}.", ip, port);

        sockets.put(senderIdx, socket);
    }

    static void send(int senderIdx, int idx, int amount, String operation) {
        Node node = Config.getNode(idx);

        BankRequest a = new BankRequest();
        a.setAmount(15000);
        a.setOperation("CREDIT");
        a.setSender(senderIdx);

        String msg = Utils.serialize(a);

        if (!sockets.containsKey(senderIdx)) {
            createConnection(senderIdx, node.getIp(), node.getPort());
        }
        sockets.get(senderIdx).send(msg.getBytes(ZMQ.CHARSET), 0);

        logger.info("Sending bank request to {} on port {}.", node.getIp(), node.getPort());
    }

    public void closeConnections() {
        for (ZMQ.Socket socket : sockets.values()) {
            socket.close();
        }

        sockets.clear();
    }

}
