package main.java.server;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class ServerUtil {

    private static CharsetDecoder decoder = Charset.forName( "UTF-8").newDecoder();

    /**
     * Read and deceode budder to string.
     *
     * @param buffer - ByteBuffer from that message needs to be read and deceoded.
     * @return {String}
     */
    public static String getStringFromBuffer(ByteBuffer buffer) throws CharacterCodingException {
        StringBuilder sb = new StringBuilder();
        buffer.flip();
        sb.append(ServerUtil.decoder.decode(buffer).toString());
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
}
