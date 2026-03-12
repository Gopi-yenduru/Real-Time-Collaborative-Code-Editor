import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import useAuthStore from '../store/authStore';
import useEditorStore from '../store/editorStore';

export default function useWebSocket(docId) {
  const { token } = useAuthStore();
  const { 
    handleOperationApplied, 
    handleCursorUpdate, 
    handleUserJoined, 
    handleUserLeft,
    setConnectionStatus
  } = useEditorStore();
  
  const clientRef = useRef(null);

  useEffect(() => {
    if (!docId || !token) return;

    const socketUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws/editor';
    
    const client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        // console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('Connected to WebSocket');
      setConnectionStatus(true);

      // Subscribe to doc-specific operations
      client.subscribe(`/topic/document/${docId}`, (message) => {
        const payload = JSON.parse(message.body);
        handleOperationApplied(payload);
      });

      // Subscribe to document presence
      client.subscribe(`/topic/presence/${docId}`, (message) => {
        const payload = JSON.parse(message.body);
        if (payload.type === 'USER_JOINED') handleUserJoined(payload);
        if (payload.type === 'USER_LEFT') handleUserLeft(payload);
      });

      // Subscribe to cursor updates
      client.subscribe(`/topic/cursors/${docId}`, (message) => {
        const payload = JSON.parse(message.body);
        handleCursorUpdate(payload);
      });

      // Join the session explicitly
      client.publish({
        destination: '/app/editor/join',
        body: JSON.stringify({ docId })
      });
    };

    client.onStompError = (frame) => {
      console.error('STOMP Error:', frame.headers['message']);
      setConnectionStatus(false);
    };

    client.onDisconnect = () => {
      console.log('Disconnected');
      setConnectionStatus(false);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [docId, token]);

  const sendOperation = (op) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination: '/app/editor/operation',
        body: JSON.stringify({ ...op, docId })
      });
    }
  };

  const sendCursorMove = (position) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination: '/app/editor/cursor',
        body: JSON.stringify({ docId, position })
      });
    }
  };

  return { sendOperation, sendCursorMove };
}
