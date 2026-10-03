import { create } from 'zustand';
import api from '../lib/api';

/**
 * Holds the current document's metadata. Real-time editing state (text,
 * presence, connection status) lives in the Yjs layer via useCollaboration.
 */
const useEditorStore = create((set) => ({
  document: null,
  isLoading: false,
  error: null,

  fetchDocument: async (id) => {
    set({ isLoading: true, error: null });
    try {
      const { data } = await api.get(`/documents/${id}`);
      set({ document: data, isLoading: false });
      return data;
    } catch (err) {
      set({
        error: err.response?.data?.message || 'Failed to load document',
        isLoading: false,
      });
      return null;
    }
  },

  clearEditor: () => set({ document: null, error: null }),
}));

export default useEditorStore;
