package main.java.client;

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

public class ClientApp extends Application {

    private final static int PORT = 9093;
    private final static int MAX_FILE_SIZE = 100 * 1000 * 1000;
    private TextArea messageArea = new TextArea();
    private Stage primaryStage = null;
    private ClientNetwork client = null;

    /**
     * Create Chat Content Form.
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

        Button sendFile = new Button("Send file");
        sendFile.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                if (file.length() >= MAX_FILE_SIZE) {
                    messageArea.appendText("Error: file size limit.\n");
                } else {
                    sendFile(file);
                }
            }
        });

        root.getChildren().addAll(messageArea, label, field, sendFile);
        return root;
    }

    /**
     * Create Enter Content Form.
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
                primaryStage.setTitle("Client application [" + field.getText() + "]");
                primaryStage.setScene(new Scene(createChatContent()));
                primaryStage.show();

            }
        });
        root.getChildren().addAll(label, field);
        return root;
    }

    /**
     * Send file to other clients.
     *
     * @param file {File}
     */
    private void sendFile(File file) {
        client.sendFile(file.getPath());
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setScene(new Scene(createEnterContent()));
        primaryStage.setTitle("Client application");
        primaryStage.show();
        client = new ClientNetwork( "localhost", PORT, messageArea);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
