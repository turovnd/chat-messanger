package main.java;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

public class Utils {

    private static CharsetDecoder decoder = Charset.forName( "UTF-8").newDecoder();

    /**
     * Read and decode buffer to string.
     *
     * @param size - size of buffer.
     * @param channel - from where read data to buffer.
     * @return {String}
     */
    public static String readToBufferAsString(int size, SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        int readVal = channel.read(buffer);
        if (readVal < 1) {
            return "Error";
        }
        StringBuilder sb = new StringBuilder();
        buffer.flip();
        sb.append(Utils.decoder.decode(buffer).toString());
        buffer.clear();
        return sb.toString().trim();
    }

    /**
     * Create a duplicate of buffer.
     *
     * @param original - ByteBuffer from that duplicate will make.
     * @return {ByteBuffer}
     */
    public static ByteBuffer cloneBuffer(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }

    /**
     * Set string to buffer.
     *
     * @param buffer - to where set message.
     * @param size - length of allowed message.
     * @param message - message to set to buffer.
     * @return {ByteBuffer}
     */
    public static ByteBuffer setStringToBuffer(ByteBuffer buffer, int size, String message) {
        int i = 0;
        while (i < size) {
            try {
                buffer.put(message.getBytes()[i]);
            } catch (ArrayIndexOutOfBoundsException e) {
                buffer.put(" ".getBytes());
            }
            i++;
        }
        return buffer;
    }

    /**
     * Save file from channel.
     *
     * @param filePath - path to where save file.
     * @param channel - from where retrieve file.
     * @param bufferSize - size of buffer.
     * @return {boolean}
     */
    public static boolean saveFileFromChannel (String filePath, SocketChannel channel, int bufferSize) {
        try {
            Path path = Paths.get(filePath);
            FileChannel fileChannel = FileChannel.open( path,
                    EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE) );
            ByteBuffer fileBuffer = ByteBuffer.allocate(bufferSize);

            int res;
            do {
                fileBuffer.clear();
                res = channel.read(fileBuffer);
                fileBuffer.flip();
                if (res > 0) {
                    fileChannel.write(fileBuffer);
                }
            } while (res >= 0);
            fileChannel.close();
            channel.close();
            return true;
        } catch (IOException e) {
            System.out.println("Error occur on saving file to " + filePath + ": " + e);
            return false;
        }
    }

    /**
     * Send file to channel.
     * @param filePath - path from where retrieve file.
     * @param channel - to where write file.
     * @param bufferSize - size of buffer.
     * @return {boolean}
     */
    public static boolean sendFileToChannel (String filePath, SocketChannel channel, int bufferSize) {
        try {
            FileChannel fileChannel = FileChannel.open(Paths.get(filePath));
            ByteBuffer fileBuffer = ByteBuffer.allocate(bufferSize);
            int bytesRead;
            do {
                bytesRead = fileChannel.read(fileBuffer);
                if (bytesRead <= 0) {
                    break;
                }
                fileBuffer.flip();
                do {
                    bytesRead -= channel.write(fileBuffer);
                } while (bytesRead > 0);
                fileBuffer.clear();
            } while (true);
            fileChannel.close();
            channel.close();
            return true;
        } catch (IOException e) {
            System.out.println("Error occur on downloading file from " + filePath + ": " + e);
            return false;
        }
    }
}
