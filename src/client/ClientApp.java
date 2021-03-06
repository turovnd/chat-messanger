package client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ClientApp extends Application {

    private static final String[] availableParams = {"port", "dirPath", "maxFileSize"};
    private static int maxFileSize;
    private static int port;
    private static String dirPath;

    private TextArea messageArea = new TextArea();
    private Stage primaryStage = null;
    private ClientNetwork client = null;

    public static void main(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (String a : args) {
            for (String b : availableParams) {
                if (a.split("=")[0].equals(b)) {
                    params.put(a.split("=")[0], a.split("=")[1]);
                }
            }
        }
        maxFileSize = 1000 * 1000 * (params.get("maxFileSize") != null ? Integer.parseInt(params.get("maxFileSize")) : 100);
        dirPath = params.get("dirPath") != null ? params.get("dirPath") : "/tmp";
        port = params.get("port") != null ? Integer.parseInt(params.get("port")) : 9093;
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setScene(new Scene(createEnterContent()));
        primaryStage.setTitle("Client application");
        primaryStage.show();
        client = new ClientNetwork( messageArea,"localhost", port, dirPath);
    }

    /**
     * Create entrance content form.
     *
     * @return Parent
     */
    private Parent createEnterContent() {
        HBox root = new HBox();
        root.setPrefHeight(50);
        root.setSpacing(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("Enter your name");
        TextField field = new TextField();
        field.setOnAction(e -> {
            if (!field.getText().equals("")) {
                client.setName(field.getText());
                client.start();
                primaryStage.setScene(new Scene(createChatContent()));
                primaryStage.setTitle("Client application [" + field.getText() + "]");
                primaryStage.show();
            }
        });
        root.getChildren().addAll(label, field);
        return root;
    }

    /**
     * Create chat content form.
     *
     * @return Parent
     */
    private Parent createChatContent() {
        messageArea.clear();
        messageArea.setPrefHeight(300);
        messageArea.setEditable(false);
        messageArea.setFocusTraversable(false);

        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        Label label = new Label("Enter your message");
        TextField field = new TextField();
        field.requestFocus();
        field.setOnAction(e -> {
            if (!field.getText().equals("")) {
                client.sendMessage(field.getText());
                field.clear();
            }
        });

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");

        Button sendFileBtn = new Button("Send file");
        sendFileBtn.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                if (file.isDirectory()) {
                    messageArea.appendText("Error: file is directory.\n");
                } else if (file.length() >= maxFileSize) {
                    messageArea.appendText("Error: file size limit.\n");
                } else {
                    client.sendFile(file.getPath());
                }
            }
        });
        root.getChildren().addAll(messageArea, label, field, sendFileBtn);
        return root;
    }
}
