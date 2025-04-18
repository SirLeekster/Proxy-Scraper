package ProxyParsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

//this class parses the Geonode JSON
//and adds the proxy info the the proxy list
public class GeonodeParser extends Task<Void> {

    private final String geonodeJSON;

    public GeonodeParser(String genodeJSON) {
        this.geonodeJSON = genodeJSON;
    }

    @Override
    protected Void call() {
        try {
            parseGeonode();
            Logger.getLogger(GeonodeParser.class.getName()).log(Level.INFO, "geonode parsing complete");
        } catch (IOException ex) {
            Logger.getLogger(GeonodeParser.class.getName()).log(Level.SEVERE, "geonode parsing failed", ex);
        }
        return null;
    }

    private void parseGeonode() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(geonodeJSON);

        if (rootNode.has("data") && rootNode.get("data").isArray()) {
            for (JsonNode proxyNode : rootNode.get("data")) {
                String ip = proxyNode.has("ip") ? proxyNode.get("ip").asText() : "";
                String port = proxyNode.has("port") ? proxyNode.get("port").asText() : "";
                String protocol = (proxyNode.has("protocols") && proxyNode.get("protocols").isArray() && proxyNode.get("protocols").size() > 0)
                        ? proxyNode.get("protocols").get(0).asText() : "";

                String ip_port = ip + ":" + port;
                String type_ip_port = protocol + "://" + ip_port;

                ProxyParserResults.proxyList.add(type_ip_port);
            }
        }
    }
}
