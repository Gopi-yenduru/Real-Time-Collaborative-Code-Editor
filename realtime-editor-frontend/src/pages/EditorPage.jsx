import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import * as Y from 'yjs';
import {
  ArrowLeft, Users, History, Share2, Code2, Loader2, Save, X, RotateCcw,
} from 'lucide-react';
import useEditorStore from '../store/editorStore';
import useCollaboration from '../hooks/useCollaboration';
import CollaborativeEditor from '../components/CollaborativeEditor';
import { initials } from '../lib/colors';
import { toBase64 } from '../lib/bytes';
import api from '../lib/api';

const STATUS_STYLES = {
  connected: { label: 'LIVE', cls: 'bg-green-500/20 text-green-400 border-green-500/30' },
  connecting: { label: 'CONNECTING', cls: 'bg-amber-500/20 text-amber-400 border-amber-500/30' },
  disconnected: { label: 'OFFLINE', cls: 'bg-red-500/20 text-red-400 border-red-500/30' },
};

export default function EditorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { document: doc, fetchDocument, clearEditor } = useEditorStore();

  const { doc: ydoc, text, awareness, provider, status, users } = useCollaboration(id);

  const [shareOpen, setShareOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);

  useEffect(() => {
    fetchDocument(id);
    return () => clearEditor();
  }, [id, fetchDocument, clearEditor]);

  const readOnly = doc?.role === 'VIEWER';

  // Persist the plain-text mirror and compact the server log (editors only).
  const handleMirror = useCallback(
    async (content) => {
      try {
        await api.put(`/documents/${id}/content`, { content });
        provider?.sendSnapshot();
      } catch {
        // transient; will retry on the next edit
      }
    },
    [id, provider],
  );

  if (!doc) {
    return (
      <div className="h-screen w-full flex items-center justify-center bg-bg-dark">
        <div className="animate-pulse flex flex-col items-center">
          <Code2 size={48} className="text-primary-500 mb-4" />
          <p className="text-slate-400">Loading your workspace...</p>
        </div>
      </div>
    );
  }

  const statusStyle = STATUS_STYLES[status] || STATUS_STYLES.connecting;

  return (
    <div className="h-screen w-screen flex flex-col bg-bg-dark overflow-hidden">
      {/* Top navbar */}
      <nav className="h-14 border-b border-slate-800 flex items-center justify-between px-4 bg-slate-900/50 backdrop-blur-md">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/')}
            className="p-1.5 hover:bg-slate-800 rounded-lg text-slate-400 transition-colors"
          >
            <ArrowLeft size={18} />
          </button>
          <div className="h-6 w-[1px] bg-slate-800 mx-1" />
          <div className="flex flex-col">
            <h1 className="text-sm font-semibold text-white flex items-center gap-2">
              {doc.title}
              <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold border ${statusStyle.cls}`}>
                {statusStyle.label}
              </span>
              {readOnly && (
                <span className="px-1.5 py-0.5 rounded text-[10px] font-bold border bg-slate-700/40 text-slate-300 border-slate-600">
                  READ-ONLY
                </span>
              )}
            </h1>
            <span className="text-[10px] text-slate-500 uppercase tracking-tighter">{doc.language}</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <PresenceAvatars users={users} />
          <div className="flex items-center gap-1">
            {!readOnly && (
              <button
                onClick={() => setShareOpen(true)}
                className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-lg transition-all border border-slate-700"
              >
                <Share2 size={14} /> Share
              </button>
            )}
            <button
              onClick={() => setHistoryOpen(true)}
              title="Version history"
              className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
            >
              <History size={18} />
            </button>
          </div>
        </div>
      </nav>

      {/* Main */}
      <div className="flex-1 flex overflow-hidden">
        <div className="flex-1 relative">
          <CollaborativeEditor
            text={text}
            awareness={awareness}
            provider={provider}
            language={doc.language}
            readOnly={readOnly}
            initialContent={doc.content}
            onMirror={handleMirror}
          />
          {status !== 'connected' && (
            <div className="absolute top-3 left-1/2 -translate-x-1/2 z-10 flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-900/90 border border-slate-700 text-xs text-slate-300 shadow-lg">
              <Loader2 size={12} className="animate-spin" />
              {status === 'connecting' ? 'Connecting to collaboration server…' : 'Reconnecting…'}
            </div>
          )}
        </div>

        {/* Collaborators panel */}
        <aside className="w-72 border-l border-slate-800 hidden xl:flex flex-col bg-slate-900/20">
          <div className="p-4 border-b border-slate-800 flex items-center gap-2">
            <Users size={16} className="text-primary-400" />
            <span className="text-xs font-bold text-slate-300">
              COLLABORATORS ({users.length})
            </span>
          </div>
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {users.length === 0 && (
              <p className="text-xs text-slate-500">No one else is here right now.</p>
            )}
            {users.map((u) => (
              <div
                key={u.clientId}
                className="flex items-center gap-3 p-2 rounded-lg bg-slate-800/30 border border-slate-800"
              >
                <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                <div
                  className="w-6 h-6 rounded flex items-center justify-center text-[10px] text-white font-bold"
                  style={{ backgroundColor: u.color }}
                >
                  {initials(u.name)}
                </div>
                <span className="text-sm text-slate-300">
                  {u.name}
                  {u.self && <span className="text-slate-500"> (you)</span>}
                </span>
              </div>
            ))}
          </div>
        </aside>
      </div>

      {shareOpen && <ShareDialog docId={id} onClose={() => setShareOpen(false)} />}
      {historyOpen && (
        <HistoryDrawer
          docId={id}
          ydoc={ydoc}
          text={text}
          readOnly={readOnly}
          onClose={() => setHistoryOpen(false)}
        />
      )}
    </div>
  );
}

function PresenceAvatars({ users }) {
  return (
    <div className="flex -space-x-2 overflow-hidden">
      {users.slice(0, 5).map((u) => (
        <div
          key={u.clientId}
          className="w-8 h-8 rounded-full border-2 border-slate-900 flex items-center justify-center text-[10px] font-bold text-white shadow-xl"
          style={{ backgroundColor: u.color }}
          title={u.self ? `${u.name} (you)` : u.name}
        >
          {initials(u.name)}
        </div>
      ))}
      {users.length > 5 && (
        <div className="w-8 h-8 rounded-full border-2 border-slate-900 bg-slate-700 flex items-center justify-center text-[10px] font-bold text-white">
          +{users.length - 5}
        </div>
      )}
    </div>
  );
}

function ShareDialog({ docId, onClose }) {
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('EDITOR');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setMessage(null);
    try {
      await api.post(`/documents/${docId}/share`, { email, role });
      setMessage({ ok: true, text: `Shared with ${email}` });
      setEmail('');
    } catch (err) {
      setMessage({ ok: false, text: err.response?.data?.message || 'Failed to share' });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal onClose={onClose} title="Share document">
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-slate-400 mb-1.5">Collaborator email</label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="input-field"
            placeholder="teammate@example.com"
            autoFocus
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-400 mb-1.5">Access level</label>
          <select value={role} onChange={(e) => setRole(e.target.value)} className="input-field">
            <option value="EDITOR">Editor — can edit</option>
            <option value="VIEWER">Viewer — read only</option>
          </select>
        </div>
        {message && (
          <p className={`text-sm ${message.ok ? 'text-green-400' : 'text-red-400'}`}>{message.text}</p>
        )}
        <div className="flex gap-3 pt-2">
          <button type="button" onClick={onClose} className="btn-secondary flex-1">Close</button>
          <button type="submit" disabled={busy} className="btn-primary flex-1 flex items-center justify-center gap-2">
            {busy ? <Loader2 size={18} className="animate-spin" /> : 'Share'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function HistoryDrawer({ docId, ydoc, text, readOnly, onClose }) {
  const [snapshots, setSnapshots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [label, setLabel] = useState('');

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get(`/documents/${docId}/snapshots`, { params: { size: 50 } });
      setSnapshots(data.content || []);
    } catch {
      setSnapshots([]);
    } finally {
      setLoading(false);
    }
  }, [docId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const saveVersion = async () => {
    if (!ydoc || !text) return;
    setSaving(true);
    try {
      const state = toBase64(Y.encodeStateAsUpdate(ydoc));
      await api.post(`/documents/${docId}/snapshots`, {
        label: label || `Version ${new Date().toLocaleString()}`,
        state,
        content: text.toString(),
      });
      setLabel('');
      await refresh();
    } finally {
      setSaving(false);
    }
  };

  const restore = async (snapshotId) => {
    if (!window.confirm('Restore this version? Current content will be replaced for everyone.')) return;
    await api.post(`/documents/${docId}/snapshots/${snapshotId}/restore`);
    // Server broadcasts RESET → provider reloads the page automatically.
  };

  return (
    <Modal onClose={onClose} title="Version history" wide>
      {!readOnly && (
        <div className="flex gap-2 mb-4">
          <input
            className="input-field"
            placeholder="Label this version (optional)"
            value={label}
            onChange={(e) => setLabel(e.target.value)}
          />
          <button onClick={saveVersion} disabled={saving} className="btn-primary flex items-center gap-2 whitespace-nowrap">
            {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
            Save version
          </button>
        </div>
      )}

      <div className="max-h-80 overflow-y-auto space-y-2">
        {loading ? (
          <div className="py-10 flex justify-center"><Loader2 className="animate-spin text-primary-500" /></div>
        ) : snapshots.length === 0 ? (
          <p className="text-sm text-slate-500 py-6 text-center">No saved versions yet.</p>
        ) : (
          snapshots.map((s) => (
            <div key={s.id} className="flex items-center justify-between p-3 rounded-lg bg-slate-800/40 border border-slate-800">
              <div className="min-w-0">
                <p className="text-sm text-slate-200 truncate">{s.label}</p>
                <p className="text-[11px] text-slate-500">
                  {s.authorUsername || 'Unknown'} · {new Date(s.createdAt).toLocaleString()}
                </p>
              </div>
              {!readOnly && (
                <button
                  onClick={() => restore(s.id)}
                  className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs bg-slate-700 hover:bg-slate-600 rounded-lg text-slate-200 border border-slate-600"
                >
                  <RotateCcw size={13} /> Restore
                </button>
              )}
            </div>
          ))
        )}
      </div>
    </Modal>
  );
}

function Modal({ title, children, onClose, wide }) {
  const ref = useRef(null);
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 backdrop-blur-sm p-4"
      onMouseDown={(e) => e.target === ref.current && onClose()}
      ref={ref}
    >
      <div className={`glass-panel p-6 rounded-2xl w-full ${wide ? 'max-w-lg' : 'max-w-md'}`}>
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-bold text-white">{title}</h2>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg">
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
