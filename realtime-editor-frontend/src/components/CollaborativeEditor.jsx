import { useEffect, useRef, useState } from 'react';
import Editor from '@monaco-editor/react';
import { MonacoBinding } from 'y-monaco';

/**
 * A Monaco editor bound to a shared Yjs text. y-monaco keeps the editor model
 * and the CRDT in lock-step and renders every remote user's cursor and
 * selection from the awareness state — no manual operation handling required.
 *
 * @param {object}   props
 * @param {import('yjs').Text}                 props.text        shared Y.Text
 * @param {import('y-protocols/awareness').Awareness} props.awareness
 * @param {object}   props.provider   CollabProvider (for the initial "synced" event)
 * @param {string}   props.language
 * @param {boolean}  props.readOnly
 * @param {string}   props.initialContent  legacy plain text to seed an empty doc
 * @param {(text: string) => void} props.onMirror  called (debounced) with plain text
 */
export default function CollaborativeEditor({
  text,
  awareness,
  provider,
  language,
  readOnly = false,
  initialContent = '',
  onMirror,
}) {
  const editorRef = useRef(null);
  const bindingRef = useRef(null);
  const [mounted, setMounted] = useState(false);

  const handleMount = (editor) => {
    editorRef.current = editor;
    setMounted(true);
  };

  // Bind the editor model to the Yjs text once both are ready.
  useEffect(() => {
    if (!mounted || !text || !awareness || !editorRef.current) return undefined;
    const model = editorRef.current.getModel();
    if (!model) return undefined;

    const binding = new MonacoBinding(text, model, new Set([editorRef.current]), awareness);
    bindingRef.current = binding;
    return () => {
      binding.destroy();
      bindingRef.current = null;
    };
  }, [mounted, text, awareness]);

  // Seed an empty document with legacy plain-text content, exactly once.
  useEffect(() => {
    if (!text || readOnly || !initialContent) return undefined;
    const doc = text.doc;

    const seed = () => {
      if (text.length > 0) return;
      const meta = doc.getMap('meta');
      if (meta.get('seeded')) return;
      doc.transact(() => {
        meta.set('seeded', true);
        text.insert(0, initialContent);
      });
    };

    if (provider?.synced) seed();
    return provider?.on('synced', seed);
  }, [text, provider, readOnly, initialContent]);

  // Mirror plain text back to the server (debounced) for previews/search + compaction.
  // Only our own edits trigger this, so N clients don't all write the same text.
  useEffect(() => {
    if (!text || readOnly || !onMirror) return undefined;
    let timer = null;
    const observer = (event) => {
      if (!event.transaction.local) return;
      clearTimeout(timer);
      timer = setTimeout(() => onMirror(text.toString()), 1500);
    };
    text.observe(observer);
    return () => {
      clearTimeout(timer);
      text.unobserve(observer);
    };
  }, [text, readOnly, onMirror]);

  return (
    <div className="h-full w-full relative">
      <Editor
        height="100%"
        language={language || 'plaintext'}
        theme="vs-dark"
        onMount={handleMount}
        options={{
          readOnly,
          minimap: { enabled: true },
          fontSize: 14,
          fontFamily: 'JetBrains Mono, Menlo, Monaco, Courier New, monospace',
          padding: { top: 20 },
          smoothScrolling: true,
          cursorSmoothCaretAnimation: 'on',
          cursorBlinking: 'smooth',
          lineNumbers: 'on',
          renderLineHighlight: 'all',
          scrollbar: { verticalScrollbarSize: 8, horizontalScrollbarSize: 8 },
        }}
      />
    </div>
  );
}
