package cz.kiv.zcu.ds.bankserver.util;

import cz.kiv.zcu.ds.bankserver.domain.Account;
import cz.kiv.zcu.ds.bankserver.domain.Message;
import cz.kiv.zcu.ds.bankserver.domain.MessageType;

import java.util.*;

/**
 * Provides methods for storing and logging global state in memory.
 */
public class LocalStateLogger {

    /**
     * ID of hosting node
     */
    private int nodeIdx;

    /**
     * State of hosting node
     */
    private int nodeState;

    /**
     * History of messages from logging time interval divided by communication channel.
     */
    private Map<Integer, List<Integer>> channelsState;

    /**
     * Defines which channels can be logged.
     */
    private BitSet logging;

    /**
     * Defines state logger
     * @param nodeIdx - hosting node ID
     * @param account - account reference
     */
    public LocalStateLogger(int nodeIdx, Account account) {
        this.nodeIdx = nodeIdx;
        this.nodeState = account.getBalance();
        this.channelsState = new HashMap<>();
        this.logging = new BitSet();
    }

    /**
     * Store message details.     *
     * @param message - received message
     */
    public void saveMessage(Message message) {
        if (channelsState.containsKey(message.getFrom())) {
            storeAmount(channelsState.get(message.getFrom()), message);
        }
        else {
            List<Integer> list = new ArrayList<>();
            storeAmount(list, message);

            channelsState.put(message.getFrom(), list);
        }
    }

    /**
     * Store amount of transferred money.
     * @param channel - reference to specific channel history
     * @param message - received message
     */
    private void storeAmount(List<Integer> channel, Message message) {
        if (MessageType.CREDIT.toString().equals(message.getType())) {
            channel.add(message.getNumData());
        }
        else if (MessageType.DEBIT.toString().equals(message.getType())) {
            channel.add(-1 * message.getNumData());
        }
    }

    /**
     * Enables logging for all specified nodes based on their ID.     *
     * @param nodeIdxs - array of nodes IDs
     */
    public void startLogging(int[] nodeIdxs) {
        for (int nodeIdx : nodeIdxs) {
            logging.set(nodeIdx);
        }
    }

    /**
     * Disable logging messages from specified hosting node.
     * @param nodeIdx - node ID
     */
    public void stopLogging(int nodeIdx) {
        logging.clear(nodeIdx);
    }

    /**
     * Checks if specified node is currently logged.
     * @param nodeIdx - node ID
     * @return - true = node is logged
     */
    public boolean isLogging(int nodeIdx) {
        return logging.get(nodeIdx);
    }

    /**
     * Checks if is logging disabled for all nodes.
     * @return - true = logging disabled for all nodes
     */
    public boolean isLoggingDone() {
        return logging.isEmpty();
    }

    /**
     * Composes report from CL algorithm for this node.
     * @return - formatted report message
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n---------------------------------------------------------------\n");
        sb.append("STATE OF NODE No.").append(nodeIdx).append("\n");
        sb.append("Amount: ").append(nodeState).append("\n");

        for (Map.Entry<Integer, List<Integer>> entry: channelsState.entrySet()) {
            sb.append("Channel: ").append(entry.getKey()).append(" => ").append(nodeIdx).append("\n");
            for (Integer amount: entry.getValue()) {
                sb.append(amount).append(",- CZK").append("\n");
            }
        }
        sb.append("---------------------------------------------------------------\n");

        return sb.toString();
    }
}
