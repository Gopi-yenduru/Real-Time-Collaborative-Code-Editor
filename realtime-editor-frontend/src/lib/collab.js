import * as Y from 'yjs';
import {
  Awareness,
  encodeAwarenessUpdate,
  applyAwarenessUpdate,
  removeAwarenessStates,
} from 'y-protocols/awareness';

/**
 * Wire protocol — must match the Java relay (com.codeeditor.websocket.YjsMessageType).
 * Every WebSocket frame is one type byte followed by an opaque payload.
 */
export const MSG = {
  SYNC: 0,        // a Yjs document update
  AWARENESS: 1,   // an awareness (cursor/presence) update
  SYNC_REQUEST: 2, // ask the server to replay the update history
  SYNCED: 3,      // server finished replaying history
  SNAPSHOT: 4,    // full merged state, compacts the server's log
  RESET: 5,       // discard local state and re-sync (after a restore)
};

/**
 * A minimal Yjs provider for our Spring backend.
 *
 * Unlike y-websocket (which needs a Yjs-aware server to diff state vectors),
 * this provider talks to a "dumb" relay: it flushes local state, asks for the
 * full history, and streams every local update up. Correctness is guaranteed by
 * Yjs — updates are commutative and idempotent, so replay-in-any-order converges.
 */
export class CollabProvider {
  /**
   * @param {string} wsBase   e.g. "ws://localhost:8080"
   * @param {string} docId
   * @param {string} token    JWT (sent as a query param — browsers can't set WS headers)
   * @param {{ doc?: Y.Doc, awareness?: Awareness }} [opts]
   */
  constructor(wsBase, docId, token, opts = {}) {
    this.wsBase = wsBase.replace(/\/$/, '');
    this.docId = docId;
    this.token = token;

    this.doc = opts.doc || new Y.Doc();
    this.awareness = opts.awareness || new Awareness(this.doc);

    this.ws = null;
    this.synced = false;
    this.shouldConnect = true;
    this.reconnectAttempts = 0;
    this.reconnectTimer = null;
    this.listeners = new Map();

    this.handleDocUpdate = this.handleDocUpdate.bind(this);
    this.handleAwarenessUpdate = this.handleAwarenessUpdate.bind(this);
    this.handleUnload = () =>
      removeAwarenessStates(this.awareness, [this.doc.clientID], 'unload');

    this.doc.on('update', this.handleDocUpdate);
    this.awareness.on('update', this.handleAwarenessUpdate);
    if (typeof window !== 'undefined') {
      window.addEventListener('beforeunload', this.handleUnload);
    }

    this.connect();
  }

  // --- tiny event emitter ---------------------------------------------------
  on(event, cb) {
    if (!this.listeners.has(event)) this.listeners.set(event, new Set());
    this.listeners.get(event).add(cb);
    return () => this.off(event, cb);
  }

  off(event, cb) {
    this.listeners.get(event)?.delete(cb);
  }

  emit(event, payload) {
    this.listeners.get(event)?.forEach((cb) => cb(payload));
  }

  // --- connection lifecycle -------------------------------------------------
  connect() {
    const url = `${this.wsBase}/ws/yjs/${encodeURIComponent(this.docId)}?token=${encodeURIComponent(this.token)}`;
    this.emit('status', 'connecting');

    let ws;
    try {
      ws = new WebSocket(url);
    } catch {
      this.scheduleReconnect();
      return;
    }
    ws.binaryType = 'arraybuffer';

    ws.onopen = () => {
      this.reconnectAttempts = 0;

      // 1) Flush any state accumulated while offline (safe: updates are idempotent).
      const state = Y.encodeStateAsUpdate(this.doc);
      if (state.length > 2) this.send(MSG.SYNC, state);

      // 2) Pull the authoritative history from the server.
      this.send(MSG.SYNC_REQUEST);

      // 3) Re-announce our presence.
      if (this.awareness.getLocalState() !== null) {
        this.send(MSG.AWARENESS, encodeAwarenessUpdate(this.awareness, [this.doc.clientID]));
      }
    };

    ws.onmessage = (event) => this.receive(new Uint8Array(event.data));

    ws.onclose = () => {
      this.ws = null;
      if (this.synced) this.synced = false;
      this.emit('status', 'disconnected');
      this.scheduleReconnect();
    };

    ws.onerror = () => {
      // onclose fires next and handles reconnection.
    };

    this.ws = ws;
  }

  scheduleReconnect() {
    if (!this.shouldConnect) return;
    const delay = Math.min(1000 * 2 ** this.reconnectAttempts, 15000);
    this.reconnectAttempts += 1;
    clearTimeout(this.reconnectTimer);
    this.reconnectTimer = setTimeout(() => this.connect(), delay);
  }

  // --- message handling -----------------------------------------------------
  receive(bytes) {
    if (bytes.length < 1) return;
    const type = bytes[0];
    const payload = bytes.subarray(1);

    switch (type) {
      case MSG.SYNC:
        // origin = this → our own doc-update handler won't echo it back.
        Y.applyUpdate(this.doc, payload, this);
        break;
      case MSG.AWARENESS:
        applyAwarenessUpdate(this.awareness, payload, this);
        break;
      case MSG.SYNCED:
        this.synced = true;
        this.emit('status', 'connected');
        this.emit('synced');
        break;
      case MSG.RESET:
        this.emit('reset');
        break;
      default:
        break;
    }
  }

  handleDocUpdate(update, origin) {
    if (origin === this) return; // came from the server; don't send it back
    this.send(MSG.SYNC, update);
  }

  handleAwarenessUpdate({ added, updated, removed }, origin) {
    if (origin === this) return;
    const changed = added.concat(updated, removed);
    this.send(MSG.AWARENESS, encodeAwarenessUpdate(this.awareness, changed));
  }

  send(type, payload) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    const body = payload || new Uint8Array(0);
    const frame = new Uint8Array(1 + body.length);
    frame[0] = type;
    frame.set(body, 1);
    this.ws.send(frame);
  }

  /** Ask the server to compact its update log into a single merged state. */
  sendSnapshot() {
    this.send(MSG.SNAPSHOT, Y.encodeStateAsUpdate(this.doc));
  }

  destroy() {
    this.shouldConnect = false;
    clearTimeout(this.reconnectTimer);
    removeAwarenessStates(this.awareness, [this.doc.clientID], 'destroy');
    this.doc.off('update', this.handleDocUpdate);
    this.awareness.off('update', this.handleAwarenessUpdate);
    if (typeof window !== 'undefined') {
      window.removeEventListener('beforeunload', this.handleUnload);
    }
    if (this.ws) {
      try {
        this.ws.close();
      } catch {
        // ignore
      }
      this.ws = null;
    }
  }
}
