package server;

import utils.Utils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ServerFiles {

    private static final int BUFFER_SIZE = 2048;

    /**
     * Retrieve file from client socket channel.
     *
     * @param channel - from where read data
     * @param filePath - path to file to where save file.
     * @return Boolean
     */
    public static boolean retrieveFromChannelFile (SocketChannel channel, String filePath) {
        return Utils.saveFileFromChannel(filePath, channel, BUFFER_SIZE);
    }

    /**
     * Send file to channel.
     *
     * @param channel - channel to where send file.
     * @param filePath - path to file from where retrieve file.
     * @return Boolean
     */
    public static boolean sendToChannelFile (SocketChannel channel, String filePath) {
        return Utils.sendFileToChannel(filePath, channel, BUFFER_SIZE);
    }

    /**
     * Get files from stored directory.
     *
     * @param dirPath - path to directory with files.
     * @return List
     */
    public static List<String> getFiles(String dirPath) {
        List<String> result = new ArrayList<>();
        try {
            Path path = Paths.get(dirPath);
            Files.list(path).forEach(p -> result.add(p.getFileName().toString()));
        } catch (IOException e) {
            System.out.println("Error on retrieving files in folder: " + dirPath);
        }
        return result;
    }
}
