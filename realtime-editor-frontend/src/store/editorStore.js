import { create } from 'zustand';
import api from '../lib/api';

const useEditorStore = create((set, get) => ({
  document: null,
  revisions: [],
  onlineUsers: [], // [{ userId, color }]
  remoteCursors: {}, // { userId: { position: 5, type: 'insert', length: 1, color: '#f00', username: 'john' } }
  lastRemoteOp: null,
  isConnected: false,
  isLoading: false,
  error: null,

  setDocument: (doc) => set({ document: doc }),
  setConnectionStatus: (status) => set({ isConnected: status }),
  
  fetchDocument: async (id) => {
    set({ isLoading: true, error: null });
    try {
      const resp = await api.get(`/documents/${id}`);
      set({ document: resp.data, isLoading: false });
      return resp.data;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      return null;
    }
  },

  // STOMP Event Handlers
  handleOperationApplied: (msg) => {
    // Only update if it's from a different user or we have a specific sync strategy
    set((state) => {
      const { document } = state;
      if (document && document.id === msg.documentId) {
        const newVersion = Math.max(document.version, msg.revision);
        return { 
          document: { ...document, version: newVersion },
          lastRemoteOp: msg // Store the op so the editor component can apply it
        };
      }
      return state;
    });
  },

  handleCursorUpdate: (msg) => {
    set((state) => ({
      remoteCursors: {
        ...state.remoteCursors,
        [msg.userId]: { ...msg, lastSeen: Date.now() }
      }
    }));
  },

  handleUserJoined: (msg) => {
    set((state) => {
      // Avoid duplicates
      const exists = state.onlineUsers.find(u => u.userId === msg.userId);
      if (exists) return state;
      return { onlineUsers: [...state.onlineUsers, msg] };
    });
  },

  handleUserLeft: (msg) => {
    set((state) => {
      const newUsers = state.onlineUsers.filter(u => u.userId !== msg.userId);
      const newCursors = { ...state.remoteCursors };
      delete newCursors[msg.userId];
      return { onlineUsers: newUsers, remoteCursors: newCursors };
    });
  },

  // Cleanup
  clearEditor: () => set({
    document: null,
    onlineUsers: [],
    remoteCursors: {},
    isConnected: false,
    error: null
  })
}));

export default useEditorStore;
