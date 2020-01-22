package cz.kiv.zcu.ds.bankserver.domain;

/**
 * Node configuration.
 */
public class Node {

    private String ip;
    private int[] ports;
    private int[] neighbours;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int[] getPorts() {
        return ports;
    }

    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    public int[] getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(int[] neighbours) {
        this.neighbours = neighbours;
    }
}
