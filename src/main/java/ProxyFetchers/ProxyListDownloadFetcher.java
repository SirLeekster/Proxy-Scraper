package ProxyFetchers;

import javafx.concurrent.Task;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

//this class fetches proxies from the ProxyListDownload API 
//and sends the JSON result to be parsed by the Parser class.
public class ProxyListDownloadFetcher extends Task<String> {

    @Override
    protected String call() {
        try {
            Logger.getLogger(ProxyListDownloadFetcher.class.getName()).log(Level.INFO, "proxylistdownload fetching complete");
            return fetchedProxyListDownload();
        } catch (IOException ex) {
            Logger.getLogger(ProxyListDownloadFetcher.class.getName()).log(Level.SEVERE, "proxylistdownload fetching failed", ex);
            return null;
        }
    }

    public static String fetchProxyListDownloadHTTP() throws IOException {
        URL url = new URL("https://www.proxy-list.download/api/v1/get?type=http");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Logger.getLogger(ProxyListDownloadFetcher.class.getName()).log(Level.WARNING,
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

    public static String fetchProxyListDownloadSOCKS4() throws IOException {
        URL url = new URL("https://www.proxy-list.download/api/v1/get?type=socks4");
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

    private static String fetchedProxyListDownload() throws IOException {
        String httpProxies = fetchProxyListDownloadHTTP();
        String socks4Proxies = fetchProxyListDownloadSOCKS4();
        String allProxies = httpProxies + socks4Proxies;

        return allProxies;
    }
}
