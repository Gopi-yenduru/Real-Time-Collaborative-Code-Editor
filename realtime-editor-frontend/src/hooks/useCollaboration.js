import { useEffect, useState } from 'react';
import * as Y from 'yjs';
import { Awareness } from 'y-protocols/awareness';
import { CollabProvider } from '../lib/collab';
import useAuthStore from '../store/authStore';
import { colorForUser } from '../lib/colors';

const WS_BASE = import.meta.env.VITE_WS_URL || 'ws://localhost:8080';

/**
 * Sets up (and tears down) a Yjs document + awareness + WebSocket provider for a
 * document. Returns the shared objects the editor binds to, plus live
 * connection status and the list of present collaborators.
 */
export default function useCollaboration(docId) {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);

  const [collab, setCollab] = useState(null); // { doc, text, awareness, provider }
  const [status, setStatus] = useState('connecting'); // connecting | connected | disconnected
  const [users, setUsers] = useState([]);

  useEffect(() => {
    if (!docId || !token) return undefined;

    const doc = new Y.Doc();
    const awareness = new Awareness(doc);
    const text = doc.getText('monaco');

    const name = user?.username || 'Anonymous';
    const color = colorForUser(user?.id ?? name);
    awareness.setLocalStateField('user', { name, color });

    const provider = new CollabProvider(WS_BASE, docId, token, { doc, awareness });

    const updateUsers = () => {
      setUsers(
        Array.from(awareness.getStates().entries())
          .filter(([, state]) => state.user)
          .map(([clientId, state]) => ({
            clientId,
            name: state.user.name,
            color: state.user.color,
            self: clientId === doc.clientID,
          })),
      );
    };

    const offStatus = provider.on('status', setStatus);
    const offReset = provider.on('reset', () => window.location.reload());
    awareness.on('change', updateUsers);
    updateUsers();

    // Expose the effect-created CRDT objects to the render tree. This runs once
    // per (docId, token) change — an intentional, one-off render, not a cascade.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setCollab({ doc, text, awareness, provider });

    return () => {
      offStatus();
      offReset();
      awareness.off('change', updateUsers);
      provider.destroy();
      awareness.destroy();
      doc.destroy();
      setCollab(null);
      setStatus('connecting');
      setUsers([]);
    };
  }, [docId, token, user?.id, user?.username]);

  return {
    doc: collab?.doc,
    text: collab?.text,
    awareness: collab?.awareness,
    provider: collab?.provider,
    status,
    users,
  };
}
