package main.java.server;

import javafx.scene.control.TextArea;
import main.java.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


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
    private ByteBuffer writeBuffer;

    private InetSocketAddress listenAddress;
    private static final long CHANNEL_WRITE_SLEEP = 10L;

    private LinkedList<ServerClientModel> clients;
    private LinkedList<SocketChannel> connections;
    private TextArea messageArea;
    private ServerDB dataStore;
    
    private boolean isRunning;

    ServerNetwork(TextArea area, String address, int port, boolean dbClear) {
        this.messageArea = area;
        this.clients = new LinkedList<>();
        this.connections = new LinkedList<>();
        this.dataStore = new ServerDB("database", dbClear);
        this.listenAddress = new InetSocketAddress(address, port);
        this.typeBuffer = ByteBuffer.allocate(BUFFER_TYPE);
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
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
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
     *
     * @param key {SelectionKey}
     */
    private void doAccept(SelectionKey key) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            SocketAddress remoteAddress = socketChannel.socket().getRemoteSocketAddress();
            messageArea.appendText("New connection: " + remoteAddress + "\n");
            connections.add(socketChannel);
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
                channel.close();
                key.cancel();
                closeConnection(channel);
                return;
            }

            switch (new String(typeBuffer.array())) {
                case "m":
                    handleReceiveMessage(channel);
                    break;
                case "f":
                    handleReceiveFile(channel);
                    break;
                case "d":
                    handleDownloadFile(channel);
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
     * @param channel - from where data is retrieve.
     */
    private void handleReceiveMessage(SocketChannel channel) throws IOException {
        String user = Utils.readToBufferAsString(BUFFER_NAME, channel);
        String msg = Utils.readToBufferAsString(BUFFER_SIZE, channel);
        messageArea.appendText("[" + user + "]: " + msg + "\n");
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
                    case "/login":
                        ServerClientModel client = new ServerClientModel(channel, user);
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
     * @param channel - from where data is retrieve.
     */
    private void handleReceiveFile(SocketChannel channel) throws IOException {
        String user = Utils.readToBufferAsString(BUFFER_NAME, channel);
        String fileName = Utils.readToBufferAsString(BUFFER_NAME, channel);
        messageArea.appendText("User [" + user + "] start sending file [" + fileName + "].\n" );
        if(!ServerFiles.retrieveFromChannelFile(channel, fileName)) {
            sendMessage(channel, "[System]: Error occur on saving file. Try again later.");
            messageArea.appendText("User [" + user + "] does not send file [" + fileName + "] with error.\n" );
        } else {
            sendBroadcastMessage(channel, "System", "User [" + user + "] send a file [" + fileName + "].");
            messageArea.appendText("User [" + user + "] sent file [" + fileName + "] successfully.\n" );
        }
        closeConnection(channel);
    }


    /**
     * Handle downloading file
     *
     * @param channel - to where data send.
     */
    private void handleDownloadFile(SocketChannel channel) throws IOException {
        String user = Utils.readToBufferAsString(BUFFER_NAME, channel);
        String fileName = Utils.readToBufferAsString(BUFFER_NAME, channel);
        messageArea.appendText("User [" + user + "] start downloading file [" + fileName + "].\n" );
        if (!ServerFiles.sendToChannelFile(channel, fileName)) {
            sendMessage(channel, "[System]: Error occur on downloading file. Try again later.");
            messageArea.appendText("User [" + user + "] does not download file [" + fileName + "].\n" );
        } else {
            messageArea.appendText("User [" + user + "] downloaded file [" + fileName + "] successfully.\n" );
        }
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
        for (ServerClientModel client : clients) {
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
        ByteBuffer buffer = Utils.cloneBuffer(writeBuffer);
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
        result.append("\nMESSAGES HISTORY:\n");
        for (String message : messages) {
            result.append("- ").append(message).append("\n");
        }
        return result.toString();
    }

    /**
     * Get files name list from server directory as string.
     *
     * @return {String}
     */
    public String getFiles () {
        List<String> files = ServerFiles.getFiles();
        StringBuilder result = new StringBuilder();
        result.append("\nFILES LIST:\n");
        for (String f: files) {
            result.append("- ").append(f).append("\n");
        }
        result.append("For download file use command `/download file_name`.\n");
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
        for (ServerClientModel client: clients) {
            if (client.getName() != null) {
                msg.append(client.getName()).append(", ");
            }
        }
        if (msg.length() > 3) {
            return msg.substring(0, msg.length() - 2);
        }
        return "No names.";
    }

    /**
     * Get client by channel.
     *
     * @param channel - channel from where request came.
     * @return {Client|null}
     */
    private ServerClientModel getClientByChannel(SocketChannel channel) {
        for (ServerClientModel client: clients) {
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
        ServerClientModel client = getClientByChannel(channel);
        if (client != null) {
            clients.remove(client);
        }
    }

    public String getInfoStatus() {
        return "[INFO] Connections: " + connections.size() + ".\n[INFO] Clients " + clients.size() + ": " + getClientsName() + ".\n";
    }
}