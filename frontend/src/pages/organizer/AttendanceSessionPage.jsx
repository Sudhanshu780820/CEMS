import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import {
  ArrowLeft,
  QrCode,
  Power,
  PowerOff,
  Users,
  CheckCircle2,
  XCircle,
  Copy,
  Check,
  RefreshCw,
  CheckCheck
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../context/ToastContext';

export default function AttendanceSessionPage() {
  const { id } = useParams();
  const [event, setEvent] = useState(null);
  const [stats, setStats] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [token, setToken] = useState(null);
  const [isActive, setIsActive] = useState(false);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState(false);
  const [copied, setCopied] = useState(false);
  const toast = useToast();

  const loadSessionData = async () => {
    try {
      const [evtRes, statsRes, partRes] = await Promise.all([
        api.get(`/events/${id}`),
        api.get(`/organizer/events/${id}/attendance/stats`),
        api.get(`/organizer/events/${id}/participants`)
      ]);
      setEvent(evtRes.data);
      setStats(statsRes.data);
      setParticipants(partRes.data || []);
      setIsActive(evtRes.data.attendanceActive);
      setToken(evtRes.data.attendanceToken);
    } catch (err) {
      toast.error('Failed to load session details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSessionData();
    const interval = setInterval(loadSessionData, 5000); // 5s live polling for check-ins!
    return () => clearInterval(interval);
  }, [id]);

  const handleStartSession = async () => {
    setToggling(true);
    try {
      const res = await api.post(`/organizer/events/${id}/attendance/start`);
      setToken(res.data.attendanceToken);
      setIsActive(true);
      toast.success('Attendance session started! QR Code active.');
      loadSessionData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to start session');
    } finally {
      setToggling(false);
    }
  };

  const handleStopSession = async () => {
    setToggling(true);
    try {
      await api.post(`/organizer/events/${id}/attendance/stop`);
      setIsActive(false);
      toast.info('Attendance session closed.');
      loadSessionData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to stop session');
    } finally {
      setToggling(false);
    }
  };

  const handleCopyToken = () => {
    if (!token) return;
    navigator.clipboard.writeText(token);
    setCopied(true);
    toast.info('Attendance token copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleMarkAllPresent = async () => {
    const studentIds = participants.map((p) => p.studentId);
    if (studentIds.length === 0) return;

    try {
      await api.post(`/organizer/events/${id}/attendance/manual`, {
        studentIds,
        status: 'PRESENT',
      });
      toast.success('All registered students marked PRESENT');
      loadSessionData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to mark all present');
    }
  };

  if (loading) {
    return (
      <DashboardLayout>
        <LoadingSpinner message="Opening live attendance console..." />
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <Link
          to={`/organizer/events/${id}/participants`}
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to participant roster
        </Link>

        {/* Console Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className={`w-2.5 h-2.5 rounded-full ${isActive ? 'bg-emerald-400 animate-ping' : event?.attendanceState === 'CHECK_IN_OPEN' ? 'bg-amber-400' : 'bg-slate-600'}`} />
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                {isActive
                  ? 'Session Live'
                  : event?.attendanceState === 'CHECK_IN_OPEN'
                  ? 'Window Open (QR Offline)'
                  : event?.attendanceState === 'NOT_STARTED'
                  ? 'Check-in Not Started'
                  : 'Session Offline'}
              </span>
            </div>
            <h1 className="text-xl sm:text-2xl font-bold text-white">{event?.title}</h1>
            <p className="text-xs text-slate-400 mt-0.5">
              {event?.eventDate} • {event?.venue?.name}
            </p>
          </div>

          <div className="flex items-center gap-2">
            {isActive ? (
              <Button
                variant="danger"
                icon={PowerOff}
                loading={toggling}
                onClick={handleStopSession}
              >
                Close Session
              </Button>
            ) : (
              <Button
                variant="success"
                icon={Power}
                loading={toggling}
                onClick={handleStartSession}
              >
                Start Attendance Session
              </Button>
            )}

            <Button
              variant="secondary"
              icon={CheckCheck}
              onClick={handleMarkAllPresent}
              disabled={participants.length === 0}
            >
              Mark All Present
            </Button>
          </div>
        </div>

        {/* Live Attendance Statistics Bar */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="text-center p-4">
            <span className="text-xs text-slate-400 font-medium">Total Registered</span>
            <p className="text-2xl font-bold text-white mt-1">{stats?.totalRegistered || 0}</p>
          </Card>

          <Card className="text-center p-4">
            <span className="text-xs text-slate-400 font-medium">Present (Attended)</span>
            <p className="text-2xl font-bold text-emerald-400 mt-1">{stats?.presentCount || 0}</p>
          </Card>

          <Card className="text-center p-4">
            <span className="text-xs text-slate-400 font-medium">Absent / Pending</span>
            <p className="text-2xl font-bold text-rose-400 mt-1">{stats?.absentCount || 0}</p>
          </Card>

          <Card className="text-center p-4">
            <span className="text-xs text-slate-400 font-medium">Attendance Rate</span>
            <p className="text-2xl font-bold text-indigo-400 mt-1">{stats?.attendancePercentage || 0}%</p>
          </Card>
        </div>

        {/* Main Console: QR Projection & Live Feed */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Left: QR Display Card */}
          <Card className="flex flex-col items-center justify-center text-center p-8 space-y-5">
            <h3 className="text-base font-bold text-white">Attendee Check-in QR Code</h3>

            {isActive ? (
              <div className="space-y-4 flex flex-col items-center">
                <div className="p-4 bg-white rounded-3xl shadow-2xl shadow-indigo-500/10 border-4 border-indigo-500/40">
                  <QRCodeSVG
                    value={token || 'NO_TOKEN'}
                    size={240}
                    level="H"
                    includeMargin={true}
                  />
                </div>

                <p className="text-xs text-slate-400 max-w-sm">
                  Project this code onto the auditorium screen. Students open their student portal and scan to verify attendance immediately.
                </p>

                {/* Token string with copy */}
                <div className="w-full flex items-center justify-between p-3 bg-slate-950 rounded-xl border border-slate-800 text-xs font-mono text-slate-300">
                  <span className="truncate max-w-[260px]">{token}</span>
                  <button
                    onClick={handleCopyToken}
                    className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white"
                  >
                    {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>
              </div>
            ) : (
              <div className="py-12 space-y-4">
                <div className="w-16 h-16 rounded-2xl bg-slate-800 text-slate-500 flex items-center justify-center mx-auto">
                  <PowerOff className="w-8 h-8" />
                </div>
                <p className="text-sm text-slate-300 font-semibold">Attendance Session Offline</p>
                <p className="text-xs text-slate-400 max-w-xs">
                  {event?.attendanceState === 'NOT_STARTED'
                    ? `Check-in opens when the event starts (${event?.startTime || 'Start time'}).`
                    : event?.attendanceState === 'CHECK_IN_CLOSED'
                    ? 'The 24-hour attendance window has ended.'
                    : "Click 'Start Attendance Session' above to generate a live QR code and accept student check-ins."}
                </p>
                <Button variant="primary" icon={Power} onClick={handleStartSession} loading={toggling}>
                  Activate Live Session
                </Button>
              </div>
            )}
          </Card>

          {/* Right: Real-time Check-in Stream */}
          <Card className="space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <Users className="w-4 h-4 text-indigo-400" />
                <h3 className="text-sm font-bold text-white">Live Attendee Roster</h3>
              </div>
              <span className="text-[11px] text-slate-500 font-mono">Auto-refreshes 5s</span>
            </div>

            <div className="max-h-96 overflow-y-auto divide-y divide-slate-800/60 pr-1">
              {participants.length === 0 ? (
                <div className="py-12 text-center text-xs text-slate-500">
                  No students registered for this event yet.
                </div>
              ) : (
                participants.map((p) => {
                  const isPresent = p.attendanceStatus === 'PRESENT';
                  return (
                    <div key={p.id} className="py-3 flex items-center justify-between gap-3">
                      <div>
                        <p className="text-xs font-bold text-white">{p.studentName}</p>
                        <p className="text-[11px] text-slate-400 font-mono">
                          {p.enrollmentId} • {p.branch} (Sec {p.section})
                        </p>
                      </div>

                      <div className="flex items-center gap-2">
                        <Badge variant={isPresent ? 'success' : 'default'} size="sm">
                          {isPresent ? 'PRESENT' : 'ABSENT'}
                        </Badge>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
}
