package main.java.server;

import main.java.Utils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ServerFiles {

    private static final int BUFFER_SIZE = 2048;
    private static final String DIR_PATH = "/Users/nturov/www/chat-messenger/tmp";

    /**
     * Retrieve file from client socket channel.
     *
     * @param channel - from where read data
     * @param fileName - name of file
     * @return Boolean
     */
    public static boolean retrieveFromChannelFile (SocketChannel channel, String fileName) {
        return Utils.saveFileFromChannel(DIR_PATH + "/" + fileName, channel, BUFFER_SIZE);
    }

    /**
     * Send file to channel.
     *
     * @param channel - channel to where send file.
     * @param fileName - name of file that is going to send.
     * @return Boolean
     */
    public static boolean sendToChannelFile (SocketChannel channel, String fileName) {
        return Utils.sendFileToChannel(DIR_PATH + "/" + fileName, channel, BUFFER_SIZE);
    }

    /**
     * Get files from stored directory.
     *
     * @return List
     */
    public static List<String> getFiles() {
        List<String> result = new ArrayList<>();
        try {
            Path path = Paths.get(DIR_PATH);
            Files.list(path).forEach(p -> {
                result.add(p.getFileName().toString());
            });
        } catch (IOException e) {
            System.out.println("Error on retrieving files name from `DIR_PATH`.");
        }
        return result;
    }
}
