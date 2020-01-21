package cz.kiv.zcu.ds.bankserver.zmq;

import cz.kiv.zcu.ds.bankserver.Account;
import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.BankRequest;
import cz.kiv.zcu.ds.bankserver.domain.MessageType;
import cz.kiv.zcu.ds.bankserver.domain.Node;
import cz.kiv.zcu.ds.bankserver.util.Utils;
import org.apache.logging.log4j.ThreadContext;
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
        ThreadContext.put("nodeID", selfNodeNumber + "");

        while (!isInterrupted()) {
            try {
                // simulate some delay
                sleep(Utils.getUDRSleepTime());

                // generated data to send
                int receiverNodeIdx = Utils.getUDRNodeIdx(selfNodeNumber);
                int amount = Utils.getUDRAmount();
                String operation = Utils.getUDRBankOperation();

                // decrease balance if sending credit
                if (MessageType.resolve(operation) == MessageType.CREDIT) {
                    if (!Account.getInstance().debit(amount)) {
                        continue; // cannot send this amount of money
                    }
                }

                // send request
                send(selfNodeNumber, receiverNodeIdx, amount, operation);
            } catch (InterruptedException e) {
                logger.trace("Cannot perform thead sleep.");
            }
        }
    }

    /**
     * Creating socket for communication between selfNode and targetNode, where IP and port is for target.
     * Target listening on port 5000 + sender's nodeID to distinguish channels.
     *
     * @param targetNodeIdx - Node ID of communication target
     * @param ip - target's IP address
     * @param port - target's port (listening on port 5000 + sender's nodeID)
     */
    private static synchronized void createConnection(int targetNodeIdx, String ip, int port) {
        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.PAIR);
        socket.connect("tcp://" + ip + ":" + port);

        logger.debug("Connecting to {} on port {}.", ip, port);

        sockets.put(targetNodeIdx, socket);
    }

    static void send(int senderIdx, int receiverIdx, int amount, String operation) {
        Node receiver = Config.getNode(receiverIdx);

        BankRequest a = new BankRequest();
        a.setAmount(amount);
        a.setOperation(operation);
        a.setSender(senderIdx);

        String msg = Utils.serialize(a);

        if (!sockets.containsKey(receiverIdx)) {
            createConnection(receiverIdx, receiver.getIp(), 5000 + senderIdx);
        }
        sockets.get(receiverIdx).send(msg.getBytes(ZMQ.CHARSET), 0);

        logger.debug("Sending bank request to {} on port {}.", receiver.getIp(), 5000 + senderIdx);
    }

    static void sendMarkers(int senderIdx, int[] receiversIndexes) {
        for (int receiverIdx: receiversIndexes) {
            Node node = Config.getNode(receiverIdx);

            BankRequest a = new BankRequest();
            a.setOperation(MessageType.MARKER.toString());
            a.setSender(senderIdx);
            String msg = Utils.serialize(a);

            if (!sockets.containsKey(receiverIdx)) {
                createConnection(receiverIdx, node.getIp(), 5000 + senderIdx);
            }
            sockets.get(receiverIdx).send(msg.getBytes(ZMQ.CHARSET), 0);

            logger.debug("Sending marker to {} on port {}.", node.getIp(), 5000 + senderIdx);
        }
    }

    public void closeConnections() {
        for (ZMQ.Socket socket : sockets.values()) {
            socket.close();
        }

        sockets.clear();
    }

}
