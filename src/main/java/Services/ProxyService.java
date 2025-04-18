package Services;

import Controllers.MainController;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javafx.concurrent.Task;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyService extends Task<Void> {

    static List<String> proxyList;
    public static boolean isComplete = false;

    @Override
    protected Void call() throws Exception {
        try {
            fetchProxies();
        } catch (IOException ex) {
            Logger.getLogger(ProxyService.class.getName()).log(Level.SEVERE, "failed to fetch proxies", ex);
        }
        return null;
    }

    public static void fetchProxies() throws IOException {
        int taskCount = 0;
        for (Map.Entry<String, Boolean> site : MainController.sitesToScrape.entrySet()) {
            if (site.getValue() == true) {
                taskCount++;
            }
        }
        CountDownLatch latch = new CountDownLatch(taskCount);
        final String[] proxyScrapeProxies = new String[1];
        final String[] geonodeProxies = new String[1];
        final String[] proxyListProxies = new String[1];
        final String[] openProxyListProxies = new String[1];

        for (Map.Entry<String, Boolean> site : MainController.sitesToScrape.entrySet()) {
            String key = site.getKey();
            boolean value = site.getValue();

            if (value == true) {
                switch (key) {
                    case "proxyscrape" -> {
                        ProxyFetchers.ProxyScrapeFetcher fetcher1 = new ProxyFetchers.ProxyScrapeFetcher();
                        fetcher1.setOnSucceeded(event -> {
                            proxyScrapeProxies[0] = fetcher1.getValue();
                            latch.countDown();
                        });
                        fetcher1.setOnFailed(event -> latch.countDown());
                        new Thread(fetcher1).start();
                    }
                    case "geonode" -> {
                        ProxyFetchers.GeonodeFetcher fetcher2 = new ProxyFetchers.GeonodeFetcher();
                        fetcher2.setOnSucceeded(event -> {
                            geonodeProxies[0] = fetcher2.getValue();
                            latch.countDown();
                        });
                        fetcher2.setOnFailed(event -> latch.countDown());
                        new Thread(fetcher2).start();
                    }
                    case "proxy-list" -> {
                        ProxyFetchers.ProxyListDownloadFetcher fetcher3 = new ProxyFetchers.ProxyListDownloadFetcher();
                        fetcher3.setOnSucceeded(event -> {
                            proxyListProxies[0] = fetcher3.getValue();
                            latch.countDown();
                        });
                        fetcher3.setOnFailed(event -> latch.countDown());
                        new Thread(fetcher3).start();
                    }
                    case "openproxy-list" -> {
                        ProxyFetchers.OpenProxyListFetcher fetcher4 = new ProxyFetchers.OpenProxyListFetcher();
                        fetcher4.setOnSucceeded(event -> {
                            openProxyListProxies[0] = fetcher4.getValue();
                            latch.countDown();
                        });
                        fetcher4.setOnFailed(event -> latch.countDown());
                        new Thread(fetcher4).start();
                    }
                }
            }
        }

        try {
            latch.await();
            Logger.getLogger(ProxyService.class.getName()).log(Level.INFO, "Proxy fetching complete");
            parseProxies(proxyScrapeProxies[0], geonodeProxies[0], proxyListProxies[0], openProxyListProxies[0]);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void parseProxies(String proxyscrape, String geonode, String proxyList, String openProxyList) {
        Logger.getLogger(ProxyService.class.getName()).log(Level.INFO, "Proxy parsing initated");

        int taskCount = 0;
        if (proxyscrape != null && !proxyscrape.isEmpty()) {
            taskCount++;
        }
        if (geonode != null && !geonode.isEmpty()) {
            taskCount++;
        }
        if (proxyList != null && !proxyList.isEmpty()) {
            taskCount++;
        }
        if (openProxyList != null && !openProxyList.isEmpty()) {
            taskCount++;
        }

        CountDownLatch latch = new CountDownLatch(taskCount);

        if (proxyscrape != null && !proxyscrape.isEmpty()) {
            ProxyParsers.ProxyScrapeParser scraperParser = new ProxyParsers.ProxyScrapeParser(proxyscrape);
            scraperParser.setOnSucceeded(event -> latch.countDown());
            scraperParser.setOnFailed(event -> latch.countDown());
            new Thread(scraperParser).start();
        }

        if (geonode != null && !geonode.isEmpty()) {
            ProxyParsers.GeonodeParser geonodeParser = new ProxyParsers.GeonodeParser(geonode);
            geonodeParser.setOnSucceeded(event -> latch.countDown());
            geonodeParser.setOnFailed(event -> latch.countDown());
            new Thread(geonodeParser).start();
        }

        if (proxyList != null && !proxyList.isEmpty()) {
            ProxyParsers.ProxyListDownloadParser proxyListParser = new ProxyParsers.ProxyListDownloadParser(proxyList);
            proxyListParser.setOnSucceeded(event -> latch.countDown());
            proxyListParser.setOnFailed(event -> latch.countDown());
            new Thread(proxyListParser).start();
        }

        if (openProxyList != null && !openProxyList.isEmpty()) {
            ProxyParsers.OpenProxyListParser openProxyListParser = new ProxyParsers.OpenProxyListParser(openProxyList);
            openProxyListParser.setOnSucceeded(event -> latch.countDown());
            openProxyListParser.setOnFailed(event -> latch.countDown());
            new Thread(openProxyListParser).start();
        }

        try {
            latch.await();
            setProxyList();
            isComplete = true;
            Logger.getLogger(ProxyService.class.getName()).log(Level.INFO, "proxy parsing complete");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            isComplete = false;
            Logger.getLogger(ProxyService.class.getName()).log(Level.INFO, "roxy parsing failed", e);
        }
    }

    private static void setProxyList() {
        proxyList = ProxyParsers.ProxyParserResults.proxyList;
    }

    public static List<String> getProxyList() {
        return proxyList;
    }
}
