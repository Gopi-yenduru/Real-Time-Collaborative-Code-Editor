package com.codeeditor.websocket;

/**
 * Wire protocol for the collaboration WebSocket. Every frame is a single binary
 * message: one type byte followed by an opaque payload (a Yjs update, an
 * awareness update, or nothing).
 *
 * <p>The server never decodes the payloads. Correctness comes entirely from
 * Yjs's CRDT guarantees: updates are commutative and idempotent, so replaying
 * the stored log — in any order, possibly with duplicates — reconstructs the
 * exact document state on every client.
 */
public final class YjsMessageType {

    private YjsMessageType() {
    }

    /** Document update (Yjs v1 binary). Client&harr;server, broadcast to peers. */
    public static final byte SYNC = 0;

    /** Awareness update (cursors/presence). Client&harr;server, relayed not stored. */
    public static final byte AWARENESS = 1;

    /** Client&rarr;server: "stream me the full update history". */
    public static final byte SYNC_REQUEST = 2;

    /** Server&rarr;client: the initial history has been fully sent. */
    public static final byte SYNCED = 3;

    /** Client&rarr;server: a merged full-state update that compacts the log. */
    public static final byte SNAPSHOT = 4;

    /** Server&rarr;client: discard local state and re-sync (after a restore). */
    public static final byte RESET = 5;
}
