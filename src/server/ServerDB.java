package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ServerDB {
    private String dataBase;

    ServerDB(String table, boolean clearDB) {
        this.dataBase = table;
        try {
            if (Files.exists(Paths.get(this.dataBase)) && clearDB) {
                Files.delete(Paths.get(this.dataBase));
            } else if (!Files.exists(Paths.get(this.dataBase))) {
                Files.createFile(Paths.get(this.dataBase));
            }
        } catch (IOException e) {
            System.out.println("Error occur on deleting file: " + this.dataBase + ". " + e);
        }
    }

    /**
     * Store message in DB.
     *
     * @param message - message from user.
     */
    public void addMessage(String message) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dataBase, true));
            writer.append(message).append("\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("Error on adding element to DB: " + e);
        }
    }

    /**
     * Get messages history from DB.
     *
     * @return List
     */
    public List<String> loadMessageHistory() {
        List<String> messages = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(dataBase));
            String line;
            while (true)  {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                messages.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error on adding element to DB: " + e);
        }
        return messages;
    }
}
