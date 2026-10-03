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
import EventDateBlock from '../../components/common/EventDateBlock';
import { formatDate, formatTime, formatDateTime } from '../../utils/dateUtils';
import { useAuth } from '../../context/AuthContext';

export default function StudentDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [registrations, setRegistrations] = useState([]);
  const [upcomingEvents, setUpcomingEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedEventForScan, setSelectedEventForScan] = useState(null);
  const navigate = useNavigate();

  const isCompletedEvent = (e) => {
    if (e.status === 'COMPLETED' || e.status === 'CANCELLED') return true;
    if (!e.eventDate) return false;
    const now = new Date();
    const eventEnd = new Date(`${e.eventDate}T${e.endTime || '23:59:59'}`);
    return eventEnd < now;
  };

  const sortRegistrations = (list) => {
    const now = Date.now();
    return [...list].sort((a, b) => {
      const getRank = (reg) => {
        const isPresent = reg.attendanceStatus === 'PRESENT' || reg.status === 'ATTENDED' || reg.attendanceState === 'PRESENT';
        const start = new Date(`${reg.eventDate}T${reg.startTime || '00:00:00'}`).getTime();
        const end = new Date(`${reg.eventDate}T${reg.endTime || '23:59:59'}`).getTime();
        const graceEnd = end + 24 * 3600 * 1000;

        // 1. CHECK-IN OPEN (Window open and not marked PRESENT)
        if (!isPresent && (reg.attendanceState === 'CHECK_IN_OPEN' || (now >= start && now <= graceEnd))) {
          return 1;
        }
        // 2. UPCOMING (Future start time, not marked present)
        if (!isPresent && (reg.attendanceState === 'NOT_STARTED' || now < start)) {
          return 2;
        }
        // 3. RECENTLY COMPLETED (Attended or completed within past 7 days)
        const sevenDaysAgo = now - 7 * 24 * 3600 * 1000;
        if (end >= sevenDaysAgo || isPresent) {
          return 3;
        }
        // 4. OLDER COMPLETED
        return 4;
      };

      const rankA = getRank(a);
      const rankB = getRank(b);

      if (rankA !== rankB) {
        return rankA - rankB;
      }

      // Within rank 1 (CHECK-IN OPEN): order by start time ascending
      if (rankA === 1) {
        return new Date(`${a.eventDate}T${a.startTime || '00:00:00'}`) - new Date(`${b.eventDate}T${b.startTime || '00:00:00'}`);
      }
      // Within rank 2 (UPCOMING): chronological by start time
      if (rankA === 2) {
        return new Date(`${a.eventDate}T${a.startTime || '00:00:00'}`) - new Date(`${b.eventDate}T${b.startTime || '00:00:00'}`);
      }
      // Within completed (ranks 3 and 4): reverse chronological by end time
      return new Date(`${b.eventDate}T${b.endTime || '23:59:59'}`) - new Date(`${a.eventDate}T${a.endTime || '23:59:59'}`);
    });
  };

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      let recs = [];
      const [statsRes, regRes] = await Promise.all([
        api.get('/student/dashboard'),
        api.get('/student/registrations')
      ]);
      setStats(statsRes.data);
      const sortedRegs = sortRegistrations(regRes.data || []);
      setRegistrations(sortedRegs);

      try {
        const recRes = await api.get('/events/recommendations');
        recs = recRes.data || [];
      } catch {
        // Fallback to discovery endpoint
        const eventsRes = await api.get('/events');
        const regIds = new Set((regRes.data || []).map(r => r.eventId));
        recs = (eventsRes.data || []).filter(e => !regIds.has(e.id) && !isCompletedEvent(e));
      }

      // Defensive filtering for recommendations: exclude registered, past/completed, and ineligible
      const regEventIds = new Set((regRes.data || []).map(r => r.eventId));
      const filteredRecs = recs
        .filter(e => !regEventIds.has(e.id) && !e.registered && !isCompletedEvent(e))
        .slice(0, 3);
      setUpcomingEvents(filteredRecs);
    } catch (err) {
      console.error('Failed to load student dashboard:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  const getAttendanceBadge = (reg) => {
    const isPresent = reg.attendanceStatus === 'PRESENT' || reg.status === 'ATTENDED' || reg.attendanceState === 'PRESENT';
    if (isPresent) {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-950/90 text-emerald-400 border border-emerald-700/60 inline-flex items-center gap-1">
          PRESENT ✓
        </span>
      );
    }

    const now = Date.now();
    const start = new Date(`${reg.eventDate}T${reg.startTime || '00:00:00'}`).getTime();
    const end = new Date(`${reg.eventDate}T${reg.endTime || '23:59:59'}`).getTime();
    const graceEnd = end + 24 * 3600 * 1000;

    if (reg.attendanceState === 'CHECK_IN_OPEN' || (now >= start && now <= graceEnd)) {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-950/90 text-amber-300 border border-amber-600/70 inline-flex items-center gap-1 animate-pulse">
          CHECK-IN OPEN
        </span>
      );
    }

    if (reg.attendanceState === 'NOT_STARTED' || now < start) {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-950/90 text-blue-300 border border-blue-700/60 inline-flex items-center gap-1">
          UPCOMING
        </span>
      );
    }

    return (
      <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-950/90 text-rose-400 border border-rose-700/60 inline-flex items-center gap-1">
        ABSENT ✕
      </span>
    );
  };

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
              {registrations.slice(0, 4).map((reg) => {
                const isPresent = reg.attendanceStatus === 'PRESENT' || reg.status === 'ATTENDED' || reg.attendanceState === 'PRESENT';
                const now = Date.now();
                const start = new Date(`${reg.eventDate}T${reg.startTime || '00:00:00'}`).getTime();
                const end = new Date(`${reg.eventDate}T${reg.endTime || '23:59:59'}`).getTime();
                const graceEnd = end + 24 * 3600 * 1000;
                const isCheckInOpen = !isPresent && (reg.attendanceState === 'CHECK_IN_OPEN' || (now >= start && now <= graceEnd));
                const isUpcoming = !isPresent && (reg.attendanceState === 'NOT_STARTED' || now < start);

                return (
                  <div
                    key={reg.id}
                    className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between space-y-4 hover:border-slate-700 transition-all"
                  >
                    <div className="flex items-start gap-3.5">
                      <EventDateBlock date={reg.eventDate} />
                      <div className="flex-1 min-w-0 space-y-1.5">
                        <div className="flex items-center justify-between gap-2">
                          <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-indigo-950 text-indigo-300 border border-indigo-800">
                            {reg.category}
                          </span>
                          {getAttendanceBadge(reg)}
                        </div>

                        <h3 className="text-sm font-bold text-white line-clamp-1">{reg.eventTitle}</h3>

                        <div className="space-y-0.5 text-xs text-slate-400">
                          <div className="flex items-center gap-1.5">
                            <Clock className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                            <span>{formatTime(reg.startTime)}</span>
                          </div>
                          <div className="flex items-center gap-1.5">
                            <MapPin className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                            <span className="truncate">{reg.venueName}</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-3 border-t border-slate-800/80">
                      <div className="text-[11px]">
                        {isPresent ? (
                          <span className="text-emerald-400 font-semibold flex items-center gap-1">
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                            Attendance Verified
                          </span>
                        ) : isCheckInOpen ? (
                          <span className="text-amber-400 font-medium">
                            Check-in open now
                          </span>
                        ) : isUpcoming ? (
                          <span className="text-slate-400">
                            Check-in opens: {formatDateTime(reg.eventDate, reg.startTime)}
                          </span>
                        ) : (
                          <span className="text-slate-500">
                            Check-in closed
                          </span>
                        )}
                      </div>

                      {/* QR Check-in action ONLY shown when check-in is open and student is ABSENT */}
                      {isCheckInOpen && (
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
                      )}
                    </div>
                  </div>
                );
              })}
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
                <div className="flex items-start gap-3">
                  <EventDateBlock date={e.eventDate} size="sm" />
                  <div className="flex-1 min-w-0 space-y-1">
                    <div className="flex justify-between items-center">
                      <span className="text-[10px] font-bold text-indigo-300 px-2 py-0.5 rounded bg-indigo-950 border border-indigo-800/60">
                        {e.category}
                      </span>
                      <Badge variant={e.eligible ? 'success' : 'danger'} size="sm">
                        {e.eligible ? 'Eligible' : 'Not Eligible'}
                      </Badge>
                    </div>
                    <h4 className="text-sm font-bold text-white line-clamp-1">{e.title}</h4>
                    <p className="text-xs text-slate-400 line-clamp-1 mt-0.5">{e.description}</p>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-800 text-xs text-slate-400 space-y-1">
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                    <span>{formatTime(e.startTime)}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <MapPin className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                    <span className="truncate text-slate-300">{e.venue?.name}</span>
                  </div>
                </div>

                <Link to={`/events/${e.id}`}>
                  <Button variant="secondary" size="sm" className="w-full">
                    {e.registered ? 'REGISTERED' : 'VIEW EVENT'}
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
