import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Calendar,
  CalendarCheck,
  CheckCircle2,
  Clock,
  MapPin,
  QrCode,
  ArrowRight,
  GraduationCap,
  Sparkles,
  Award
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import QrScannerModal from '../../components/qr/QrScannerModal';
import { useAuth } from '../../context/AuthContext';

export default function StudentDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [registrations, setRegistrations] = useState([]);
  const [upcomingEvents, setUpcomingEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedEventForScan, setSelectedEventForScan] = useState(null);
  const navigate = useNavigate();

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const [statsRes, regRes, eventsRes] = await Promise.all([
        api.get('/student/dashboard'),
        api.get('/student/registrations'),
        api.get('/events')
      ]);
      setStats(statsRes.data);
      setRegistrations(regRes.data || []);
      setUpcomingEvents(eventsRes.data?.slice(0, 3) || []);
    } catch (err) {
      console.error('Failed to load student dashboard:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Welcome Banner */}
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-indigo-900/60 via-slate-900 to-indigo-950/40 border border-indigo-800/40 p-6 sm:p-8 shadow-2xl">
          <div className="relative z-10 max-w-2xl space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 text-xs font-semibold">
              <Sparkles className="w-3.5 h-3.5" />
              Verified Student Account
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Welcome back, {user?.name || 'Student'}!
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
              Enrollment ID: <span className="font-mono font-bold text-white">{user?.enrollmentId}</span> • Branch:{' '}
              <span className="text-indigo-300 font-semibold">{user?.branch}</span>. Explore eligible campus events and verify attendance.
            </p>
          </div>
        </div>

        {/* Quick KPI Stat Cards */}
        {loading ? (
          <LoadingSpinner message="Calculating dashboard statistics..." />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
            <Card hover className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-2xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center">
                <CalendarCheck className="w-6 h-6" />
              </div>
              <div>
                <p className="text-xs text-slate-400 font-medium">Registered Events</p>
                <h3 className="text-2xl font-bold text-white mt-0.5">{stats?.registeredEventsCount || 0}</h3>
              </div>
            </Card>

            <Card hover className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-2xl bg-emerald-600/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center">
                <Award className="w-6 h-6" />
              </div>
              <div>
                <p className="text-xs text-slate-400 font-medium">Events Attended</p>
                <h3 className="text-2xl font-bold text-white mt-0.5">{stats?.attendedEventsCount || 0}</h3>
              </div>
            </Card>

            <Card hover className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-2xl bg-purple-600/20 text-purple-400 border border-purple-500/30 flex items-center justify-center">
                <Calendar className="w-6 h-6" />
              </div>
              <div>
                <p className="text-xs text-slate-400 font-medium">Available Events</p>
                <h3 className="text-2xl font-bold text-white mt-0.5">{stats?.availableEventsCount || 0}</h3>
              </div>
            </Card>
          </div>
        )}

        {/* My Registered Events Strip */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-white">My Registered Events</h2>
              <p className="text-xs text-slate-400">Events you have signed up to participate in</p>
            </div>
            <Link to="/student/registrations" className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1">
              View all ({registrations.length}) <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {registrations.length === 0 ? (
            <EmptyState
              title="No event registrations yet"
              description="Browse upcoming college events and reserve your seat today."
              actionLabel="Explore Events"
              onAction={() => navigate('/student/events')}
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {registrations.slice(0, 4).map((reg) => (
                <div
                  key={reg.id}
                  className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between space-y-4 hover:border-slate-700 transition-all"
                >
                  <div className="space-y-2">
                    <div className="flex items-start justify-between gap-2">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-indigo-950 text-indigo-300 border border-indigo-800">
                        {reg.category}
                      </span>
                      <Badge variant={reg.status === 'ATTENDED' ? 'success' : 'default'} size="sm">
                        {reg.status}
                      </Badge>
                    </div>

                    <h3 className="text-sm font-bold text-white line-clamp-1">{reg.eventTitle}</h3>

                    <div className="space-y-1 text-xs text-slate-400">
                      <div className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-indigo-400" />
                        <span>{reg.eventDate} • {reg.startTime}</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <MapPin className="w-3.5 h-3.5 text-indigo-400" />
                        <span>{reg.venueName}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-800/80">
                    <span className="text-[11px] text-slate-500">
                      Check-in: <strong className={reg.attendanceStatus === 'PRESENT' ? 'text-emerald-400' : 'text-slate-400'}>{reg.attendanceStatus || 'PENDING'}</strong>
                    </span>
                    <Button
                      variant="primary"
                      size="sm"
                      icon={QrCode}
                      onClick={() => {
                        setSelectedEventForScan({ id: reg.eventId, title: reg.eventTitle, venue: { name: reg.venueName } });
                      }}
                    >
                      Scan QR Check-in
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Upcoming Campus Events Recommendations */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-white">Recommended Upcoming Events</h2>
              <p className="text-xs text-slate-400">Trending campus workshops and seminars</p>
            </div>
            <Link to="/student/events" className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1">
              Browse all <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {upcomingEvents.map((e) => (
              <div
                key={e.id}
                className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden flex flex-col justify-between hover:border-slate-700 transition-all p-4 space-y-3"
              >
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-[10px] font-bold text-indigo-300 px-2 py-0.5 rounded bg-indigo-950 border border-indigo-800/60">
                      {e.category}
                    </span>
                    <Badge variant={e.eligible ? 'success' : 'danger'} size="sm">
                      {e.eligible ? 'Eligible' : 'Not Eligible'}
                    </Badge>
                  </div>
                  <h4 className="text-sm font-bold text-white line-clamp-1">{e.title}</h4>
                  <p className="text-xs text-slate-400 line-clamp-2 mt-1">{e.description}</p>
                </div>

                <div className="pt-2 border-t border-slate-800 text-xs text-slate-400 space-y-1">
                  <p>{e.eventDate} at {e.startTime}</p>
                  <p className="truncate text-slate-300">{e.venue?.name}</p>
                </div>

                <Link to={`/events/${e.id}`}>
                  <Button variant="secondary" size="sm" className="w-full">
                    View & Register
                  </Button>
                </Link>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* QR Code Scanner Dialog */}
      {selectedEventForScan && (
        <QrScannerModal
          isOpen={!!selectedEventForScan}
          onClose={() => setSelectedEventForScan(null)}
          event={selectedEventForScan}
          onSuccess={() => loadDashboardData()}
        />
      )}
    </DashboardLayout>
  );
}
