package main.java.server;

import javafx.scene.control.TextArea;

import main.java.client.ClientModel;
import main.java.utils.FileUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;


/**
 *
 * This is a simple NIO based server.
 *
 */
class ServerNetwork extends Thread {
    private Selector selector;

    private final static int BUFFER_TYPE = 1;
    private final static int BUFFER_SIZE = 1024;
    private final static int BUFFER_NAME = 32;

    private ByteBuffer typeBuffer;
    private ByteBuffer nameBuffer;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;

    private InetSocketAddress listenAddress;
    private static final long CHANNEL_WRITE_SLEEP = 10L;

    private LinkedList<ClientModel> clients;
    private LinkedList<SocketChannel> connections;
    private TextArea messageArea;
    private ServerDB dataStore;
    
    private boolean isRunning;
    
    private final static String dataBaseName = "database";
    private final static boolean dataBaseClear = true;

    ServerNetwork(TextArea area, String address, int port) {
        this.messageArea = area;
        this.clients = new LinkedList<>();
        this.connections = new LinkedList<>();
        this.dataStore = new ServerDB(dataBaseName, dataBaseClear);
        this.listenAddress = new InetSocketAddress(address, port);
        this.typeBuffer = ByteBuffer.allocate(BUFFER_TYPE);
        this.nameBuffer = ByteBuffer.allocate(BUFFER_NAME);
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    @Override
    public void run() {
        startServer();
    }

    /**
     * Start the server
     */
    private void startServer() {
        try {
            this.selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            
            serverChannel.socket().bind(listenAddress);
            serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port >> " + listenAddress.getPort());

            isRunning = true;
            while (isRunning) {
                // wait for events
                int readyCount = selector.select();
                if (readyCount == 0) {
                    continue;
                }

                // process selected keys...
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    // Remove key from set so we don't process it twice
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) { // Accept client connections
                        this.doAccept(key);
                    } else if (key.isReadable()) { // Read from client
                        this.doRead(key);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error on starting server: " + e);
            System.exit(1);
        }
    }

    /**
     * Accept client connection.
     * @param key {SelectionKey}
     */
    private void doAccept(SelectionKey key) {
        try {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);
            Socket socket = channel.socket();
            SocketAddress remoteAddress = socket.getRemoteSocketAddress();
            channel.register(this.selector, SelectionKey.OP_READ);
            messageArea.appendText("New connection: " + remoteAddress + "\n");
            connections.add(channel);
        } catch (IOException e) {
            System.out.println("Error on create new connection.");
        }
    }

    /**
     * Read from the socket channel.
     *
     * @param key {SelectionKey}
     */
    private void doRead(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();

            typeBuffer.clear();
            int nBytes = channel.read(typeBuffer);

            if (nBytes == -1) {
                closeConnection(channel);
                channel.close();
                key.cancel();
                return;
            }

            nameBuffer.clear();
            channel.read(nameBuffer);
            // user or file name
            String name = ServerUtil.getStringFromBuffer(nameBuffer);

            switch (new String(typeBuffer.array())) {
                case "m":
                    handleReceiveMessage(name, channel);
                    break;
                case "f":
                    handleReceiveFile(name, channel);
                    break;
                case "d":
                    handleDownloadFile(name, channel);
                    break;
                default:
                    messageArea.appendText("Received: unregistered action.\n");
            }
        } catch (IOException e) {
            System.out.println("Error occur in reading from socket: " + e);
        }
    }


    /**
     * Handle receive message from channel.
     *
     * @param user - user name.
     * @param channel - from where data is retrieve.
     */
    private void handleReceiveMessage(String user, SocketChannel channel) throws IOException {
        channel.read(readBuffer);
        String msg = ServerUtil.getStringFromBuffer(readBuffer);
        readBuffer.clear();
        messageArea.appendText(channel.socket().getRemoteSocketAddress() + " [" + user + "]: " + msg + "\n");
        switch (msg) {
            case "/online":
            case "/o":
                sendMessage(channel, "[System]: Online [" + clients.size() + "] users: " + getClientsName());
                break;
            case "/history":
            case "/h":
                sendMessage(channel, getMessageHistory());
                break;
            case "/files":
                sendMessage(channel, getFiles());
                break;
            default:
                switch (msg) {
                    case "/exit":
                    case "/q":
                        msg = "[" + user + "] logged out.";
                        user = "System";
                        break;
                    case "/send_file":
                        msg = "[" + user + "] send new file.";
                        user = "System";
                        break;
                    case "/login":
                        ClientModel client = new ClientModel(channel, user);
                        clients.add(client);
                        sendMessage(channel, "[System]: Welcome to our chat! Online " + clients.size() + " users.");
                        msg = "New user [" + user + "] logged in.";
                        user = "System";
                        break;
                }
                sendBroadcastMessage(channel, user, msg);
                break;
        }
    }



    /**
     * Handle receive file from channel.
     *
     * @param fileName - name of uploading file.
     * @param channel - from where data is retrieve.
     */
    private void handleReceiveFile(String fileName, SocketChannel channel) throws IOException {
        FileUtils.saveFile(channel, fileName);
        closeConnection(channel);
    }

    /**
     * TODO
     * Handle downloading file
     *
     * @param fileName - name of downloading file.
     * @param channel - to where data send.
     */
    private void handleDownloadFile(String fileName, SocketChannel channel) throws IOException {
        FileUtils.handleSendFile(channel, FileUtils.DIR_PATH + "/" + fileName);
        closeConnection(channel);
    }


    /**
     * Send message to chanel.
     *
     * @param channel - to where send message.
     * @param message - any string.
     */
    private void sendMessage(SocketChannel channel, String message) {
        writeBuffer.clear();
        writeBuffer.put("m".getBytes());
        writeBuffer.put(message.getBytes());
        writeBuffer.flip();
        writeToChanel(channel, writeBuffer);
    }

    /**
     * Send message for all socket channels in `clients` array without author chanel.
     *
     * @param from - chanel from where message came.
     * @param name - who send message.
     * @param msg - any string.
     */
    public void sendBroadcastMessage(SocketChannel from, String name, String msg) {
        String message = "[" + name + "]: " + msg;
        dataStore.addMessage(message);
        writeBuffer.clear();
        writeBuffer.put("m".getBytes());
        writeBuffer.put(message.getBytes());
        writeBuffer.flip();
        for (ClientModel client : clients) {
            if (client.getChannel() != from) {
                writeToChanel(client.getChannel(), writeBuffer);
            }
        }
        writeBuffer.clear();
    }

    /**
     * Write info from buffer to channel.
     *
     * @param channel - to where send data by socket.
     * @param writeBuffer - ByteBuffer with message.
     */
    private void writeToChanel(SocketChannel channel, ByteBuffer writeBuffer) {
        ByteBuffer buffer = ServerUtil.cloneBuffer(writeBuffer);
        long nBytes = 0;
        long toWrite = buffer.remaining();
        try {
            while (nBytes != toWrite) {
                nBytes += channel.write(buffer);
                try {
                    Thread.sleep(CHANNEL_WRITE_SLEEP);
                } catch (InterruptedException ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Error occur on write to " + channel.socket().getRemoteSocketAddress());
        }
    }

    /**
     * Get messages history from DB as string.
     *
     * @return {String}
     */
    public String getMessageHistory () {
        List<String> messages = dataStore.loadMessageHistory();
        StringBuilder result = new StringBuilder();
        result.append("\nSTART MESSAGES HISTORY\n");
        for (String message : messages) {
            result.append(message).append("\n");
        }
        result.append("FINISH MESSAGES HISTORY\n");
        return result.toString();
    }

    /**
     * Get files name list from server directory as string.
     *
     * @return {String}
     */
    public String getFiles () {
        List<String> files = FileUtils.getFiles();
        StringBuilder result = new StringBuilder();
        result.append("\nFILES LIST\n");
        for (String f: files) {
            result.append(f).append("\n");
        }
        result.append("For download file use `/f:file_name`.\n");
        return result.toString();
    }

    /**
     * Disconnect server.
     *
     * @param msg - reason of disconnection.
     */
    public void disconnect (String msg) {
        System.out.println(msg);
        isRunning = false;
        System.exit(0);
    }

    /**
     * Get clients names as string.
     *
     * @return {String}
     */
    private String getClientsName() {
        StringBuilder msg = new StringBuilder();
        for (ClientModel client: clients) {
            if (client.getName() != null) {
                msg.append(client.getName()).append(", ");
            }
        }
        msg = new StringBuilder(msg.substring(0, msg.length() - 2));
        return msg.toString();
    }

    /**
     * Get client by channel.
     *
     * @param channel - channel from where request came.
     * @return {Client|null}
     */
    private ClientModel getClientByChannel(SocketChannel channel) {
        for (ClientModel client: clients) {
            if (client.isSameChannel(channel)) {
                return client;
            }
        }
        return null;
    }

    /**
     * Close connection by chanel.
     *
     * @param channel - channel for closing.
     */
    private void closeConnection(SocketChannel channel) {
        messageArea.appendText("Closed connection: " + channel.socket().getRemoteSocketAddress() + "\n");
        connections.remove(channel);
        ClientModel client = getClientByChannel(channel);
        if (client != null) {
            clients.remove(client);
        }
    }
}