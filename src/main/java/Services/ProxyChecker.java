package Services;

import Controllers.CheckProxiesController;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;

public class ProxyChecker extends Task<Void> {

    private static final Logger LOGGER = Logger.getLogger(ProxyChecker.class.getName());

    private final List<String> proxies;
    private final List<String> successfulProxies = Collections.synchronizedList(new ArrayList<>());
    private final String urlString;
    private final int timeout;
    private final CheckProxiesController controller;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final int threads;
    private ExecutorService executor;
    private final Map<InetSocketAddress, HttpClient> httpClientCache = new ConcurrentHashMap<>();

    public ProxyChecker(CheckProxiesController controller, List<String> proxies,
            String urlString, int threads, int timeout) {
        this.controller = controller;
        this.proxies = proxies;
        this.urlString = urlString;
        this.threads = threads;
        this.timeout = timeout;
    }

    @Override
    protected Void call() {
        executor = Executors.newFixedThreadPool(threads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String proxy : proxies) {
            if (isCancelled() || cancelRequested.get()) {
                break;
            }
            futures.add(processProxy(proxy));
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            all.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error waiting for proxy checks", e);
            Thread.currentThread().interrupt();
        }

        if (!isCancelled() && !cancelRequested.get()) {
            Platform.runLater(() -> {
                LOGGER.log(Level.INFO, "Proxy Check Complete: Success: {0}, Failed: {1}",
                        new Object[]{successfulProxies.size(), proxies.size() - successfulProxies.size()});
                controller.showAlert("Proxy Check Complete", "Results:",
                        "Success: " + successfulProxies.size() + "\nFailed: " + (proxies.size() - successfulProxies.size()));
                controller.showElements();
            });
        }

        executor.shutdownNow();
        LOGGER.log(Level.INFO, "Proxy Check complete");
        return null;
    }

    private CompletableFuture<Void> processProxy(String proxy) {
        if (isCancelled() || cancelRequested.get()) {
            return CompletableFuture.completedFuture(null);
        }

        String[] parts = proxy.split("://");
        String proxyType = parts.length > 1 ? parts[0].toLowerCase() : "http";
        String proxyAddress = parts.length > 1 ? parts[1] : parts[0];
        String[] proxyParts = proxyAddress.split(":");
        if (proxyParts.length != 2) {
            LOGGER.log(Level.INFO, "Skipping invalid proxy format: {0}", proxy);
            return CompletableFuture.completedFuture(null);
        }

        String host = proxyParts[0];
        int port;
        try {
            port = Integer.parseInt(proxyParts[1]);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.INFO, "Invalid port for proxy: {0}", proxy);
            return CompletableFuture.completedFuture(null);
        }

        switch (proxyType) {
            case "http":
                return processHttpProxy(proxy, host, port);
            case "socks4":
                return CompletableFuture.runAsync(() -> {
                    try {
                        testSocks4Proxy(host, port, proxy);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "SOCKS4 proxy test failed for: " + proxy, e);
                    }
                }, executor);
            case "socks5":
                return CompletableFuture.runAsync(() -> {
                    try {
                        testSocks5Proxy(host, port, proxy);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "SOCKS5 proxy test failed for: " + proxy, e);
                    }
                }, executor);
            default:
                LOGGER.log(Level.INFO, "Unsupported proxy type: {0}", proxy);
                return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Void> processHttpProxy(String proxy, String host, int port) {
        InetSocketAddress addr = new InetSocketAddress(host, port);
        HttpClient client = httpClientCache.computeIfAbsent(addr, a
                -> HttpClient.newBuilder()
                        .executor(executor)
                        .proxy(ProxySelector.of(a))
                        .connectTimeout(Duration.ofMillis(timeout))
                        .build()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .timeout(Duration.ofMillis(timeout))
                .build();

        return client.sendAsync(request, BodyHandlers.discarding())
                .thenAccept(response -> {
                    int code = response.statusCode();
                    LOGGER.log(Level.INFO, "HTTP proxy {0} returned {1}", new Object[]{proxy, code});
                    if (code >= 200 && code < 400) {
                        successfulProxies.add(proxy);
                    }
                    controller.checkedProxies.incrementAndGet();
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "HTTP proxy " + proxy + " error: ", ex);
                    controller.checkedProxies.incrementAndGet();
                    return null;
                });
    }

    private void testSocks4Proxy(String host, int port, String proxy) throws IOException {
        if (isCancelled() || cancelRequested.get()) {
            return;
        }
        System.setProperty("socksProxyVersion", "4");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            successfulProxies.add(proxy);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "SOCKS4 proxy " + proxy + " failed: ", e);
        } finally {
            controller.checkedProxies.incrementAndGet();
            System.setProperty("socksProxyVersion", "5");
        }
    }

    private void testSocks5Proxy(String host, int port, String proxy) throws IOException {
        if (isCancelled() || cancelRequested.get()) {
            return;
        }
        try (Socket socket = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port)))) {
            socket.connect(new InetSocketAddress("www.example.com", 80), timeout);
            successfulProxies.add(proxy);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "SOCKS5 proxy " + proxy + " failed: ", e);
        } finally {
            controller.checkedProxies.incrementAndGet();
        }
    }

    public List<String> getSuccessfulProxies() {
        return new ArrayList<>(successfulProxies);
    }

    public void cancelTask() {
        this.cancel();
        cancelRequested.set(true);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}
