package main.java.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class FileUtils {

    private static final int BUFFER_SIZE = 1024;
    public static final String DIR_PATH = "/Users/nturov/www/chat-messanger/tmp";

    public static void sendFile(String path, InetSocketAddress address) {
        try {
            Selector selector = Selector.open();
            SocketChannel socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(address);
            socket.register(selector, SelectionKey.OP_CONNECT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    SocketChannel channel = (SocketChannel) key.channel();

                    if (key.isConnectable()) {
                        if (channel.isConnectionPending()) {
                            channel.finishConnect();
                        }
                        channel.register(selector, SelectionKey.OP_WRITE);
                        continue;
                    }

                    if (key.isWritable()) {
                        FileUtils.handleSendFile(channel, path);
                        channel.close();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error on open socket for sending file.");
        }
    }

    public static void handleSendFile(SocketChannel channel, String path) throws IOException {
        FileChannel fileChannel = FileChannel.open(Paths.get(path));
        ByteBuffer fileBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead = 0;

        // Send action
        channel.write(ByteBuffer.wrap("f".getBytes()));

        // Send file name
        String fName = Paths.get(path).getFileName().toString();
        while (fName.length() < 16) {
            fName = "0" + fName;
        }
        if (fName.length() > 16) {
            fName = fName.substring(fName.length() - 16, fName.length());
        }
        channel.write(ByteBuffer.wrap(fName.getBytes()));

        // Send file
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
    }

    public static void saveFile(SocketChannel channel, String fileName) {
        try {
            Path path = Paths.get(DIR_PATH + "/" + fileName);
            FileChannel fileChannel = FileChannel.open( path,
                    EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE) );

            ByteBuffer fileBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            int res = 0;
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
        } catch (IOException e) {
            System.out.println("Error on open socket for sending file.");
        }
    }

    public static void downloadFile(String name, InetSocketAddress address) {
        try {
            Selector selector = Selector.open();
            SocketChannel socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(address);
            socket.register(selector, SelectionKey.OP_CONNECT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    SocketChannel channel = (SocketChannel) key.channel();

                    if (key.isConnectable()) {
                        if (channel.isConnectionPending()) {
                            channel.finishConnect();
                        }
                        channel.register(selector, SelectionKey.OP_READ);
                        channel.write(ByteBuffer.wrap("d".getBytes()));
                        channel.write(ByteBuffer.wrap(name.getBytes()));
                        continue;
                    }

                    if (key.isReadable()) {
                        FileUtils.handleDownloadFile(channel, name);
                        channel.close();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error on open socket for sending file.");
        }
    }

    private static void handleDownloadFile(SocketChannel channel, String name) throws IOException {
        System.out.println("TUT");
        channel.close();
    }

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
