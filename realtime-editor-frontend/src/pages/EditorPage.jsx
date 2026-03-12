import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  Users, 
  History, 
  Share2, 
  Settings,
  CloudCheck,
  Code2
} from 'lucide-react';
import useEditorStore from '../store/editorStore';
import CollaborativeEditor from '../components/CollaborativeEditor';
import useWebSocket from '../hooks/useWebSocket';

export default function EditorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { 
    document, 
    fetchDocument, 
    onlineUsers, 
    isConnected,
    clearEditor 
  } = useEditorStore();
  
  const [activeTab, setActiveTab] = useState('editor'); // or 'history'

  useEffect(() => {
    fetchDocument(id);
    return () => clearEditor();
  }, [id]);

  if (!document) {
    return (
      <div className="h-screen w-full flex items-center justify-center bg-bg-dark">
        <div className="animate-pulse flex flex-col items-center">
          <Code2 size={48} className="text-primary-500 mb-4" />
          <p className="text-slate-400">Loading your workspace...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen w-screen flex flex-col bg-bg-dark overflow-hidden">
      
      {/* Top Navbar */}
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
              {document.title}
              {isConnected ? (
                <div className="bg-green-500/20 px-1.5 py-0.25 rounded text-[10px] text-green-400 font-bold border border-green-500/30">
                  LIVE
                </div>
              ) : (
                <div className="bg-red-500/20 px-1.5 py-0.25 rounded text-[10px] text-red-400 font-bold border border-red-500/30">
                  OFFLINE
                </div>
              )}
            </h1>
            <span className="text-[10px] text-slate-500 uppercase tracking-tighter">{document.language}</span>
          </div>
        </div>

        {/* Presence Bar */}
        <div className="flex items-center gap-3">
          <div className="flex -space-x-2 mr-4 overflow-hidden">
            {onlineUsers.map((u) => (
              <div 
                key={u.userId}
                className="w-8 h-8 rounded-full border-2 border-slate-900 flex items-center justify-center text-[10px] font-bold text-white shadow-xl"
                style={{ backgroundColor: u.color }}
                title={u.userName || `User ${u.userId}`}
              >
                {u.userName ? u.userName[0].toUpperCase() : 'U'}
              </div>
            ))}
          </div>
          
          <div className="flex items-center gap-1">
            <button className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-lg transition-all border border-slate-700">
              <Share2 size={14} />
              Share
            </button>
            <button className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
              <History size={18} />
            </button>
            <button className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
              <Settings size={18} />
            </button>
          </div>
        </div>
      </nav>

      {/* Main Container */}
      <div className="flex-1 flex overflow-hidden">
        
        {/* Sidebar (Optional - for file trees if we ever add them) */}
        {/* <div className="w-64 border-r border-slate-800 hidden lg:block" /> */}

        {/* Editor Area */}
        <div className="flex-1 relative">
          <CollaborativeEditor docId={id} initialContent={document.content} language={document.language} />
        </div>

        {/* Right Panel (Activity / Chat) */}
        <div className="w-72 border-l border-slate-800 hidden xl:flex flex-col bg-slate-900/20">
          <div className="p-4 border-b border-slate-800 flex items-center gap-2">
            <Users size={16} className="text-primary-400" />
            <span className="text-xs font-bold text-slate-300">COLLABORATORS ({onlineUsers.length})</span>
          </div>
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {onlineUsers.map(u => (
              <div key={u.userId} className="flex items-center gap-3 p-2 rounded-lg bg-slate-800/30 border border-slate-800">
                 <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                 <div 
                  className="w-6 h-6 rounded flex items-center justify-center text-[10px] text-white font-bold"
                  style={{ backgroundColor: u.color }}
                >
                  {u.userName ? u.userName[0].toUpperCase() : 'U'}
                </div>
                <span className="text-sm text-slate-300">{u.userName || 'Anonymous User'}</span>
              </div>
            ))}
          </div>
        </div>

      </div>
      
      {/* Footer Info */}
      <footer className="h-6 bg-primary-600 flex items-center justify-between px-3 text-[10px] text-white/80 font-medium">
        <div className="flex items-center gap-3">
          <span className="flex items-center gap-1"><CloudCheck size={12} /> Sync Active</span>
          <span>UTF-8</span>
        </div>
        <div>
          Row 1, Col 1
        </div>
      </footer>

    </div>
  );
}
