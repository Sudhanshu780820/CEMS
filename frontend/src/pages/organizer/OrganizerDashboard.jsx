import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Calendar,
  CalendarPlus,
  Users,
  Award,
  Clock,
  MapPin,
  QrCode,
  ArrowRight,
  TrendingUp,
  AlertCircle
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import QrSessionModal from '../../components/qr/QrSessionModal';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

export default function OrganizerDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeSessionEvent, setActiveSessionEvent] = useState(null);
  const [activeSessionToken, setActiveSessionToken] = useState(null);
  const navigate = useNavigate();
  const toast = useToast();

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [statsRes, eventsRes] = await Promise.all([
        api.get('/organizer/dashboard'),
        api.get('/organizer/events')
      ]);
      setStats(statsRes.data);
      setEvents(eventsRes.data || []);
    } catch (err) {
      console.error('Failed to load organizer dashboard:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleStartAttendance = async (event) => {
    try {
      const res = await api.post(`/organizer/events/${event.id}/attendance/start`);
      setActiveSessionToken(res.data.attendanceToken);
      setActiveSessionEvent(event);
      toast.success('Live attendance session started!');
      fetchDashboardData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to start attendance session');
    }
  };

  const getLocalDateString = () => {
    const d = new Date();
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  const today = getLocalDateString();
  const todayEvents = events.filter((e) => e.eventDate === today);

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Top Header Banner */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white">Organizer Dashboard</h1>
            <p className="text-xs text-slate-400">
              Welcome, {user?.name}! Manage department events, monitor registrations, and verify attendance
            </p>
          </div>

          <Button
            variant="primary"
            icon={CalendarPlus}
            onClick={() => navigate('/organizer/events/create')}
          >
            Create New Event
          </Button>
        </div>

        {/* Dashboard KPI Stat Cards */}
        {loading ? (
          <LoadingSpinner message="Calculating organizer performance metrics..." />
        ) : (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <Card hover className="flex items-center gap-3.5">
              <div className="w-11 h-11 rounded-xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center shrink-0">
                <Calendar className="w-5 h-5" />
              </div>
              <div>
                <p className="text-[11px] text-slate-400 font-medium">Total Events</p>
                <h3 className="text-xl font-bold text-white mt-0.5">{stats?.totalEvents || 0}</h3>
              </div>
            </Card>

            <Card hover className="flex items-center gap-3.5">
              <div className="w-11 h-11 rounded-xl bg-purple-600/20 text-purple-400 border border-purple-500/30 flex items-center justify-center shrink-0">
                <Clock className="w-5 h-5" />
              </div>
              <div>
                <p className="text-[11px] text-slate-400 font-medium">Upcoming Events</p>
                <h3 className="text-xl font-bold text-white mt-0.5">{stats?.upcomingEvents || 0}</h3>
              </div>
            </Card>

            <Card hover className="flex items-center gap-3.5">
              <div className="w-11 h-11 rounded-xl bg-emerald-600/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center shrink-0">
                <Users className="w-5 h-5" />
              </div>
              <div>
                <p className="text-[11px] text-slate-400 font-medium">Total Participants</p>
                <h3 className="text-xl font-bold text-white mt-0.5">{stats?.totalParticipants || 0}</h3>
              </div>
            </Card>

            <Card hover className="flex items-center gap-3.5">
              <div className="w-11 h-11 rounded-xl bg-amber-600/20 text-amber-400 border border-amber-500/30 flex items-center justify-center shrink-0">
                <TrendingUp className="w-5 h-5" />
              </div>
              <div>
                <p className="text-[11px] text-slate-400 font-medium">Attendance Rate</p>
                <h3 className="text-xl font-bold text-white mt-0.5">{stats?.overallAttendanceRate || 0}%</h3>
              </div>
            </Card>
          </div>
        )}

        {/* Today's Events Section */}
        {todayEvents.length > 0 && (
          <div className="p-5 rounded-2xl bg-indigo-950/30 border border-indigo-800/50 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
                <h3 className="text-base font-bold text-white">Happening Today ({todayEvents.length})</h3>
              </div>
              <span className="text-xs text-indigo-300 font-medium">Ready for Attendance Check-in</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {todayEvents.map((evt) => (
                <div
                  key={evt.id}
                  className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex flex-col justify-between space-y-3"
                >
                  <div>
                    <div className="flex justify-between items-center mb-1">
                      <span className="text-xs font-bold text-indigo-400">{evt.category}</span>
                      <Badge
                        variant={
                          evt.attendanceActive
                            ? 'success'
                            : evt.attendanceState === 'CHECK_IN_OPEN'
                            ? 'warning'
                            : evt.attendanceState === 'NOT_STARTED'
                            ? 'default'
                            : 'danger'
                        }
                        size="sm"
                      >
                        {evt.attendanceActive
                          ? 'QR Active'
                          : evt.attendanceState === 'CHECK_IN_OPEN'
                          ? 'Window Open'
                          : evt.attendanceState === 'NOT_STARTED'
                          ? 'Upcoming'
                          : 'Attendance Closed'}
                      </Badge>
                    </div>
                    <h4 className="text-sm font-bold text-white">{evt.title}</h4>
                    <p className="text-xs text-slate-400 mt-1">{evt.venue?.name} • {evt.startTime} to {evt.endTime}</p>
                  </div>

                  <div className="flex items-center gap-2 pt-2 border-t border-slate-800">
                    <Button
                      variant={evt.attendanceActive ? 'success' : 'primary'}
                      size="sm"
                      icon={QrCode}
                      className="flex-1"
                      onClick={() => {
                        if (evt.attendanceActive) {
                          setActiveSessionToken(evt.attendanceToken);
                          setActiveSessionEvent(evt);
                        } else {
                          handleStartAttendance(evt);
                        }
                      }}
                    >
                      {evt.attendanceActive ? 'Display QR Code' : 'Start QR Attendance'}
                    </Button>

                    <Link to={`/organizer/events/${evt.id}/attendance`}>
                      <Button variant="secondary" size="sm">
                        Console
                      </Button>
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Managed Events List */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-white">My Managed Events</h2>
              <p className="text-xs text-slate-400">Events scheduled and operated by your department</p>
            </div>
            <Link to="/organizer/events" className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1">
              View All ({events.length}) <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {events.length === 0 ? (
            <EmptyState
              title="No events created yet"
              description="Create your first college event to open registrations for students."
              actionLabel="Create Event"
              onAction={() => navigate('/organizer/events/create')}
            />
          ) : (
            <div className="space-y-4">
              {events.slice(0, 5).map((evt) => {
                const capacityPct = Math.min(100, Math.round((evt.registeredCount / evt.maxCapacity) * 100));

                return (
                  <div
                    key={evt.id}
                    className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col lg:flex-row lg:items-center justify-between gap-4 hover:border-slate-700 transition-all"
                  >
                    <div className="space-y-2 flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-bold text-indigo-300 px-2 py-0.5 rounded bg-indigo-950 border border-indigo-800/60">
                          {evt.category}
                        </span>
                        <Badge variant="default" size="sm">
                          {evt.status}
                        </Badge>
                      </div>

                      <Link to={`/organizer/events/${evt.id}/participants`} className="hover:text-indigo-400 transition-colors block">
                        <h3 className="text-base font-bold text-white truncate">{evt.title}</h3>
                      </Link>

                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-400">
                        <span>{evt.eventDate} ({evt.startTime} - {evt.endTime})</span>
                        <span>•</span>
                        <span className="truncate">{evt.venue?.name}</span>
                      </div>
                    </div>

                    {/* Progress Indicator */}
                    <div className="w-full lg:w-56 space-y-1">
                      <div className="flex justify-between text-xs">
                        <span className="text-slate-400">Registered:</span>
                        <span className="text-white font-mono font-bold">
                          {evt.registeredCount} / {evt.maxCapacity} ({capacityPct}%)
                        </span>
                      </div>
                      <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-800">
                        <div
                          className={`h-full ${capacityPct >= 100 ? 'bg-rose-500' : capacityPct >= 80 ? 'bg-amber-500' : 'bg-indigo-500'}`}
                          style={{ width: `${capacityPct}%` }}
                        />
                      </div>
                      <span className="text-[11px] text-slate-500">
                        {evt.remainingSeats} seats available
                      </span>
                    </div>

                    {/* Quick Button Strip */}
                    <div className="flex items-center gap-2 shrink-0 self-end lg:self-center">
                      <Link to={`/organizer/events/${evt.id}/participants`}>
                        <Button variant="secondary" size="sm" icon={Users}>
                          Participants
                        </Button>
                      </Link>

                      <Link to={`/organizer/events/${evt.id}/attendance`}>
                        <Button variant="primary" size="sm" icon={QrCode}>
                          Attendance
                        </Button>
                      </Link>

                      <Link to={`/organizer/events/${evt.id}/edit`}>
                        <Button variant="outline" size="sm">
                          Edit
                        </Button>
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* QR Code Presentation Dialog */}
      {activeSessionEvent && (
        <QrSessionModal
          isOpen={!!activeSessionEvent}
          onClose={() => setActiveSessionEvent(null)}
          event={activeSessionEvent}
          token={activeSessionToken}
          onSessionEnd={() => fetchDashboardData()}
        />
      )}
    </DashboardLayout>
  );
}
