#!/bin/bash
# Install wscat first: npm install -g wscat

# Replace with your actual JWT token and Document ID
TOKEN="your.jwt.token.here"
DOC_ID="your-document-id"

echo "Connecting to WebSocket STOMP endpoint..."

# First connect
# For a real STOMP client, you must send the CONNECT frame first.
# Here's a basic raw payload to send via wscat manually once connected:
#
# CONNECT
# accept-version:1.1,1.0
# heart-beat:10000,10000
# Authorization:Bearer $TOKEN
#
# ^@
#
# SUBSCRIBE
# id:sub-0
# destination:/topic/document/$DOC_ID
#
# ^@
#
# SEND
# destination:/app/editor/join
# content-length:53
#
# {"docId":"$DOC_ID","userId":1}^@

wscat -H "Authorization: Bearer $TOKEN" -c ws://localhost:8080/ws/editor
