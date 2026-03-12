import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Plus, 
  FileText, 
  Search, 
  Settings, 
  LogOut, 
  Clock, 
  ChevronRight,
  Code2,
  Loader2
} from 'lucide-react';
import api from '../lib/api';
import useAuthStore from '../store/authStore';

export default function Dashboard() {
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [newDocData, setNewDocData] = useState({ title: '', language: 'javascript' });
  
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    try {
      // Assuming a generic GET /documents endpoint exists or we fetch the user's specific ones
      // For now, let's assume the backend has a way to list accessible docs
      const resp = await api.get('/documents');
      setDocuments(Array.isArray(resp.data) ? resp.data : []);
    } catch (err) {
      console.error('Failed to fetch documents', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateDocument = async (e) => {
    e.preventDefault();
    setIsCreating(true);
    try {
      const resp = await api.post('/documents', newDocData);
      navigate(`/d/${resp.data.id}`);
    } catch (err) {
      alert('Failed to create document');
    } finally {
      setIsCreating(false);
    }
  };

  const filteredDocs = documents.filter(doc => 
    doc.title.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="w-full h-full min-h-screen flex flex-col p-6 lg:p-10 max-w-7xl mx-auto">
      
      {/* Header */}
      <header className="flex items-center justify-between mb-10">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-primary-600 rounded-xl flex items-center justify-center shadow-lg shadow-primary-600/30">
            <Code2 size={24} className="text-white" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">CloudEdit</h1>
            <p className="text-xs text-slate-400">Collaborative Workspace</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right hidden sm:block">
            <p className="text-sm font-medium text-slate-200">{user?.username}</p>
            <p className="text-xs text-slate-500">Free Account</p>
          </div>
          <button 
            onClick={logout}
            className="p-2.5 bg-slate-800 hover:bg-slate-700 rounded-xl border border-slate-700 transition-colors text-slate-400 hover:text-white"
            title="Logout"
          >
            <LogOut size={20} />
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col gap-8">
        
        {/* Actions & Search */}
        <div className="flex flex-col md:flex-row gap-4 items-center justify-between">
          <div className="relative w-full md:w-96">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
            <input 
              type="text" 
              placeholder="Search documents..."
              className="input-field pl-11"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <button 
            onClick={() => document.getElementById('create-modal').showModal()}
            className="btn-primary w-full md:w-auto flex items-center justify-center gap-2"
          >
            <Plus size={20} />
            New Document
          </button>
        </div>

        {/* Document Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {isLoading ? (
            <div className="col-span-full py-20 flex flex-col items-center justify-center">
              <Loader2 className="animate-spin text-primary-500 mb-4" size={40} />
              <p className="text-slate-400">Loading your workspace...</p>
            </div>
          ) : filteredDocs.length === 0 ? (
            <div className="col-span-full py-20 glass-card flex flex-col items-center justify-center border-dashed border-2">
              <FileText className="text-slate-600 mb-4" size={48} />
              <p className="text-slate-400 font-medium">No documents found</p>
              <p className="text-slate-500 text-sm mt-1">Create your first collaborative document to get started</p>
            </div>
          ) : (
            filteredDocs.map((doc) => (
              <div 
                key={doc.id} 
                onClick={() => navigate(`/d/${doc.id}`)}
                className="glass-card p-6 cursor-pointer group"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="p-2.5 bg-slate-700/50 rounded-lg text-primary-400 group-hover:bg-primary-500 group-hover:text-white transition-all duration-300">
                    <FileText size={20} />
                  </div>
                  <div className="text-[10px] uppercase tracking-wider font-bold bg-slate-700 px-2 py-0.5 rounded text-slate-400">
                    {doc.language}
                  </div>
                </div>
                
                <h3 className="text-slate-200 font-semibold mb-1 group-hover:text-primary-400 transition-colors truncate">
                  {doc.title}
                </h3>
                
                <div className="flex items-center gap-1.5 text-xs text-slate-500 mb-4">
                  <Clock size={12} />
                  <span>Modified {new Date(doc.updatedAt).toLocaleDateString()}</span>
                </div>

                <div className="flex items-center justify-between pt-4 border-t border-slate-700/50">
                  <span className="text-[10px] text-slate-500 flex items-center gap-1">
                    <div className="w-1.5 h-1.5 rounded-full bg-green-500" /> 
                    Public Link
                  </span>
                  <ChevronRight size={14} className="text-slate-600 group-hover:translate-x-1 transition-transform" />
                </div>
              </div>
            ))
          )}
        </div>
      </main>

      {/* Create Document Modal (Using native dialog for simplicity/performance) */}
      <dialog id="create-modal" className="bg-transparent backdrop:bg-slate-950/80 p-0 rounded-2xl">
        <div className="glass-panel p-8 w-[400px] border-none shadow-2xl">
          <h2 className="text-xl font-bold text-white mb-6">Create New Document</h2>
          
          <form onSubmit={handleCreateDocument} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-400 mb-1.5">Title</label>
              <input 
                type="text" 
                className="input-field"
                placeholder="My Awesome Project"
                value={newDocData.title}
                onChange={(e) => setNewDocData({...newDocData, title: e.target.value})}
                required
                autoFocus
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-400 mb-1.5">Language</label>
              <select 
                className="input-field"
                value={newDocData.language}
                onChange={(e) => setNewDocData({...newDocData, language: e.target.value})}
              >
                <option value="javascript">JavaScript</option>
                <option value="typescript">TypeScript</option>
                <option value="java">Java</option>
                <option value="python">Python</option>
                <option value="html">HTML</option>
                <option value="css">CSS</option>
              </select>
            </div>

            <div className="flex gap-3 pt-4">
              <button 
                type="button" 
                onClick={() => document.getElementById('create-modal').close()}
                className="btn-secondary flex-1"
              >
                Cancel
              </button>
              <button 
                type="submit" 
                disabled={isCreating}
                className="btn-primary flex-1 flex items-center justify-center gap-2"
              >
                {isCreating ? <Loader2 size={18} className="animate-spin" /> : 'Create'}
              </button>
            </div>
          </form>
        </div>
      </dialog>

      <style>{`
        dialog::backdrop {
          backdrop-filter: blur(5px);
        }
      `}</style>
    </div>
  );
}
