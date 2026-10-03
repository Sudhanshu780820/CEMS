import React, { useState, useEffect, useRef } from 'react';
import { Link, NavLink, useNavigate, useLocation } from 'react-router-dom';
import {
  GraduationCap,
  LayoutDashboard,
  Calendar,
  CalendarPlus,
  Users,
  UserCheck,
  Building2,
  BarChart3,
  LogOut,
  Bell,
  Menu,
  X,
  CheckCheck,
  CheckCircle2,
  CalendarCheck,
  User,
  History,
  QrCode
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/client';
import Badge from '../common/Badge';

export default function DashboardLayout({ children }) {
  const { user, logout, isAdmin, isOrganizer, isStudent } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [pendingStudentsCount, setPendingStudentsCount] = useState(0);
  const notifRef = useRef(null);
  const location = useLocation();

  // Close sidebar on route change on mobile
  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  // Fetch notifications and pending stats
  const fetchNotifications = async () => {
    try {
      const [notifRes, unreadRes] = await Promise.all([
        api.get('/notifications'),
        api.get('/notifications/unread-count')
      ]);
      setNotifications(notifRes.data || []);
      setUnreadCount(unreadRes.data?.unreadCount || 0);

      if (isAdmin) {
        const [pendingStudentsRes, pendingEventsRes] = await Promise.all([
          api.get('/admin/students/pending').catch(() => ({ data: [] })),
          api.get('/admin/events/pending').catch(() => ({ data: [] }))
        ]);
        const totalPending = (pendingStudentsRes.data?.length || 0) + (pendingEventsRes.data?.length || 0);
        setPendingStudentsCount(totalPending);
      }
    } catch (err) {
      console.warn('Could not fetch notifications:', err);
    }
  };

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 30000); // 30s polling for live notifications
    return () => clearInterval(interval);
  }, [isAdmin]);

  // Close notifications on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setNotificationsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleMarkAllRead = async () => {
    try {
      await api.put('/notifications/read-all');
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error(err);
    }
  };

  const handleNotificationClick = async (notif) => {
    if (!notif.isRead) {
      try {
        await api.put(`/notifications/${notif.id}/read`);
        setNotifications((prev) =>
          prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n))
        );
        setUnreadCount((c) => Math.max(0, c - 1));
      } catch (err) {
        console.error(err);
      }
    }
  };

  // Role Navigation Config
  const adminLinks = [
    { label: 'Dashboard', path: '/admin/dashboard', icon: LayoutDashboard },
    {
      label: 'Pending Approvals',
      path: '/admin/students/pending',
      icon: UserCheck,
      badge: pendingStudentsCount > 0 ? pendingStudentsCount : null,
      badgeColor: 'bg-amber-500 text-slate-950 font-bold'
    },
    { label: 'All Students', path: '/admin/students', icon: Users },
    { label: 'Organizers', path: '/admin/organizers', icon: User },
    { label: 'Events', path: '/admin/events', icon: Calendar },
    { label: 'Campus Venues', path: '/admin/venues', icon: Building2 },
    { label: 'Reports & Analytics', path: '/admin/reports', icon: BarChart3 },
  ];

  const organizerLinks = [
    { label: 'Dashboard', path: '/organizer/dashboard', icon: LayoutDashboard },
    { label: 'My Events', path: '/organizer/events', icon: Calendar },
    { label: 'Create Event', path: '/organizer/events/create', icon: CalendarPlus },
  ];

  const studentLinks = [
    { label: 'Dashboard', path: '/student/dashboard', icon: LayoutDashboard },
    { label: 'Browse Events', path: '/student/events', icon: Calendar },
    { label: 'My Registrations', path: '/student/registrations', icon: CalendarCheck },
    { label: 'Attendance History', path: '/student/attendance', icon: History },
    { label: 'My Profile', path: '/student/profile', icon: User },
  ];

  const currentLinks = isAdmin ? adminLinks : isOrganizer ? organizerLinks : studentLinks;

  const roleLabel = isAdmin ? 'Administrator' : isOrganizer ? 'Event Organizer' : 'Student Portal';
  const roleBadgeVariant = isAdmin ? 'danger' : isOrganizer ? 'purple' : 'primary';

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col md:flex-row">
      {/* Mobile Drawer Backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/80 backdrop-blur-sm md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar Navigation */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-72 bg-slate-900 border-r border-slate-800 flex flex-col transition-transform duration-300 ease-in-out md:sticky md:top-0 md:h-screen md:shrink-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
        }`}
      >
        {/* Brand Header */}
        <div className="h-16 px-6 border-b border-slate-800 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-indigo-700 flex items-center justify-center text-white shadow-lg shadow-indigo-500/20">
              <GraduationCap className="w-5 h-5" />
            </div>
            <div>
              <div className="font-bold text-base text-white tracking-tight leading-tight">CampusEvents</div>
              <div className="text-[10px] text-slate-400 font-semibold tracking-wider uppercase">{roleLabel}</div>
            </div>
          </Link>
          <button
            onClick={() => setSidebarOpen(false)}
            className="md:hidden text-slate-400 hover:text-white p-1"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* User Card */}
        <div className="p-4 mx-3 my-4 rounded-xl bg-slate-950/60 border border-slate-800/80">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-indigo-600/20 text-indigo-400 font-bold border border-indigo-500/30 flex items-center justify-center text-sm">
              {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-white truncate">{user?.name || user?.email}</p>
              <div className="flex items-center gap-1.5 mt-0.5">
                <Badge variant={roleBadgeVariant} size="sm">
                  {user?.role?.replace('ROLE_', '')}
                </Badge>
                {user?.branch && (
                  <span className="text-[11px] font-mono text-slate-400">({user.branch})</span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-3 space-y-1 overflow-y-auto">
          {currentLinks.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all duration-150 ${
                    isActive
                      ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20 font-semibold'
                      : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/60'
                  }`
                }
              >
                <div className="flex items-center gap-3">
                  <Icon className="w-4 h-4 shrink-0" />
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`px-2 py-0.5 text-xs rounded-full ${item.badgeColor || 'bg-slate-800 text-slate-300'}`}
                  >
                    {item.badge}
                  </span>
                )}
              </NavLink>
            );
          })}
        </nav>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Navbar */}
        <header className="h-16 border-b border-slate-800 bg-slate-900/50 backdrop-blur-md px-4 sm:px-6 flex items-center justify-between sticky top-0 z-30">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSidebarOpen(true)}
              className="md:hidden text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800"
            >
              <Menu className="w-5 h-5" />
            </button>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                {roleLabel}
              </span>
              <span className="text-slate-600">/</span>
              <span className="text-sm font-medium text-slate-200 capitalize">
                {location.pathname.split('/').filter(Boolean).pop() || 'Dashboard'}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Notification Bell Dropdown */}
            <div className="relative" ref={notifRef}>
              <button
                onClick={() => setNotificationsOpen(!notificationsOpen)}
                className="relative p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
                aria-label="Notifications"
              >
                <Bell className="w-5 h-5" />
                {unreadCount > 0 && (
                  <span className="absolute top-1.5 right-1.5 w-4 h-4 rounded-full bg-indigo-500 text-[10px] font-bold text-white flex items-center justify-center animate-pulse">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {notificationsOpen && (
                <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-slate-900 border border-slate-800 shadow-2xl shadow-black/50 z-50 overflow-hidden">
                  <div className="p-4 border-b border-slate-800 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-sm text-white">Notifications</span>
                      {unreadCount > 0 && (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-950 text-indigo-300 font-medium">
                          {unreadCount} new
                        </span>
                      )}
                    </div>
                    {unreadCount > 0 && (
                      <button
                        onClick={handleMarkAllRead}
                        className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1 font-medium"
                      >
                        <CheckCheck className="w-3.5 h-3.5" />
                        Mark all read
                      </button>
                    )}
                  </div>

                  <div className="max-h-80 overflow-y-auto divide-y divide-slate-800/60">
                    {notifications.length === 0 ? (
                      <div className="p-6 text-center text-xs text-slate-500">
                        No notifications yet
                      </div>
                    ) : (
                      notifications.slice(0, 15).map((notif) => (
                        <div
                          key={notif.id}
                          onClick={() => handleNotificationClick(notif)}
                          className={`p-3.5 text-left transition-colors cursor-pointer hover:bg-slate-800/50 ${
                            !notif.isRead ? 'bg-indigo-950/20' : ''
                          }`}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <p className="text-xs font-semibold text-white">{notif.title}</p>
                            {!notif.isRead && (
                              <span className="w-2 h-2 rounded-full bg-indigo-400 shrink-0 mt-1" />
                            )}
                          </div>
                          <p className="mt-1 text-xs text-slate-400 line-clamp-2 leading-relaxed">
                            {notif.message}
                          </p>
                          <span className="mt-1.5 block text-[10px] text-slate-500 font-mono">
                            {new Date(notif.createdAt).toLocaleDateString()} at{' '}
                            {new Date(notif.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Quick Public View */}
            <Link
              to="/events"
              className="text-xs font-medium text-slate-400 hover:text-white px-2.5 py-1.5 rounded-lg border border-slate-800 hover:bg-slate-800 transition-colors hidden sm:block"
            >
              View Public Events
            </Link>

            {/* Top-Right Sign Out Button */}
            <button
              onClick={logout}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-rose-400 hover:text-rose-300 hover:bg-rose-950/40 border border-rose-900/50 transition-all cursor-pointer shadow-sm shadow-rose-950/20"
              title="Sign Out of CampusEvents"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Sign Out</span>
            </button>
          </div>
        </header>

        {/* Dynamic Page Content */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
