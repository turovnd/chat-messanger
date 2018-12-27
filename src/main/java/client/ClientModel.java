package main.java.client;

import java.nio.channels.SocketChannel;

public class ClientModel {
    private String name = null;
    private SocketChannel channel;

    public ClientModel(SocketChannel channel, String name) {
        this.channel = channel;
        this.name = name;
    }

    public SocketChannel getChannel() {
        return this.channel;
    }

    public String getName() {
        return this.name;
    }

    public boolean isSameChannel(SocketChannel channel) {
        return this.channel.equals(channel);
    }
}
