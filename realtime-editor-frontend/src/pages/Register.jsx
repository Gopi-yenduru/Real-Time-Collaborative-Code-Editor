import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserPlus, ArrowRight, Loader2, Mail, Lock, User } from 'lucide-react';
import useAuthStore from '../store/authStore';

export default function Register() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const { register, isLoading, error, clearError } = useAuthStore();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    clearError();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      alert("Passwords do not match");
      return;
    }
    const success = await register(formData.username, formData.email, formData.password);
    if (success) {
      navigate('/login');
    }
  };

  return (
    <div className="w-full max-w-md mx-auto relative z-10 p-8">
      <div className="glass-card p-10 flex flex-col items-center">
        
        <div className="w-16 h-16 bg-primary-600/20 rounded-2xl flex items-center justify-center mb-6 border border-primary-500/30">
          <UserPlus size={32} className="text-primary-400" />
        </div>
        
        <h1 className="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-slate-400 mb-2">
          Create Account
        </h1>
        <p className="text-slate-400 mb-8 text-center">
          Join the community of collaborative developers
        </p>

        {error && (
          <div className="w-full p-3 mb-6 bg-red-500/10 border border-red-500/50 rounded-lg text-red-400 text-sm font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="w-full space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5 ml-1 flex items-center gap-2">
              <User size={14} /> Username
            </label>
            <input
              name="username"
              type="text"
              value={formData.username}
              onChange={handleChange}
              className="input-field"
              placeholder="johndoe"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5 ml-1 flex items-center gap-2">
              <Mail size={14} /> Email
            </label>
            <input
              name="email"
              type="email"
              value={formData.email}
              onChange={handleChange}
              className="input-field"
              placeholder="john@example.com"
              required
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5 ml-1 flex items-center gap-2">
              <Lock size={14} /> Password
            </label>
            <input
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              className="input-field"
              placeholder="••••••••"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5 ml-1 flex items-center gap-2">
              <Lock size={14} /> Confirm Password
            </label>
            <input
              name="confirmPassword"
              type="password"
              value={formData.confirmPassword}
              onChange={handleChange}
              className="input-field"
              placeholder="••••••••"
              required
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="btn-primary w-full flex items-center justify-center gap-2 mt-2"
          >
            {isLoading ? <Loader2 size={20} className="animate-spin" /> : 'Sign Up'}
            {!isLoading && <ArrowRight size={18} />}
          </button>
        </form>

        <p className="mt-8 text-slate-400 text-sm">
          Already have an account?{' '}
          <Link to="/login" className="text-primary-400 hover:text-primary-300 font-medium transition-colors">
            Sign In
          </Link>
        </p>
      </div>
    </div>
  );
}
