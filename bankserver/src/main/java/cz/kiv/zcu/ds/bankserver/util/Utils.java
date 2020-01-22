package cz.kiv.zcu.ds.bankserver.util;

import com.google.gson.Gson;
import cz.kiv.zcu.ds.bankserver.config.Config;
import cz.kiv.zcu.ds.bankserver.domain.Message;
import cz.kiv.zcu.ds.bankserver.domain.MessageType;

import java.util.Random;

/**
 * Provides methods for generating random data for bank requests and methods for JSON serialization/deserialization.
 */
public class Utils {

    // Used references
    private static Random r = new Random();
    private static Gson gson = new Gson();

    /**
     * Deserialize JSON and return message.
     * @param msg  - serialized version of message in JSON format
     * @return - custom ZeroMq message
     */
    public static Message deserialize(String msg) {
        return gson.fromJson(msg, Message.class);
    }

    /**
     * Serialize ZeroMQ message into string version of JSON.
     * @param message - ZeroMQ message
     * @return - serialized JSON
     */
    public static String serialize(Message message) {
        return gson.toJson(message);
    }

    /**
     * Generates uniformly distributed random sleep time in range <MIN_SLEEP_TIME, SLEEP_TIME_INTERVAL_LENGTH + MIN_SLEEP_TIME>.
     * @return - uniformly distributed random sleep time
     */
    public static int getUDRSleepTime() {
        int rand_time = r.nextInt(Config.SLEEP_TIME_INTERVAL_LENGTH) + Config.MIN_SLEEP_TIME;
        return rand_time * 1000;
    }

    /**
     * Generates uniformly distributed random node ID from range <0, number of nodes in config file> except
     * hosting node ID.
     * @param nodeNumber - hosting node ID
     * @return - uniformly distributed random node ID
     */
    public static int getUDRNodeIdx(int nodeNumber) {
        int[] neighbours = Config.getNode(nodeNumber).getNeighbours();

        int idx = r.nextInt(neighbours.length);
        return neighbours[idx];
    }

    /**
     * Generates uniformly distributed random amount of money from range <MIN_AMOUNT, MAX_AMOUNT>.
     * @return - uniformly distributed random amount of money
     */
    public static int getUDRAmount() {
        final int interval_length = Config.MAX_AMOUNT - Config.MIN_AMOUNT;

        return r.nextInt(interval_length) + Config.MIN_AMOUNT;
    }

    /**
     * Generates uniformly distributed random bank operation (message type) from options CREDIT or DEBIT.
     * @return - uniformly distributed random bank operation
     */
    public static String getUDRBankOperation() {
        double number = r.nextDouble();

        if (number <= 0.5) return MessageType.CREDIT.toString();
        else return MessageType.DEBIT.toString();
    }

}
