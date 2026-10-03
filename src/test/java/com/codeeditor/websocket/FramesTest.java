package com.codeeditor.websocket;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class FramesTest {

    @Test
    void encodeThenDecodeRoundTripsTypeAndPayload() {
        byte[] payload = {10, 20, 30, 40};
        byte[] frame = Frames.encode(YjsMessageType.SYNC, payload);

        ByteBuffer buffer = ByteBuffer.wrap(frame);
        assertEquals(YjsMessageType.SYNC, Frames.type(buffer));
        assertArrayEquals(payload, Frames.payload(buffer));
    }

    @Test
    void encodeTypeOnlyHasEmptyPayload() {
        byte[] frame = Frames.encode(YjsMessageType.SYNCED);
        ByteBuffer buffer = ByteBuffer.wrap(frame);

        assertEquals(1, frame.length);
        assertEquals(YjsMessageType.SYNCED, Frames.type(buffer));
        assertEquals(0, Frames.payload(buffer).length);
    }

    @Test
    void nullPayloadEncodesAsTypeOnly() {
        byte[] frame = Frames.encode(YjsMessageType.RESET, null);
        assertEquals(1, frame.length);
        assertEquals(YjsMessageType.RESET, Frames.type(ByteBuffer.wrap(frame)));
    }
}
