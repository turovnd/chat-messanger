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
     * @return boolean
     */
    public static boolean retrieveFromChannelFile (SocketChannel channel, String fileName) {
        return Utils.saveFileFromChannel(DIR_PATH + "/" + fileName, channel, BUFFER_SIZE);
    }

    public static boolean sendToChannelFile (SocketChannel channel, String fileName) {
        return Utils.sendFileToChannel(DIR_PATH + "/" + fileName, channel, BUFFER_SIZE);
    }

    /**
     * Get files list in DIR_PATH
     *
     * @return List<String>
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
