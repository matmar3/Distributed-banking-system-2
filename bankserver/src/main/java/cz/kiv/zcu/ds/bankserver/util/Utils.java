package cz.kiv.zcu.ds.bankserver.util;

import com.google.gson.Gson;
import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.Message;
import cz.kiv.zcu.ds.bankserver.domain.MessageType;

import java.util.Random;

public class Utils {

    private static Random r = new Random();

    private static Gson gson = new Gson();

    public static Message deserialize(String msg) {
        return gson.fromJson(msg, Message.class);
    }

    public static String serialize(Message message) {
        return gson.toJson(message);
    }

    public static int getUDRSleepTime() {
        int rand_time = r.nextInt(Config.SLEEP_TIME_INTERVAL_LENGTH) + Config.MIN_SLEEP_TIME;
        return rand_time * 1000;
    }

    public static int getUDRNodeIdx(int nodeNumber) {
        int[] neighbours = Config.getNode(nodeNumber).getNeighbours();

        int idx = r.nextInt(neighbours.length);
        return neighbours[idx];
    }

    public static int getUDRAmount() {
        final int interval_length = Config.MAX_AMOUNT - Config.MIN_AMOUNT;

        return r.nextInt(interval_length) + Config.MIN_AMOUNT;
    }

    public static String getUDRBankOperation() {
        double number = r.nextDouble();

        if (number <= 0.5) return MessageType.CREDIT.toString();
        else return MessageType.DEBIT.toString();
    }

}
