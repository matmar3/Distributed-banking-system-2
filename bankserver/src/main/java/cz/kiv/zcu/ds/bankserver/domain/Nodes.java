package cz.kiv.zcu.ds.bankserver.domain;

import java.util.List;

/**
 * Wrapper for list of nodes.
 */
public class Nodes {

    private List<Node> nodes;

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
}
