import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { GraduationCap, Calendar, LogIn, UserPlus, LayoutDashboard } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import Button from '../common/Button';

export default function PublicNavbar() {
  const { user, isAuthenticated, isAdmin, isOrganizer } = useAuth();
  const navigate = useNavigate();

  const getDashboardPath = () => {
    if (isAdmin) return '/admin/dashboard';
    if (isOrganizer) return '/organizer/dashboard';
    return '/student/dashboard';
  };

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800 bg-slate-950/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand Logo */}
        <Link to="/" className="flex items-center gap-3 group">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-indigo-700 flex items-center justify-center text-white shadow-lg shadow-indigo-500/20 group-hover:scale-105 transition-transform">
            <GraduationCap className="w-6 h-6" />
          </div>
          <div>
            <span className="font-bold text-lg text-white tracking-tight">Campus<span className="text-indigo-400">Events</span></span>
            <span className="hidden sm:inline-block ml-2 text-xs font-semibold px-2 py-0.5 rounded-full bg-indigo-950 text-indigo-300 border border-indigo-800/50">College EMS</span>
          </div>
        </Link>

        {/* Navigation Actions */}
        <div className="flex items-center gap-3">
          <Link
            to="/events"
            className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-slate-300 hover:text-white rounded-lg hover:bg-slate-900 transition-colors"
          >
            <Calendar className="w-4 h-4 text-slate-400" />
            <span>Discover Events</span>
          </Link>

          {isAuthenticated ? (
            <Button
              variant="primary"
              size="sm"
              icon={LayoutDashboard}
              onClick={() => navigate(getDashboardPath())}
            >
              Dashboard
            </Button>
          ) : (
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                icon={LogIn}
                onClick={() => navigate('/login')}
              >
                Sign In
              </Button>
              <Button
                variant="primary"
                size="sm"
                icon={UserPlus}
                onClick={() => navigate('/register')}
              >
                Register
              </Button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
