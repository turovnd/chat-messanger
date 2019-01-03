package main.java.server;

import java.nio.channels.SocketChannel;

public class ServerClientModel {
    private String name = null;
    private SocketChannel channel;

    ServerClientModel(SocketChannel channel, String name) {
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
