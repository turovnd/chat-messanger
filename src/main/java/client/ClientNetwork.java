package main.java.client;

import javafx.scene.control.TextArea;
import main.java.utils.FileUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

class ClientNetwork extends Thread {

    private final static int BUFFER_TYPE = 1;
    private static final int BUFFER_SIZE = 1024;
    private final static int BUFFER_NAME = 32;

    private ByteBuffer typeBuffer;
    private ByteBuffer nameBuffer;
    private ByteBuffer writeBuffer;
    private ByteBuffer readBuffer;

    private InetSocketAddress address;
    private SocketChannel socketMessage;

    private CharsetDecoder stringDecoder;
    private TextArea messageArea;
    private boolean isRunning;

    ClientNetwork(String address, int port, TextArea area) {
        this.messageArea = area;
        this.address = new InetSocketAddress(address, port);
        this.typeBuffer = ByteBuffer.allocate(BUFFER_TYPE);
        this.nameBuffer = ByteBuffer.allocate(BUFFER_NAME);
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeBuffer = ByteBuffer.allocate(BUFFER_SIZE+BUFFER_NAME+BUFFER_TYPE);
        this.stringDecoder = Charset.forName( "UTF-8").newDecoder();
    }

    @Override
    public void run() {
        try {
            Selector readSelector = Selector.open();
            socketMessage = SocketChannel.open(address);
            socketMessage.configureBlocking(false);
            socketMessage.register(readSelector, SelectionKey.OP_READ, new StringBuffer());

            sendMessage("/login");
            System.out.println("Client connected");

            isRunning = true;
            while (isRunning) {
                // wait for events
                int readyCount = readSelector.select();
                if (readyCount == 0) {
                    continue;
                }

                // process selected keys...
                Set<SelectionKey> readyKeys = readSelector.selectedKeys();
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
            System.out.println("Error occur while connecting to main.java.server: " + e);
            System.exit(1);
        }
    }

    /**
     * Disconnect from the main.java.server.
     *
     * @param message - reason of disconnection.
     */
    private void disconnectChanel(String message) {
        try {
            System.out.println(message);
            isRunning = false;
            if(socketMessage != null && socketMessage.isConnected()) {
                socketMessage.close();
            }
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Error occur on disconnecting with main.java.server.");
        }
    }

    /**
     * Read incoming messages.
     */
    private void readMessages(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();

            typeBuffer.clear();

            int nBytes = channel.read(typeBuffer);

            if (nBytes == -1) {
                disconnectChanel("Server was disconnected.");
                return;
            }

            switch (new String(typeBuffer.array())) {
                case "m":
                    handleReceiveMessage(channel, key);
                    break;
                case "f":
                    // TODO
                    handleReceiveFile(channel, key);
                    break;
                default:
                    messageArea.appendText("Received: unregistered action.\n");
            }
        } catch (Exception e) {
            System.out.println("Error on read messages: " + e);
        }
    }

    /**
     * Handle receive message from main.java.server.
     *
     * @param channel - from where retrieve data.
     * @param key {SelectionKey}
     */
    private void handleReceiveMessage(SocketChannel channel, SelectionKey key) {
        try {
            readBuffer.clear();
            channel.read(readBuffer);

            StringBuffer sb = (StringBuffer) key.attachment();
            readBuffer.flip();

            sb.append(stringDecoder.decode(readBuffer).toString());
            readBuffer.clear();

            String msg = sb.toString();
            sb.delete(0, sb.length());

            messageArea.appendText(msg + "\n");
        } catch (Exception e) {
            System.out.println("Error on read messages: " + e);
        }
    }

    /**
     * Send message to main.java.server.
     */
    public void sendMessage(String message) {
        if (message.split(":")[0].equals("/f")) {
            downloadFile(message);
        } else {
            writeBuffer.clear();
            writeBuffer.put("m".getBytes());
            int i = 0;
            while (i < BUFFER_NAME) {
                try {
                    writeBuffer.put(this.getName().getBytes()[i]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    writeBuffer.put(" ".getBytes());
                }
                i++;
            }
            writeBuffer.put(message.getBytes());
            writeBuffer.flip();
            try {
                socketMessage.write(writeBuffer);
            } catch (Exception e) {
                System.out.println("Error on write message: " + e);
            }
            writeBuffer.rewind();
            if (message.equals("/exit") || message.equals("/q")) {
                disconnectChanel("Client closed application.");
            } else if (!message.equals("/login")) {
                messageArea.appendText("[" + this.getName() + "]: " + message + "\n");
            }
        }
    }

    /**
     * Handle receive file from main.java.server.
     * TODO
     * @param channel - from where retrieve data.
     * @param key {SelectionKey}
     */
    private void handleReceiveFile(SocketChannel channel, SelectionKey key) {

    }

    /**
     * Send file to main.java.server.
     *
     * @param path - path to file on local PC
     */
    public void sendFile(String path) {
        sendMessage("/send_file");
        messageArea.appendText("[" + this.getName() + "]: send a file\n");
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtils.sendFile(path, address);
            }
        }).start();
    }

    /**
     * Download file by name.
     *
     * @param message - message in format `/f:file_name`
     */
    private void downloadFile(String message) {
        if (message.split(":").length <= 1) {
            messageArea.appendText("[System] error: incorrect command.\n");
            return;
        }
        String fileName = message.split(":")[1];
        if (fileName.equals("")) {
            messageArea.appendText("[System] error: incorrect file name.\n");
        } else {
            messageArea.appendText("[System] downloading file `" + fileName + "`.\n");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileUtils.downloadFile(fileName, address);
                }
            }).start();
        }
    }
}