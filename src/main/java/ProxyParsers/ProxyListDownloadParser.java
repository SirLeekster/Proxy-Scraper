package ProxyParsers;

import javafx.concurrent.Task;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

//this class parses the ProxyListDownload JSON
//and adds the proxy info the the proxy list
public class ProxyListDownloadParser extends Task<Void> {

    private final String response;

    public ProxyListDownloadParser(String response) {
        this.response = response;
    }

    @Override
    protected Void call() {
        try {
            parseProxyListDownload();
            Logger.getLogger(ProxyListDownloadParser.class.getName()).log(Level.INFO, "proxylistdownload parsing complete");
        } catch (IOException ex) {
            Logger.getLogger(ProxyListDownloadParser.class.getName()).log(Level.SEVERE, "proxylistdownload parsing failed", ex);
        }
        return null;
    }

    private void parseProxyListDownload() throws IOException {
        String[] proxies = response.split("\n");
        for (String proxy : proxies) {
            if (isCancelled()) {
                break;
            }
            ProxyParserResults.proxyList.add(proxy);
        }
    }
}
