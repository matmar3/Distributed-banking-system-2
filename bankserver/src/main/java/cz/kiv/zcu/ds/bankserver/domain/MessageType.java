package cz.kiv.zcu.ds.bankserver.domain;

/**
 * Types of ZeroMQ messages that this applications supports.
 */
public enum MessageType {

    CREDIT,         // Credit bank request
    DEBIT,          // Debit bank request
    MARKER,         // Marker message for CL algorithm
    GLOBAL_STATE;   // Result of CL algorithm

    public static MessageType resolve(String messageType) {
        for (MessageType m: values()) {
            if (m.toString().equals(messageType)) {
                return m;
            }
        }

        return null;
    }

}
