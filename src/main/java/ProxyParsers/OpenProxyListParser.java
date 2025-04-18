package ProxyParsers;

import javafx.concurrent.Task;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

//this class parses the OpenProxyList JSON
//and adds the proxy info the the proxy list
public class OpenProxyListParser extends Task<Void> {

    private final String response;

    public OpenProxyListParser(String response) {
        this.response = response;
    }

    @Override
    protected Void call() {
        try {
            parseOpenProxyList();
            Logger.getLogger(OpenProxyListParser.class.getName()).log(Level.INFO, "openproxylist parsing complete");
        } catch (IOException ex) {
            Logger.getLogger(OpenProxyListParser.class.getName()).log(Level.SEVERE, "openproxylist parsing failed", ex);
        }
        return null;
    }

    private void parseOpenProxyList() throws IOException {
        String[] proxies = response.split("\n");
        for (String proxy : proxies) {
            if (isCancelled()) {
                break;
            }
            ProxyParserResults.proxyList.add(proxy);
        }
    }
}
