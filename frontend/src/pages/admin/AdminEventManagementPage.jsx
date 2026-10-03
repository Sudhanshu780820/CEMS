import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Clock, MapPin, Users, Search, Trash2, Edit } from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import { useToast } from '../../context/ToastContext';

export default function AdminEventManagementPage() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const toast = useToast();

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const res = await api.get('/events');
      setEvents(res.data || []);
    } catch (err) {
      toast.error('Failed to load events catalog');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  const handleCancel = async (id, title) => {
    if (!window.confirm(`Are you sure you want to administratively cancel "${title}"?`)) {
      return;
    }

    try {
      await api.delete(`/organizer/events/${id}`);
      toast.success('Event cancelled');
      fetchEvents();
    } catch (err) {
      toast.error('Failed to cancel event');
    }
  };

  const filteredEvents = events.filter((e) =>
    e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    e.venue?.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    e.organizer?.name?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">All Institutional Events</h1>
          <p className="text-xs text-slate-400">
            Monitor capacity, registration percentages, and attendance across departments
          </p>
        </div>

        <div className="relative">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by event title, organizer, or venue..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        {loading ? (
          <LoadingSpinner message="Loading all campus events..." />
        ) : filteredEvents.length === 0 ? (
          <EmptyState title="No events found" />
        ) : (
          <div className="space-y-4">
            {filteredEvents.map((evt) => {
              const capacityPct = Math.min(100, Math.round((evt.registeredCount / evt.maxCapacity) * 100));

              return (
                <div
                  key={evt.id}
                  className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col lg:flex-row lg:items-center justify-between gap-4 hover:border-slate-700 transition-all"
                >
                  <div className="space-y-1.5 flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] font-bold text-indigo-300 px-2 py-0.5 rounded bg-indigo-950 border border-indigo-800/60">
                        {evt.category}
                      </span>
                      {evt.hasVenueConflict || evt.status === 'PENDING_APPROVAL' ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-950 text-rose-300 border border-rose-700/60">
                          VENUE CONFLICT
                        </span>
                      ) : (
                        <Badge variant="default" size="sm">{evt.status}</Badge>
                      )}
                      <span className="text-xs text-slate-400">By: {evt.organizer?.name}</span>
                    </div>

                    <Link to={`/events/${evt.id}`} className="hover:text-indigo-400 transition-colors block">
                      <h3 className="text-base font-bold text-white truncate">{evt.title}</h3>
                    </Link>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-400">
                      <span>{evt.eventDate} ({evt.startTime} - {evt.endTime})</span>
                      <span>•</span>
                      <span>{evt.venue?.name}</span>
                    </div>
                  </div>

                  <div className="w-full lg:w-56 space-y-1">
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400">Booked Seats</span>
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
                  </div>

                  <div className="flex items-center gap-2 shrink-0 self-end lg:self-center">
                    <Link to={`/organizer/events/${evt.id}/participants`}>
                      <Button variant="secondary" size="sm" icon={Users}>
                        Roster
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
                        onClick={() => handleCancel(evt.id, evt.title)}
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
