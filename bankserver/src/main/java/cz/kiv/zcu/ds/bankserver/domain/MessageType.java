package cz.kiv.zcu.ds.bankserver.domain;

public enum MessageType {

    CREDIT,
    DEBIT,
    MARKER;

    public static MessageType resolve(String messageType) {
        for (MessageType m: values()) {
            if (m.toString().equals(messageType)) {
                return m;
            }
        }

        return null;
    }



}
