package cz.kiv.zcu.ds.bankserver.util;

import cz.kiv.zcu.ds.bankserver.Account;
import cz.kiv.zcu.ds.bankserver.domain.Message;
import cz.kiv.zcu.ds.bankserver.domain.MessageType;

import java.util.*;

public class LocalStateLogger {

    private int nodeIdx;

    private int nodeState;

    private Map<Integer, List<Integer>> channelsState;

    private BitSet logging;

    public LocalStateLogger(int nodeIdx, Account account) {
        this.nodeIdx = nodeIdx;
        this.nodeState = account.getBalance();
        this.channelsState = new HashMap<>();
        this.logging = new BitSet();
    }

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

    private void storeAmount(List<Integer> channel, Message message) {
        if (MessageType.CREDIT.toString().equals(message.getType())) {
            channel.add(message.getNumData());
        }
        else if (MessageType.DEBIT.toString().equals(message.getType())) {
            channel.add(-1 * message.getNumData());
        }
    }

    public void startLogging(int[] nodeIdxs) {
        for (int nodeIdx : nodeIdxs) {
            logging.set(nodeIdx);
        }
    }

    public void stopLogging(int nodeIdx) {
        logging.clear(nodeIdx);
    }

    public boolean isLogging(int nodeIdx) {
        return logging.get(nodeIdx);
    }

    public boolean isLoggingDone() {
        return logging.isEmpty();
    }

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
