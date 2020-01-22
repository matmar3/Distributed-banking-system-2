package cz.kiv.zcu.ds.bankserver.zmq;

import cz.kiv.zcu.ds.bankserver.domain.Account;
import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.Message;
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

/**
 * Provides methods for responding on received messages and generates random bank operations that sends
 * to random nodes each X seconds.
 */
public class Sender extends Thread {

    private static Logger logger = LoggerFactory.getLogger(Sender.class);

    /**
     * Hosting node ID
     */
    private int selfNodeNumber;

    /**
     * Opened sockets, where number specifies target node ID.
     */
    private static volatile HashMap<Integer, ZMQ.Socket> sockets = new HashMap<>();

    /**
     * Defines sender for specified hosting node.
     * @param nodeNumber - hosting node ID
     */
    public Sender(int nodeNumber) {
       this.selfNodeNumber = nodeNumber;
    }

    /**
     * Generates random bank operations and sends them to random nodes each X seconds.
     */
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

    /**
     * Sends bank request based on given parameters.
     * @param senderIdx - hosting node ID
     * @param receiverIdx - target node ID
     * @param amount - amount of money
     * @param operation - bank operation CREDIT or DEBIT
     */
    static void send(int senderIdx, int receiverIdx, int amount, String operation) {
        Node receiver = Config.getNode(receiverIdx);

        Message a = new Message();
        a.setNumData(amount);
        a.setType(operation);
        a.setFrom(senderIdx);

        String msg = Utils.serialize(a);

        if (!sockets.containsKey(receiverIdx)) {
            createConnection(receiverIdx, receiver.getIp(), 5000 + senderIdx);
        }
        sockets.get(receiverIdx).send(msg.getBytes(ZMQ.CHARSET), 0);

        logger.debug("Sending bank request to {} on port {}.", receiver.getIp(), 5000 + senderIdx);
    }

    /**
     * Sends marker based on given parameters.
     * @param globalStateID - global state identifier
     * @param senderIdx - hosting node ID
     * @param receiversIndexes - target node ID
     */
    static void sendMarkers(int globalStateID, int senderIdx, int[] receiversIndexes) {
        for (int receiverIdx: receiversIndexes) {
            Node node = Config.getNode(receiverIdx);

            Message a = new Message();
            a.setType(MessageType.MARKER.toString());
            a.setFrom(senderIdx);
            a.setNumData(globalStateID);
            String msg = Utils.serialize(a);

            if (!sockets.containsKey(receiverIdx)) {
                createConnection(receiverIdx, node.getIp(), 5000 + senderIdx);
            }
            sockets.get(receiverIdx).send(msg.getBytes(ZMQ.CHARSET), 0);

            logger.debug("Sending marker to {} on port {}.", node.getIp(), 5000 + senderIdx);
        }
    }

    /**
     * Sends Cl algorithm report based on given parameters.
     * @param selfNodeNumber - hosting node ID
     * @param rawString - CL algorithm report
     */
    static void sendLocalState(int selfNodeNumber, String rawString) {
        final int ID = 550 + selfNodeNumber;
        Node node = Config.getNode(0);

        Message rawMessage = new Message();
        rawMessage.setFrom(ID); // results in port 5550 + NodeID
        rawMessage.setType(MessageType.GLOBAL_STATE.toString());
        rawMessage.setStrData(rawString);
        String msg = Utils.serialize(rawMessage);

        if (!sockets.containsKey(ID)) {
            createConnection(ID, node.getIp(), 5000 + ID);
        }
        sockets.get(ID).send(msg.getBytes(ZMQ.CHARSET), 0);

        logger.debug("Sending global state to {} on port {}.", node.getIp(), 5000 + ID);
    }

    /**
     * Close all opened sockets.
     */
    public void closeConnections() {
        for (ZMQ.Socket socket : sockets.values()) {
            socket.close();
        }

        sockets.clear();
    }

}
