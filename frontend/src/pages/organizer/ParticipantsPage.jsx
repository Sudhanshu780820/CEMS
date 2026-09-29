import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Users,
  Download,
  Search,
  CheckCircle2,
  XCircle,
  QrCode,
  CheckCheck,
  UserCheck
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import { useToast } from '../../context/ToastContext';

export default function ParticipantsPage() {
  const { id } = useParams();
  const [event, setEvent] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [branchFilter, setBranchFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [markingAll, setMarkingAll] = useState(false);
  const toast = useToast();

  const loadData = async () => {
    setLoading(true);
    try {
      const [evtRes, partRes] = await Promise.all([
        api.get(`/events/${id}`),
        api.get(`/organizer/events/${id}/participants`)
      ]);
      setEvent(evtRes.data);
      setParticipants(partRes.data || []);
    } catch (err) {
      toast.error('Failed to load participants roster');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id]);

  const handleExportCsv = async () => {
    try {
      const res = await api.get(`/organizer/events/${id}/participants/export`, {
        responseType: 'blob',
      });
      const blob = new Blob([res.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `participants-event-${id}.csv`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      toast.success('Participant roster exported to CSV!');
    } catch (err) {
      toast.error('Failed to export CSV');
    }
  };

  const handleManualToggle = async (studentId, newStatus) => {
    try {
      await api.post(`/organizer/events/${id}/attendance/manual`, {
        studentIds: [studentId],
        status: newStatus,
      });
      toast.success(`Marked as ${newStatus}`);
      loadData();
    } catch (err) {
      toast.error('Failed to update attendance');
    }
  };

  const handleMarkAllPresent = async () => {
    const studentIds = participants.map((p) => p.studentId);
    if (studentIds.length === 0) return;

    setMarkingAll(true);
    try {
      await api.post(`/organizer/events/${id}/attendance/manual`, {
        studentIds,
        status: 'PRESENT',
      });
      toast.success(`All ${studentIds.length} registered students marked PRESENT`);
      loadData();
    } catch (err) {
      toast.error('Failed to update batch attendance');
    } finally {
      setMarkingAll(false);
    }
  };

  const branches = ['ALL', 'CSE', 'IT', 'AI/ML', 'ECE', 'ME', 'Civil'];

  const filteredParticipants = participants.filter((p) => {
    const matchesSearch = !searchQuery.trim() ||
      p.studentName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.enrollmentId.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesBranch = branchFilter === 'ALL' || p.branch === branchFilter;
    const matchesStatus = statusFilter === 'ALL' || p.attendanceStatus === statusFilter;

    return matchesSearch && matchesBranch && matchesStatus;
  });

  const presentCount = participants.filter((p) => p.attendanceStatus === 'PRESENT').length;
  const attendanceRate = participants.length > 0
    ? Math.round((presentCount / participants.length) * 100)
    : 0;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <Link
          to="/organizer/events"
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to events
        </Link>

        {/* Header Summary */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <span className="text-[11px] font-bold text-indigo-400 uppercase tracking-wider">
              Participant Roster
            </span>
            <h1 className="text-xl sm:text-2xl font-bold text-white">{event?.title || 'Event'}</h1>
            <p className="text-xs text-slate-400 mt-0.5">
              {event?.eventDate} • {event?.venue?.name} • Registered: {participants.length} / {event?.maxCapacity}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              icon={Download}
              onClick={handleExportCsv}
              disabled={participants.length === 0}
            >
              Export CSV
            </Button>

            <Button
              variant="success"
              size="sm"
              icon={CheckCheck}
              loading={markingAll}
              onClick={handleMarkAllPresent}
              disabled={participants.length === 0}
            >
              Mark All Present
            </Button>

            <Link to={`/organizer/events/${id}/attendance`}>
              <Button variant="primary" size="sm" icon={QrCode}>
                Live QR Console
              </Button>
            </Link>
          </div>
        </div>

        {/* KPI Mini-Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
          <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
            <span className="text-slate-400">Total Registered</span>
            <p className="text-lg font-bold text-white mt-0.5">{participants.length}</p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
            <span className="text-slate-400">Present (Attended)</span>
            <p className="text-lg font-bold text-emerald-400 mt-0.5">{presentCount}</p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
            <span className="text-slate-400">Absent</span>
            <p className="text-lg font-bold text-rose-400 mt-0.5">{participants.length - presentCount}</p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
            <span className="text-slate-400">Attendance Rate</span>
            <p className="text-lg font-bold text-indigo-400 mt-0.5">{attendanceRate}%</p>
          </div>
        </div>

        {/* Filter Controls */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search student by name or enrollment ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <select
            value={branchFilter}
            onChange={(e) => setBranchFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 text-slate-200 text-xs rounded-xl px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {branches.map((b) => (
              <option key={b} value={b} className="bg-slate-900">
                {b === 'ALL' ? 'All Branches' : b}
              </option>
            ))}
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 text-slate-200 text-xs rounded-xl px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">All Attendance</option>
            <option value="PRESENT">Present</option>
            <option value="ABSENT">Absent / Pending</option>
          </select>
        </div>

        {/* Participants Table */}
        {loading ? (
          <LoadingSpinner message="Loading participant roster..." />
        ) : filteredParticipants.length === 0 ? (
          <EmptyState
            icon={Users}
            title="No participants found"
            description="No registered students match your current search or filters."
          />
        ) : (
          <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/80 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                  <tr>
                    <th className="px-5 py-3.5">Student Name</th>
                    <th className="px-5 py-3.5">Enrollment ID</th>
                    <th className="px-5 py-3.5">Branch / Sec</th>
                    <th className="px-5 py-3.5">Registration Date</th>
                    <th className="px-5 py-3.5">Registration</th>
                    <th className="px-5 py-3.5">Attendance</th>
                    <th className="px-5 py-3.5 text-right">Manual Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {filteredParticipants.map((p) => {
                    const isPresent = p.attendanceStatus === 'PRESENT';

                    return (
                      <tr key={p.id} className="hover:bg-slate-800/40 transition-colors">
                        <td className="px-5 py-3.5 font-bold text-white">{p.studentName}</td>
                        <td className="px-5 py-3.5 font-mono text-indigo-300">{p.enrollmentId}</td>
                        <td className="px-5 py-3.5">{p.branch} (Sec {p.section})</td>
                        <td className="px-5 py-3.5 text-slate-400">
                          {new Date(p.registeredAt).toLocaleDateString()}
                        </td>
                        <td className="px-5 py-3.5">
                          <Badge variant="default" size="sm">{p.status}</Badge>
                        </td>
                        <td className="px-5 py-3.5">
                          <Badge variant={isPresent ? 'success' : 'danger'} size="sm">
                            {isPresent ? 'PRESENT' : 'ABSENT'}
                          </Badge>
                        </td>
                        <td className="px-5 py-3.5 text-right">
                          {isPresent ? (
                            <button
                              onClick={() => handleManualToggle(p.studentId, 'ABSENT')}
                              className="text-xs text-rose-400 hover:text-rose-300 font-semibold px-2 py-1 rounded bg-rose-950/40 border border-rose-800/60 cursor-pointer"
                            >
                              Mark Absent
                            </button>
                          ) : (
                            <button
                              onClick={() => handleManualToggle(p.studentId, 'PRESENT')}
                              className="text-xs text-emerald-400 hover:text-emerald-300 font-semibold px-2 py-1 rounded bg-emerald-950/40 border border-emerald-800/60 cursor-pointer"
                            >
                              Mark Present
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
