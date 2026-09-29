import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Calendar,
  Clock,
  MapPin,
  Users,
  QrCode,
  Edit,
  Trash2,
  CalendarPlus,
  Search,
  ExternalLink
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import { useToast } from '../../context/ToastContext';

export default function MyEventsPage() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const navigate = useNavigate();
  const toast = useToast();

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const res = await api.get('/organizer/events');
      setEvents(res.data || []);
    } catch (err) {
      toast.error('Failed to load events');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  const handleCancelEvent = async (id, title) => {
    if (!window.confirm(`Are you sure you want to cancel the event "${title}"? All registered students will be notified.`)) {
      return;
    }

    try {
      await api.delete(`/organizer/events/${id}`);
      toast.success('Event cancelled and registered students notified.');
      fetchEvents();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel event');
    }
  };

  const filteredEvents = events.filter((e) => {
    const matchesSearch = !searchQuery.trim() ||
      e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.venue?.name?.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus = statusFilter === 'ALL' || e.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white">Managed Events Catalog</h1>
            <p className="text-xs text-slate-400">
              Overview of all departmental events, capacity ceilings, and active attendance sessions
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

        {/* Search & Filter Bar */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by event title or venue..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 text-slate-200 text-xs rounded-xl px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="PUBLISHED">Published</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        {loading ? (
          <LoadingSpinner message="Loading events..." />
        ) : filteredEvents.length === 0 ? (
          <EmptyState
            title="No events found"
            description="No events match your current filter settings."
            actionLabel="Create Event"
            onAction={() => navigate('/organizer/events/create')}
          />
        ) : (
          <div className="space-y-4">
            {filteredEvents.map((evt) => {
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
                      {evt.attendanceActive && (
                        <span className="text-[10px] font-bold text-emerald-400 bg-emerald-950/80 border border-emerald-700/60 px-2 py-0.5 rounded-full animate-pulse">
                          QR Session Open
                        </span>
                      )}
                    </div>

                    <h3 className="text-base font-bold text-white truncate">{evt.title}</h3>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-400">
                      <div className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-indigo-400" />
                        <span>{evt.eventDate} ({evt.startTime} - {evt.endTime})</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <MapPin className="w-3.5 h-3.5 text-indigo-400" />
                        <span className="truncate">{evt.venue?.name}</span>
                      </div>
                    </div>

                    <div className="text-[11px] text-slate-400">
                      <span>Eligible: </span>
                      <strong className="text-slate-300">
                        {evt.eligibleBranches || 'All Branches'}
                      </strong>
                      {evt.eligibleAcademicYears && <span> • {evt.eligibleAcademicYears}</span>}
                    </div>
                  </div>

                  {/* Seat Capacity Progress */}
                  <div className="w-full lg:w-56 space-y-1">
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400">Capacity:</span>
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
                      {evt.remainingSeats} remaining seats
                    </span>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex flex-wrap items-center gap-2 shrink-0 self-end lg:self-center">
                    <Link to={`/organizer/events/${evt.id}/participants`}>
                      <Button variant="secondary" size="sm" icon={Users}>
                        Participants ({evt.registeredCount})
                      </Button>
                    </Link>

                    <Link to={`/organizer/events/${evt.id}/attendance`}>
                      <Button variant="primary" size="sm" icon={QrCode}>
                        Attendance
                      </Button>
                    </Link>

                    <Link to={`/organizer/events/${evt.id}/edit`}>
                      <Button variant="outline" size="sm" icon={Edit}>
                        Edit
                      </Button>
                    </Link>

                    {evt.status !== 'CANCELLED' && (
                      <Button
                        variant="danger"
                        size="sm"
                        icon={Trash2}
                        onClick={() => handleCancelEvent(evt.id, evt.title)}
                      >
                        Cancel
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
