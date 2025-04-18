package ProxyParsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

//this class parses the ProxyScrape JSON
//and adds the proxy info the the proxy list
public class ProxyScrapeParser extends Task<Void> {

    private final String proxyScrapeJSON;

    public ProxyScrapeParser(String proxyScrapeJSON) {
        this.proxyScrapeJSON = proxyScrapeJSON;
    }

    @Override
    protected Void call() {
        try {
            parseProxyScrape();
            Logger.getLogger(ProxyScrapeParser.class.getName()).log(Level.INFO, "proxyscrape parsing complete");
        } catch (IOException ex) {
            Logger.getLogger(ProxyScrapeParser.class.getName()).log(Level.SEVERE, "proxyscrape parsing failed", ex);
        }
        return null;
    }

    private void parseProxyScrape() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(proxyScrapeJSON);

        if (rootNode.has("proxies") && rootNode.get("proxies").isArray()) {
            for (JsonNode proxyNode : rootNode.get("proxies")) {
                if (isCancelled()) {
                    break;
                }

                String protocol = proxyNode.has("protocol") ? proxyNode.get("protocol").asText() : "";
                String ip = proxyNode.has("ip") ? proxyNode.get("ip").asText() : "";
                String port = proxyNode.has("port") ? proxyNode.get("port").asText() : "";

                String ip_port = ip + ":" + port;
                String type_ip_port = protocol + "://" + ip_port;

                ProxyParserResults.proxyList.add(type_ip_port);
            }
        }
    }
}
