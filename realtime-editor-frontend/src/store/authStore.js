import { create } from 'zustand';
import api from '../lib/api';

const useAuthStore = create((set, get) => ({
  user: null,
  token: localStorage.getItem('token') || null,
  isAuthenticated: !!localStorage.getItem('token'),
  isLoading: false,
  error: null,

  login: async (username, password) => {
    set({ isLoading: true, error: null });
    try {
      const { data } = await api.post('/auth/login', { username, password });
      localStorage.setItem('token', data.token);
      set({
        token: data.token,
        user: data.user,
        isAuthenticated: true,
        isLoading: false,
      });
      return true;
    } catch (err) {
      set({
        error: err.response?.data?.message || 'Login failed',
        isLoading: false,
      });
      return false;
    }
  },

  register: async (username, email, password) => {
    set({ isLoading: true, error: null });
    try {
      await api.post('/auth/register', { username, email, password });
      set({ isLoading: false });
      return true;
    } catch (err) {
      set({
        error: err.response?.data?.message || 'Registration failed',
        isLoading: false,
      });
      return false;
    }
  },

  /** Rehydrate the current user after a page reload (token persists, state doesn't). */
  fetchMe: async () => {
    if (!get().token || get().user) return;
    try {
      const { data } = await api.get('/auth/me');
      set({ user: data });
    } catch {
      // token invalid/expired — the 401 interceptor will log us out
    }
  },

  logout: () => {
    localStorage.removeItem('token');
    set({ user: null, token: null, isAuthenticated: false });
  },

  clearError: () => set({ error: null }),
}));

// Global 401 handler (dispatched by the axios interceptor) forces a logout.
if (typeof window !== 'undefined') {
  window.addEventListener('auth:unauthorized', () => {
    useAuthStore.getState().logout();
  });
}

export default useAuthStore;
