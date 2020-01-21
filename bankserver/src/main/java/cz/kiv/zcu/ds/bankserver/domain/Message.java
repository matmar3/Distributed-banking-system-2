package cz.kiv.zcu.ds.bankserver.domain;

public class Message {

    private String strData;

    private int numData;

    private String type;

    private int from;

    public String getStrData() {
        return strData;
    }

    public void setStrData(String strData) {
        this.strData = strData;
    }

    public int getNumData() {
        return numData;
    }

    public void setNumData(int numData) {
        this.numData = numData;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

}
