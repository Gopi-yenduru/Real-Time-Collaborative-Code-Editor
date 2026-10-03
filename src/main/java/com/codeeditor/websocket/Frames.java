package com.codeeditor.websocket;

import java.nio.ByteBuffer;

/** Encodes/decodes the {@code [type byte][payload]} wire frame. */
public final class Frames {

    private Frames() {
    }

    public static byte[] encode(byte type, byte[] payload) {
        byte[] frame = new byte[1 + (payload == null ? 0 : payload.length)];
        frame[0] = type;
        if (payload != null && payload.length > 0) {
            System.arraycopy(payload, 0, frame, 1, payload.length);
        }
        return frame;
    }

    public static byte[] encode(byte type) {
        return new byte[]{type};
    }

    public static byte type(ByteBuffer buffer) {
        return buffer.get(0);
    }

    public static byte[] payload(ByteBuffer buffer) {
        byte[] payload = new byte[Math.max(0, buffer.remaining() - 1)];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = buffer.get(i + 1);
        }
        return payload;
    }
}
