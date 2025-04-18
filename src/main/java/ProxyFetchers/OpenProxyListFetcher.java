package ProxyFetchers;

import javafx.concurrent.Task;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

//this class fetches proxies from the OpenProxyList API 
//and sends the JSON result to be parsed by the Parser class.
public class OpenProxyListFetcher extends Task<String> {

    @Override
    protected String call() {
        try {
            Logger.getLogger(OpenProxyListFetcher.class.getName()).log(Level.INFO, "openproxylist fetching complete");
            return fetchedOpenProxyList();
        } catch (IOException ex) {
            Logger.getLogger(OpenProxyListFetcher.class.getName()).log(Level.SEVERE, "openproxylist fetching failed", ex);
            return null;
        }
    }

    public static String fetchOpenProxyListHTTP() throws IOException {
        URL url = new URL("https://api.openproxylist.xyz/http.txt");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Logger.getLogger(OpenProxyListFetcher.class.getName()).log(Level.WARNING,
                    "Failed to fetch data, HTTP response code: " + responseCode);
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append("http://").append(line).append("\n");
            }
        }
        return response.toString();
    }

    public static String fetchOpenProxyListSOCKS4() throws IOException {
        URL url = new URL("https://api.openproxylist.xyz/socks4.txt");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Logger.getLogger(OpenProxyListFetcher.class.getName()).log(Level.WARNING,
                    "Failed to fetch data, HTTP response code: " + responseCode);
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append("socks4://").append(line).append("\n");
            }
        }
        return response.toString();
    }

    public static String fetchOpenProxyListSOCKS5() throws IOException {
        URL url = new URL("https://api.openproxylist.xyz/socks5.txt");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Logger.getLogger(OpenProxyListFetcher.class.getName()).log(Level.WARNING,
                    "Failed to fetch data, HTTP response code: " + responseCode);
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append("socks5://").append(line).append("\n");
            }
        }
        return response.toString();
    }

    private static String fetchedOpenProxyList() throws IOException {
        String httpProxies = fetchOpenProxyListHTTP();
        String socks4Proxies = fetchOpenProxyListSOCKS4();
        String socks5Proxies = fetchOpenProxyListSOCKS5();
        String allProxies = httpProxies + socks4Proxies + socks5Proxies;

        return allProxies;
    }
}
