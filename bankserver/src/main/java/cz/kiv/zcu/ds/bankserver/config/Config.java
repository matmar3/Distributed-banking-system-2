package cz.kiv.zcu.ds.bankserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.kiv.zcu.ds.bankserver.domain.Node;
import cz.kiv.zcu.ds.bankserver.domain.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class Config {

    public static final int MIN_SLEEP_TIME = 2;
    public static final int SLEEP_TIME_INTERVAL_LENGTH = 45;

    public static final int MIN_AMOUNT = 10000;
    public static final int MAX_AMOUNT = 50000;

    private static Logger logger = LoggerFactory.getLogger(Config.class);

    private static Nodes nodes;

    static {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            nodes = mapper.readValue(Config.class.getClassLoader().getResourceAsStream("nodes.yaml"), Nodes.class);
        } catch (IOException e) {
            logger.error("Cannot loads nodes configuration.");
            System.exit(-2);
        }
    }

    public static Node getNode(int idx) {
        return nodes.getNodes().get(idx);
    }

    public static int nodesCount() {
        return nodes.getNodes().size();
    }

}
