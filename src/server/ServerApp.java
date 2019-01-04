package server;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class ServerApp extends Application {

    private static final String[] availableParams = {"dbName", "dbClear", "dirPath", "port"};
    private static boolean dbClear;
    private static String dbName;
    private static String dirPath;
    private static int port;

    private TextArea messageArea = new TextArea();
    private ServerNetwork server = null;

    public static void main(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (String a : args) {
            for (String b : availableParams) {
                if (a.split("=")[0].equals(b)) {
                    params.put(a.split("=")[0], a.split("=")[1]);
                }
            }
        }
        dbClear = params.get("dbClear") != null ? params.get("dbClear").equals("true") : Boolean.parseBoolean("true");
        dbName = params.get("dbName") != null ? params.get("dbName") : "database";
        dirPath = params.get("dirPath") != null ? params.get("dirPath") : "/tmp";
        port = params.get("port") != null ? Integer.parseInt(params.get("port")) : 9093;
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(createContent()));
        stage.setTitle("Server application");
        stage.show();
        server = new ServerNetwork(messageArea, "localhost", port, dbClear, dbName, dirPath);
        server.start();
    }

    /**
     * Create server content form.
     *
     * @return Parent
     */
    private Parent createContent() {
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
            String message = field.getText();
            if (!message.equals("")) {
                switch (message) {
                    case "/exit":
                    case "/q":
                        server.disconnect("Console exit command.");
                        break;
                    case "/history":
                    case "/h":
                        messageArea.appendText(server.getMessageHistory());
                        break;
                    case "/files":
                        messageArea.appendText(server.getFiles());
                        break;
                    case "/info":
                        messageArea.appendText(server.getInfoStatus());
                        break;
                    default:
                        messageArea.appendText("[System]: " + message + "\n");
                        server.sendBroadcastMessage(null, "System", message);
                        break;
                }
                field.clear();
            }
        });
        root.getChildren().addAll(messageArea, label, field);
        return root;
    }
}
