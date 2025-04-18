package Controllers;

import Services.ProxyService;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainController implements Initializable {

    @FXML
    TextArea outputArea;
    @FXML
    Label proxyCount;
    @FXML
    ComboBox comboSort;
    @FXML
    CheckBox checkboxHTTP;
    @FXML
    CheckBox checkboxSOCKS4;
    @FXML
    CheckBox checkboxSOCKS5;
    @FXML
    CheckBox checkboxShowProxyType;
    @FXML
    CheckBox checkboxProxyScrape;
    @FXML
    CheckBox checkboxGeoNode;
    @FXML
    CheckBox checkboxProxyListDownload;
    @FXML
    CheckBox checkboxOpenProxyList;
    @FXML
    Button btnCheckProxies;
    @FXML
    Label lblURL;
    @FXML
    Label lblThreads;
    @FXML
    Label lblTimeout;
    @FXML
    Label lblProxiesFound;
    @FXML
    Spinner<Integer> spinnerThreads;
    @FXML
    Spinner<Integer> spinnerTimeout;
    @FXML
    TextField checkURL;

    public List<String> proxyList;
    private static final ArrayList<String> filteredProxyList = new ArrayList<>();
    public static final Map<String, Boolean> sitesToScrape = new HashMap<>();
    private static final Map<String, Boolean> displayProxyList = new HashMap<>();
    private final int optimalThreads = Runtime.getRuntime().availableProcessors() * 10;
    static int proxyCountNum;
    private Timeline fetchingAnimation;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showProxyCheckElements(false);
        comboSort.getItems().addAll("None", "Type (Ascending)", "Type (Descending)");
        sitesToScrape.put("proxyscrape", checkboxProxyScrape.isSelected());
        sitesToScrape.put("geonode", checkboxGeoNode.isSelected());
        sitesToScrape.put("proxy-list", checkboxProxyListDownload.isSelected());
        sitesToScrape.put("openproxy-list", checkboxOpenProxyList.isSelected());
        displayProxyList.put("http", checkboxHTTP.isSelected());
        displayProxyList.put("socks4", checkboxSOCKS4.isSelected());
        displayProxyList.put("socks5", checkboxSOCKS5.isSelected());

        SpinnerValueFactory<Integer> timeoutLimits
                = new SpinnerValueFactory.IntegerSpinnerValueFactory(2000, 10000, 6000);

        SpinnerValueFactory<Integer> threadLimits
                = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 2000, optimalThreads);

        spinnerThreads.setValueFactory(threadLimits);
        spinnerTimeout.setValueFactory(timeoutLimits);

    }

    private void startFetchingAnimation() {
        fetchingAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, event -> outputArea.setText("Fetching")),
                new KeyFrame(Duration.seconds(0.3), event -> outputArea.setText("Fetching.")),
                new KeyFrame(Duration.seconds(0.6), event -> outputArea.setText("Fetching..")),
                new KeyFrame(Duration.seconds(0.9), event -> outputArea.setText("Fetching...")),
                new KeyFrame(Duration.seconds(1.2), event -> outputArea.setText("Fetching....")),
                new KeyFrame(Duration.seconds(1.5), event -> outputArea.setText("Fetching....."))
        );
        fetchingAnimation.setCycleCount(Timeline.INDEFINITE);
        fetchingAnimation.play();
    }

    @FXML
    private void btnStartActionPerformed() {
        Logger.getLogger(MainController.class.getName()).log(Level.INFO, "proxy fetching initiated");

        if (outputArea != null) {
            Platform.runLater(() -> startFetchingAnimation());
        }

        ProxyParsers.ProxyParserResults.proxyList.clear();
        filteredProxyList.clear();

        ProxyService fetchTask = new ProxyService();

        fetchTask.setOnSucceeded(event -> {
            if (ProxyService.isComplete) {
                showProxyCheckElements(true);
                proxyList = ProxyService.getProxyList();
                Platform.runLater(this::displayFilteredProxies);
            }
        });

        fetchTask.setOnFailed(event -> {
            Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, "proxy fetching failed", fetchTask.getException());
        });

        new Thread(fetchTask).start();

    }

    private void showProxyCheckElements(boolean toggle) {
        if (toggle) {
            checkboxHTTP.setDisable(false);
            checkboxSOCKS4.setDisable(false);
            checkboxSOCKS5.setDisable(false);
            checkboxShowProxyType.setDisable(false);
            comboSort.setDisable(false);
            checkURL.setDisable(false);
            btnCheckProxies.setDisable(false);
            lblProxiesFound.setDisable(false);
            lblURL.setDisable(false);
            lblThreads.setDisable(false);
            lblTimeout.setDisable(false);
            checkURL.setDisable(false);
            spinnerThreads.setDisable(false);
            spinnerThreads.setEditable(true);
            spinnerTimeout.setDisable(false);
            spinnerTimeout.setEditable(true);
        } else {
            checkboxHTTP.setDisable(true);
            checkboxSOCKS4.setDisable(true);
            checkboxSOCKS5.setDisable(true);
            checkboxShowProxyType.setDisable(true);
            comboSort.setDisable(true);
            checkURL.setDisable(true);
            btnCheckProxies.setDisable(true);
            lblProxiesFound.setDisable(true);
            lblURL.setDisable(true);
            lblThreads.setDisable(true);
            lblTimeout.setDisable(true);
            spinnerThreads.setDisable(true);
            spinnerTimeout.setDisable(true);

        }

    }

    @FXML
    public void displayFilteredProxies() {
        if (fetchingAnimation != null) {
            fetchingAnimation.stop();
        }
        proxyCountNum = 0;
        ArrayList<String> tempFilteredList = new ArrayList<>();

        for (String proxyInfo : proxyList) {
            for (Map.Entry<String, Boolean> entry : displayProxyList.entrySet()) {
                if (entry.getValue()) {
                    if (proxyInfo.contains(entry.getKey())) {
                        proxyCountNum++;
                        tempFilteredList.add(proxyInfo);
                        break;
                    }
                }
            }
        }

        filteredProxyList.clear();
        filteredProxyList.addAll(tempFilteredList);

        Platform.runLater(() -> {
            StringBuilder textBuilder = new StringBuilder();
            for (String proxyInfo : tempFilteredList) {
                textBuilder.append(proxyInfo).append("\n");
            }
            outputArea.setText(textBuilder.toString());
            proxyCount.setText(Integer.toString(proxyCountNum));
        });

    }

    @FXML
    private void btnCheckProxiesActionPerformed() {
        if (!CheckProxiesController.IN_PROGRESS) {
            if (outputArea.getText().isEmpty()) {
                showValidationError("No proxies found");
            }

            String url = checkURL.getText();
            int threads = (int) spinnerThreads.getValue();

            int timeout = (int) spinnerTimeout.getValue();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CheckProxies.fxml"));
                Parent root = loader.load();

                CheckProxiesController controller = loader.getController();
                String proxies = outputArea.getText();
                controller.startProxyCheck(url, threads, timeout, proxies);

                Stage stage = new Stage();
                Scene scene = new Scene(root);
                stage.setScene(scene);

                stage.initOwner(outputArea.getScene().getWindow());
                stage.initModality(Modality.WINDOW_MODAL);
                stage.setResizable(false);
                stage.show();

                CheckProxiesController.IN_PROGRESS = true;

                stage.setOnCloseRequest(event -> {
                    CheckProxiesController.IN_PROGRESS = false;
                    controller.stopProxyCheck();
                    Logger.getLogger(MainController.class.getName()).log(Level.INFO, "requested cancellation of proxy checking");
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void sortDisplayedProxies() {
        String selectedValue = comboSort.getValue().toString();

        switch (selectedValue) {
            case "None":
                break;

            case "Type (Ascending)":
                Collections.sort(filteredProxyList);
                Platform.runLater(() -> {
                    outputArea.setText("");
                    StringBuilder textBuilder = new StringBuilder();
                    for (String proxy : filteredProxyList) {
                        textBuilder.append(proxy).append("\n");
                    }
                    outputArea.setText(textBuilder.toString());
                });
                break;

            case "Type (Descending)":
                Collections.sort(filteredProxyList);
                Collections.reverse(filteredProxyList);
                Platform.runLater(() -> {
                    outputArea.setText("");
                    StringBuilder textBuilder = new StringBuilder();
                    for (String proxy : filteredProxyList) {
                        textBuilder.append(proxy).append("\n");
                    }
                    outputArea.setText(textBuilder.toString());
                });
                break;
        }
    }

    @FXML
    private void showProxyTypeActionPerformed() {
        StringBuilder updatedText = new StringBuilder();

        if (!checkboxShowProxyType.isSelected()) {
            for (String proxy : filteredProxyList) {
                String[] parts = proxy.split("://");
                if (parts.length > 1) {
                    updatedText.append(parts[1]).append("\n");
                }
            }
        } else {
            for (String proxy : filteredProxyList) {
                updatedText.append(proxy).append("\n");
            }
        }
        Platform.runLater(() -> outputArea.setText(updatedText.toString()));
    }

    @FXML
    private void checkboxSitesActionPerformed() {
        sitesToScrape.replace("proxyscrape", checkboxProxyScrape.isSelected());
        sitesToScrape.replace("geonode", checkboxGeoNode.isSelected());
        sitesToScrape.replace("proxy-list", checkboxProxyListDownload.isSelected());
        sitesToScrape.replace("openproxy-list", checkboxOpenProxyList.isSelected());
    }

    @FXML
    private void checkboxProxiesActionPerformed() {
        displayProxyList.replace("http", checkboxHTTP.isSelected());
        displayProxyList.replace("socks4", checkboxSOCKS4.isSelected());
        displayProxyList.replace("socks5", checkboxSOCKS5.isSelected());
        displayFilteredProxies();
    }

}
