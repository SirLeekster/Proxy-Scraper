package ProxyFetchers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

//this class fetches proxies from the Geonode API 
//and sends the JSON result to be parsed by the Parser class.
public class GeonodeFetcher extends Task<String> {

    @Override
    protected String call() {
        try {
            fetchGeonode();
            Logger.getLogger(GeonodeFetcher.class.getName()).log(Level.INFO, "geonode fetching complete");
        } catch (IOException ex) {
            Logger.getLogger(GeonodeFetcher.class.getName()).log(Level.SEVERE, "geonode fetching failed", ex);
        }
        return null;
    }

    private String fetchGeonode() throws IOException {
        URL url = new URL("https://proxylist.geonode.com/api/proxy-list?limit=500&page=1&sort_by=lastChecked&sort_type=desc");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(50000);
        conn.setReadTimeout(50000);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Logger.getLogger(GeonodeFetcher.class.getName()).log(Level.WARNING,
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
