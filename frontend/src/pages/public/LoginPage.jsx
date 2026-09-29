import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { GraduationCap, LogIn, Lock, Mail, AlertCircle, Info, Sparkles } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import PublicNavbar from '../../components/layout/PublicNavbar';

export default function LoginPage() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const toast = useToast();

  const handleLogin = async (e) => {
    if (e) e.preventDefault();
    if (!identifier.trim() || !password) {
      setErrorMessage('Please enter both identifier and password');
      return;
    }

    setLoading(true);
    setErrorMessage('');

    try {
      const user = await login(identifier.trim(), password);
      toast.success(`Welcome back, ${user.name || user.email}!`);

      if (user.role === 'ROLE_ADMIN') {
        navigate('/admin/dashboard');
      } else if (user.role === 'ROLE_ORGANIZER') {
        navigate('/organizer/dashboard');
      } else {
        navigate('/student/dashboard');
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Login failed. Please check your credentials.';
      setErrorMessage(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const setDemoCredentials = (idVal, passVal) => {
    setIdentifier(idVal);
    setPassword(passVal);
    setErrorMessage('');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      <PublicNavbar />

      <div className="flex-1 flex items-center justify-center p-4 sm:p-6 lg:p-8">
        <div className="max-w-md w-full space-y-6">
          {/* Header */}
          <div className="text-center space-y-2">
            <div className="inline-flex w-14 h-14 rounded-2xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400 items-center justify-center mb-2 shadow-inner">
              <GraduationCap className="w-8 h-8" />
            </div>
            <h2 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">
              Sign in to Portal
            </h2>
            <p className="text-xs text-slate-400">
              Access college events, registrations, attendance, and administration
            </p>
          </div>

          {searchParams.get('registered') && (
            <div className="p-4 rounded-xl bg-amber-950/70 border border-amber-800 text-amber-200 text-xs flex items-start gap-2.5">
              <Info className="w-4 h-4 shrink-0 mt-0.5 text-amber-400" />
              <div>
                <p className="font-semibold">Account Registered: PENDING Approval</p>
                <p className="mt-0.5 text-amber-300/80 leading-relaxed">
                  Your registration has been forwarded to the college administrator. You will be able to log in once your details are verified.
                </p>
              </div>
            </div>
          )}

          {/* Form Card */}
          <div className="bg-slate-900 border border-slate-800/90 rounded-2xl p-6 sm:p-8 shadow-2xl shadow-black/40">
            {errorMessage && (
              <div className="mb-5 p-3.5 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-200 text-xs flex items-start gap-2.5 leading-relaxed">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
                <span>{errorMessage}</span>
              </div>
            )}

            <form onSubmit={handleLogin} className="space-y-4">
              <Input
                label="Email or Enrollment ID"
                placeholder="e.g. admin@college.edu or EN2023CSE001"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                icon={Mail}
                required
              />

              <Input
                label="Password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                icon={Lock}
                required
              />

              <Button
                type="submit"
                variant="primary"
                className="w-full mt-2"
                size="md"
                loading={loading}
                icon={LogIn}
              >
                Sign In
              </Button>
            </form>

            <div className="mt-6 text-center text-xs text-slate-400">
              New college student?{' '}
              <Link to="/register" className="text-indigo-400 hover:text-indigo-300 font-semibold underline-offset-4 hover:underline">
                Create an account
              </Link>
            </div>
          </div>

          {/* Quick Demo Test Logins */}
          <div className="p-4 rounded-xl bg-slate-900/40 border border-slate-800/80 space-y-3">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
              <Sparkles className="w-3.5 h-3.5 text-amber-400" />
              <span>One-Click Test Accounts:</span>
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <button
                type="button"
                onClick={() => setDemoCredentials('admin@college.edu', 'Admin@123')}
                className="p-2 rounded-lg bg-slate-950 border border-slate-800 hover:border-slate-700 text-left text-slate-300 hover:text-white transition-colors"
              >
                <span className="block font-bold text-rose-400">ADMIN</span>
                admin@college.edu
              </button>

              <button
                type="button"
                onClick={() => setDemoCredentials('cs.dept@college.edu', 'Organizer@123')}
                className="p-2 rounded-lg bg-slate-950 border border-slate-800 hover:border-slate-700 text-left text-slate-300 hover:text-white transition-colors"
              >
                <span className="block font-bold text-purple-400">ORGANIZER</span>
                cs.dept@college.edu
              </button>

              <button
                type="button"
                onClick={() => setDemoCredentials('rahul.cse@college.edu', 'Student@123')}
                className="p-2 rounded-lg bg-slate-950 border border-slate-800 hover:border-slate-700 text-left text-slate-300 hover:text-white transition-colors"
              >
                <span className="block font-bold text-emerald-400">STUDENT (Approved)</span>
                EN2023CSE001 (Rahul)
              </button>

              <button
                type="button"
                onClick={() => setDemoCredentials('sneha.ece@college.edu', 'Student@123')}
                className="p-2 rounded-lg bg-slate-950 border border-slate-800 hover:border-slate-700 text-left text-slate-300 hover:text-white transition-colors"
              >
                <span className="block font-bold text-amber-400">STUDENT (Pending)</span>
                EN2024ECE089 (Sneha)
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
