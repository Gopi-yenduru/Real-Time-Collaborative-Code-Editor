#!/bin/bash
# ---------------------------------------------------------------------------
# Collaboration WebSocket smoke test.
#
# The editor now uses a NATIVE (binary) WebSocket that carries Yjs CRDT frames —
# not STOMP. Each frame is: [1 type byte][opaque payload].
#   type 0 = SYNC (Yjs update)      type 3 = SYNCED
#   type 1 = AWARENESS              type 4 = SNAPSHOT
#   type 2 = SYNC_REQUEST           type 5 = RESET
#
# Auth + document access are checked during the HTTP handshake. The JWT travels
# as a query parameter (browsers can't set headers on a WebSocket):
#
#     ws://localhost:8080/ws/yjs/<DOC_ID>?token=<JWT>
#
# Because the payloads are binary Yjs updates, the real client is the React app
# (src/lib/collab.js). This script just proves the handshake authorizes and the
# socket opens. Install wscat first:  npm install -g wscat
# ---------------------------------------------------------------------------

TOKEN="${1:-your.jwt.token.here}"
DOC_ID="${2:-your-document-id}"

echo "Opening collaboration socket for document $DOC_ID ..."
echo "A successful connect + a binary SYNCED frame (type byte 0x03) means auth works."
echo

# --connect keeps the socket open; you'll see binary frames arrive after the
# server replays history. Ctrl-C to exit.
wscat -c "ws://localhost:8080/ws/yjs/${DOC_ID}?token=${TOKEN}"
