package client;

import javafx.scene.control.TextArea;
import utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

class ClientNetwork extends Thread {

    private final static int BUFFER_TYPE = 1;
    private static final int BUFFER_SIZE = 1024;
    private final static int BUFFER_NAME = 32;

    private ByteBuffer typeBuffer;
    private ByteBuffer writeBuffer;

    private InetSocketAddress address;
    private SocketChannel socketRead;

    private TextArea messageArea;
    private boolean isRunning;

    private String DIR_PATH;

    ClientNetwork(TextArea area, String address, int port, String dirPath) {
        this.messageArea = area;
        this.DIR_PATH = dirPath;
        this.address = new InetSocketAddress(address, port);
        this.typeBuffer = ByteBuffer.allocate(BUFFER_TYPE);
        this.writeBuffer = ByteBuffer.allocate(BUFFER_TYPE + BUFFER_NAME + BUFFER_SIZE);
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();

            socketRead = SocketChannel.open(address);
            socketRead.configureBlocking(false);
            socketRead.register(selector, SelectionKey.OP_READ);

            sendMessage("/login");
            System.out.println("Client connected");

            isRunning = true;
            while (isRunning) {

                int readyCount = selector.select();
                if (readyCount == 0) {
                    continue;
                }

                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) {
                        readMessages(key);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error occur while connecting to server: " + e);
            System.exit(1);
        }
    }

    /**
     * Read incoming messages.
     *
     * @param key - readable selector key.
     */
    private void readMessages (SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();

            typeBuffer.clear();

            int nBytes = channel.read(typeBuffer);

            if (nBytes == -1) {
                disconnectChannel("Server was disconnected.");
                key.cancel();
                return;
            }

            switch (new String(typeBuffer.array())) {
                case "m":
                    handleReceiveMessage(channel);
                    break;
                default:
                    messageArea.appendText("Received: unregistered action.\n");
            }
        } catch (Exception e) {
            System.out.println("Error on read messages: " + e);
        }
    }


    /**
     * Disconnect from the server.
     *
     * @param message - reason of disconnection.
     */
    private void disconnectChannel (String message) {
        try {
            System.out.println(message);
            isRunning = false;
            if(socketRead != null && socketRead.isConnected()) {
                socketRead.close();
            }
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Error occur on disconnecting with server.");
        }
    }

    /**
     * Handle receive message from server.
     *
     * @param channel - from where retrieve data.
     */
    private void handleReceiveMessage (SocketChannel channel) {
        try {
            String msg = Utils.readToBufferAsString(BUFFER_SIZE, channel);
            messageArea.appendText(msg + "\n");
        } catch (Exception e) {
            System.out.println("Error on read messages: " + e);
        }
    }

    /**
     * Send message to server.
     *
     * @param message - text message for sending.
     */
    public void sendMessage (String message) {
        if (message.split(" ")[0].equals("/download")) {
            downloadFile(message);
        } else {
            writeBuffer.clear();
            writeBuffer.put("m".getBytes());
            writeBuffer = Utils.setStringToBuffer(writeBuffer, BUFFER_NAME, this.getName());
            writeBuffer.put(message.getBytes());
            writeBuffer.flip();
            try {
                socketRead.write(writeBuffer);
            } catch (Exception e) {
                System.out.println("Error on write message: " + e);
            }
            switch (message) {
                case "/exit":
                case "/q":
                    disconnectChannel("Client closed application.");
                    break;
                case "/login":
                    break;
                default:
                    messageArea.appendText("[" + this.getName() + "]: " + message + "\n");
            }
        }
    }

    /**
     * Send file to server.
     *
     * @param path - path to file on local PC.
     */
    public void sendFile (String path) {
        new Thread(() -> ClientFiles.sendFile(path, this.getName(), address)).start();
    }

    /**
     * Download file by name.
     *
     * @param message - command for downloading file in format `/download file_name`
     */
    private void downloadFile(String message) {
        if (message.split(" ").length <= 1) {
            messageArea.appendText("[System] error: incorrect command. Use command `/download file_name` for downloading file.\n");
            return;
        }
        String fileName = message.split(" ")[1];
        if (fileName.equals("")) {
            messageArea.appendText("[System] error: incorrect file name.\n");
        } else {
            messageArea.appendText("[System] file [" + fileName + "] was downloaded successfully.\n");
            new Thread(() -> ClientFiles.downloadFile(DIR_PATH, fileName, this.getName(), address)).start();
        }
    }
}