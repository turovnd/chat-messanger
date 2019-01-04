package client;

import utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.util.Iterator;

class ClientFiles {

    private static final int BUFFER_SIZE = 2048;
    private static final int BUFFER_NAME = 32;
    private static final String DIR_PATH = "/Users/nturov/www/chat-messenger/tmp-client";

    /**
     * Send file from client to server.
     *
     * @param path - path on local PC.
     * @param user - sender's name.
     * @param address - address to where send file.
     */
    public static void sendFile (String path, String user, InetSocketAddress address) {
        try {
            Selector selector = Selector.open();

            SocketChannel socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(address);
            socket.register(selector, SelectionKey.OP_CONNECT);

            boolean isSending = true;

            while (isSending) {
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
                        isSending = false;
                        ByteBuffer fileBuffer = ByteBuffer.allocate(BUFFER_SIZE);

                        // Set action => 1 byte
                        fileBuffer.put("f".getBytes());

                        // Set user name => 32 bytes
                        fileBuffer = Utils.setStringToBuffer(fileBuffer, BUFFER_NAME, user);

                        // Set file name => 32 bytes
                        fileBuffer = Utils.setStringToBuffer(fileBuffer, BUFFER_NAME, Paths.get(path).getFileName().toString());

                        // Send metadata
                        fileBuffer.flip();
                        channel.write(fileBuffer);
                        fileBuffer.clear();

                        // Send file
                        Utils.sendFileToChannel(path, channel, BUFFER_SIZE);
                        selector.close();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error on open socket for sending file." + e);
        }
    }

    /**
     * Download file from the server.
     *
     * @param dirPath - path to directory to where save file.
     * @param fileName - name of file for download.
     * @param address - address from where download file.
     */
    public static void downloadFile (String dirPath, String fileName, String user, InetSocketAddress address) {
        try {
            Selector selector = Selector.open();

            SocketChannel socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(address);
            socket.register(selector, SelectionKey.OP_CONNECT);

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            boolean isDownloading = true;

            while (isDownloading) {
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
                        buffer.clear();
                        buffer = Utils.setStringToBuffer(buffer, 1, "d");
                        buffer = Utils.setStringToBuffer(buffer, BUFFER_NAME, user);
                        buffer = Utils.setStringToBuffer(buffer, BUFFER_NAME, fileName);
                        buffer.flip();
                        channel.write(buffer);
                        channel.register(selector, SelectionKey.OP_READ);
                        continue;
                    }

                    if (key.isReadable()) {
                        isDownloading = false;
                        Utils.saveFileFromChannel(dirPath + "/" + fileName, channel, BUFFER_SIZE);
                        selector.close();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error on open socket for downloading file." + e);
        }
    }
}
