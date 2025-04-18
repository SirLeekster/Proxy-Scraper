package ProxyFetchers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

//this class fetches proxies from the ProxyScrape API 
//and sends the JSON result to be parsed by the Parser class.
public class ProxyScrapeFetcher extends Task<String> {

    @Override
    protected String call() {
        try {
            Logger.getLogger(ProxyScrapeFetcher.class.getName()).log(Level.INFO, "proxyscrape fetching complete");
            return fetchProxyScrape();
        } catch (IOException ex) {
            Logger.getLogger(ProxyScrapeFetcher.class.getName()).log(Level.SEVERE, "proxyscrape fetching failed", ex);
            return null;
        }
    }

    private String fetchProxyScrape() throws IOException {
        URL url = new URL("https://api.proxyscrape.com/v3/free-proxy-list/get?request=displayproxies&proxy_format=protocolipport&format=json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Logger.getLogger(ProxyScrapeFetcher.class.getName()).log(Level.WARNING,
                    "Failed to fetch data, HTTP response code: " + responseCode);
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
