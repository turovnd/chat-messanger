package main.java.server;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class ServerApp extends Application {

    private TextArea messageArea = new TextArea();
    private ServerNetwork server = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(createContent()));
        stage.setTitle("Server application");
        stage.show();
        server = new ServerNetwork(messageArea, "localhost", 9093);
        server.start();
    }

    /**
     * Create Content Frame.
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
