package Controllers;

import Services.ProxyChecker;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.stage.Window;

public class CheckProxiesController implements Initializable {

    @FXML
    private Button btnSaveFile;
    @FXML
    private Button btnCopyClipboard;
    @FXML
    private CheckBox showProxyType;
    @FXML
    private ComboBox<String> comboboxProxyType;
    @FXML
    private TextFlow checkedProxyOutputArea;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblCheckedNum;
    @FXML
    private Label lblTotalProxies;
    @FXML
    private Label lblPercentage;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label elapsedTime;
    @FXML
    ScrollPane scrollPane;

    private int totalProxies;
    public volatile AtomicInteger checkedProxies = new AtomicInteger(0);
    public static boolean IN_PROGRESS = false;
    private Services.ProxyChecker proxyChecker;
    private Timeline progressAnimation;
    private Timeline timer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnCopyClipboard.setVisible(false);
        btnSaveFile.setVisible(false);
        showProxyType.setVisible(false);
        comboboxProxyType.setVisible(false);
        lblStatus.setText("Waiting to start...");
        comboboxProxyType.getItems().addAll("All", "Http", "Socks4", "Socks5");
        comboboxProxyType.setValue("All");
    }

    public void startProxyCheck(String urlString, int threads, int timeout, String proxies) {

        ArrayList<String> checkList = new ArrayList<>(Arrays.asList(proxies.split("\n")));
        proxyChecker = new ProxyChecker(this, checkList, urlString, threads, timeout);

        totalProxies = checkList.size();

        new Thread(proxyChecker).start();
        startUpdatingAnimation();
        startTimer();
    }

    public void stopProxyCheck() {
        if (proxyChecker != null) {
            proxyChecker.cancelTask();
        }
    }

    public void showElements() {
        Platform.runLater(() -> {
            lblStatus.setText("Done.");
            updateCheckedProxyDisplay();
            btnCopyClipboard.setVisible(true);
            btnSaveFile.setVisible(true);
            showProxyType.setVisible(true);
            comboboxProxyType.setVisible(true);
        });
    }

    public void appendToOutputArea(String text, boolean isSuccess) {
        Text textNode = new Text(text + "\n");
        textNode.setFill(isSuccess ? Color.GREEN : Color.RED);
        checkedProxyOutputArea.getChildren().add(textNode);
        scrollPane.setVvalue(1.0);
    }

    private void startTimer() {
        long startTime = System.currentTimeMillis();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            long elapsedTimeMillis = System.currentTimeMillis() - startTime;
            long seconds = (elapsedTimeMillis / 1000) % 60;
            long minutes = (elapsedTimeMillis / (1000 * 60)) % 60;
            long hours = (elapsedTimeMillis / (1000 * 60 * 60)) % 24;
            elapsedTime.setText(String.format("%dh:%dm:%ds", hours, minutes, seconds));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    public void startUpdatingAnimation() {
        progressAnimation = new Timeline(new KeyFrame(Duration.seconds(.2), event -> {
            Platform.runLater(() -> {
                int checkedProxiesInteger = checkedProxies.get();

                int progress = totalProxies > 0 ? (int) (((double) checkedProxiesInteger / totalProxies) * 100) : 0;
                progressBar.setProgress(progress / 100.0);
                lblCheckedNum.setText(Integer.toString(checkedProxiesInteger));
                lblTotalProxies.setText(Integer.toString(totalProxies));
                lblPercentage.setText(progress + "%");

                if (progress >= 100) {
                    progressAnimation.stop();
                    timer.stop();
                    lblStatus.setText("Completed");
                    showElements();
                }
            });
        }));

        progressAnimation.setCycleCount(Timeline.INDEFINITE);
        progressAnimation.play();
    }

    public void updateCheckedProxyDisplay() {
        checkedProxyOutputArea.getChildren().clear();
        boolean showType = showProxyType.isSelected();
        String selectedType = comboboxProxyType.getValue();

        for (String proxy : proxyChecker.getSuccessfulProxies()) {
            if (selectedType.equals("All")) {
                if (showType) {
                    appendToOutputArea(proxy, true);
                } else {
                    String[] parts = proxy.split("://");
                    String formattedProxy = (parts.length > 1) ? parts[1] : proxy;
                    appendToOutputArea(formattedProxy, true);
                }
            } else if (!selectedType.equals("All") && proxy.toLowerCase().contains(selectedType.toLowerCase())) {
                if (showType) {
                    appendToOutputArea(proxy, true);
                } else {
                    String[] parts = proxy.split("://");
                    String formattedProxy = (parts.length > 1) ? parts[1] : proxy;
                    appendToOutputArea(formattedProxy, true);
                }

            }

        }
    }

    @FXML
    private void copyToClipboard() {
        String text = getProxyOutput();

        StringSelection stringSelection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    @FXML
    private void saveToFile() {
        String text = getProxyOutput();

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Directory");

        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt"));

        Window window = checkedProxyOutputArea.getScene().getWindow();
        File selectedDirectory = fileChooser.showSaveDialog(window);

        if (selectedDirectory != null) {
            String path = selectedDirectory.toString();
            try (FileWriter writer = new FileWriter(path)) {
                writer.write(text.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void showAlert(String title, String header, String text) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(text);
            alert.showAndWait();
        });
    }

    public String getProxyOutput() {
        StringBuilder output = new StringBuilder();
        checkedProxyOutputArea.getChildren().forEach(node -> {
            if (node instanceof Text) {
                output.append(((Text) node).getText());
            }
        });
        return output.toString();
    }

    public String getProtocolType() {
        return comboboxProxyType.getSelectionModel().getSelectedItem();
    }
}
