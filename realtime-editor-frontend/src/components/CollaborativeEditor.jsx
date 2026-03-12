import React, { useRef, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import useWebSocket from '../hooks/useWebSocket';
import useEditorStore from '../store/editorStore';

const CollaborativeEditor = ({ docId, initialContent, language }) => {
  const monacoRef = useRef(null);
  const editorRef = useRef(null);
  const isLocalChange = useRef(false);
  const decorationIds = useRef([]);
  
  const { sendOperation, sendCursorMove } = useWebSocket(docId);
  const { 
    document: docState, 
    remoteCursors,
    handleOperationApplied // This is useful if we want to trigger manually, but we'll use a listener
  } = useEditorStore();

  const handleEditorDidMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;

    if (initialContent) {
      editor.setValue(initialContent);
    }

    editor.onDidChangeModelContent((event) => {
      if (isLocalChange.current) return;

      event.changes.forEach((change) => {
        const { rangeOffset, rangeLength, text } = change;
        
        if (text.length > 0 && rangeLength === 0) {
          // INSERT
          if (text.length === 1) {
            sendOperation({
              type: 'INSERT',
              position: rangeOffset,
              character: text,
              revision: docState?.version || 0
            });
          } else {
            for(let i=0; i<text.length; i++) {
              sendOperation({
                type: 'INSERT',
                position: rangeOffset + i,
                character: text[i],
                revision: docState?.version || 0
              });
            }
          }
        } else if (text.length === 0 && rangeLength > 0) {
          // DELETE
          for(let i=0; i<rangeLength; i++) {
            sendOperation({
              type: 'DELETE',
              position: rangeOffset,
              revision: docState?.version || 0
            });
          }
        }
      });
    });

    editor.onDidChangeCursorPosition((e) => {
      const position = editor.getModel().getOffsetAt(e.position);
      sendCursorMove(position);
    });
  };

  // Listen for remote operations in store
  useEffect(() => {
    if (!editorRef.current || !monacoRef.current) return;

    const unsubscribe = useEditorStore.subscribe(
      (state) => state.lastRemoteOp,
      (op) => {
        if (!op || !editorRef.current) return;
        
        // Skip ops from self (backend broadcasts all, but let's be safe)
        // Note: We need the current user ID to truly distinguish
        
        const editor = editorRef.current;
        const model = editor.getModel();
        const monaco = monacoRef.current;

        isLocalChange.current = true;
        
        const pos = model.getPositionAt(op.position);
        
        if (op.type === 'INSERT') {
          model.pushEditOperations(
            [],
            [{
              range: new monaco.Range(pos.lineNumber, pos.column, pos.lineNumber, pos.column),
              text: op.character,
              forceMoveMarkers: true
            }],
            () => null
          );
        } else if (op.type === 'DELETE') {
          const endPos = model.getPositionAt(op.position + 1);
          model.pushEditOperations(
            [],
            [{
              range: new monaco.Range(pos.lineNumber, pos.column, endPos.lineNumber, endPos.column),
              text: null,
              forceMoveMarkers: true
            }],
            () => null
          );
        }

        isLocalChange.current = false;
      }
    );

    return () => unsubscribe();
  }, []);

  // Sync Remote Cursors
  useEffect(() => {
    if (!editorRef.current || !monacoRef.current) return;
    
    const editor = editorRef.current;
    const monaco = monacoRef.current;
    const model = editor.getModel();

    const newDecorations = Object.values(remoteCursors).map(cursor => {
      const pos = model.getPositionAt(cursor.position);
      return {
        range: new monaco.Range(pos.lineNumber, pos.column, pos.lineNumber, pos.column),
        options: {
          className: `remote-cursor`,
          beforeContentClassName: `remote-cursor-line`,
          hoverMessage: { value: cursor.userName || 'User' },
          stickiness: monaco.editor.TrackedRangeStickiness.NeverGrowsWhenTypingAtEdges
        }
      };
    });

    // We can't easily set dynamic CSS colors per user via className without injecting styles
    // A better way is to provide a unique class for each color or use a style tag
    decorationIds.current = editor.deltaDecorations(decorationIds.current, newDecorations);

  }, [remoteCursors]);

  return (
    <div className="h-full w-full relative">
      <style>{`
        .remote-cursor-line {
          border-left: 2px solid #3b82f6;
          height: 100%;
          margin-left: -1px;
        }
        .remote-cursor:after {
          content: attr(data-username);
          position: absolute;
          top: -14px;
          left: 0;
          font-size: 10px;
          padding: 1px 4px;
          background: #3b82f6;
          color: white;
          border-radius: 2px;
          white-space: nowrap;
          pointer-events: none;
        }
      `}</style>
      <Editor
        height="100%"
        defaultLanguage={language || 'javascript'}
        defaultValue={initialContent}
        theme="vs-dark"
        onMount={handleEditorDidMount}
        options={{
          minimap: { enabled: true },
          fontSize: 14,
          fontFamily: 'JetBrains Mono, Menlo, Monaco, Courier New, monospace',
          padding: { top: 20 },
          smoothScrolling: true,
          cursorSmoothCaretAnimation: 'on',
          cursorBlinking: 'smooth',
          lineNumbers: 'on',
          renderLineHighlight: 'all',
          scrollbar: {
            verticalScrollbarSize: 8,
            horizontalScrollbarSize: 8
          }
        }}
      />
    </div>
  );
};

export default CollaborativeEditor;
