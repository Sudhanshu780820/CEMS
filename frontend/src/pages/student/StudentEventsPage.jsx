import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  Clock,
  MapPin,
  Search,
  Filter,
  Users,
  ChevronRight,
  Sparkles,
  CheckCircle2,
  XCircle
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import { useAuth } from '../../context/AuthContext';

export default function StudentEventsPage() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [eligibilityOnly, setEligibilityOnly] = useState(false);
  const { user } = useAuth();

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const res = await api.get('/events');
      setEvents(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  const categories = ['ALL', 'Technical', 'Workshop', 'Cultural', 'Seminar', 'Sports'];

  const isCompletedEvent = (e) => {
    if (e.status === 'COMPLETED' || e.status === 'CANCELLED') return true;
    if (!e.eventDate) return false;
    const now = new Date();
    const eventEnd = new Date(`${e.eventDate}T${e.endTime || '23:59:59'}`);
    return eventEnd < now;
  };

  const filteredEvents = events.filter((e) => {
    if (isCompletedEvent(e)) return false;

    const matchesSearch = !searchQuery.trim() ||
      e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.venue?.name?.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesCategory = selectedCategory === 'ALL' || e.category === selectedCategory;
    const matchesEligibility = !eligibilityOnly || e.eligible;

    return matchesSearch && matchesCategory && matchesEligibility;
  });

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header Title */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white">Campus Events Directory</h1>
            <p className="text-xs text-slate-400">
              Discover and register for eligible events matching your branch ({user?.branch})
            </p>
          </div>

          <label className="inline-flex items-center gap-2 cursor-pointer bg-slate-900 border border-slate-800 px-3 py-2 rounded-xl text-xs text-slate-300 hover:border-slate-700">
            <input
              type="checkbox"
              checked={eligibilityOnly}
              onChange={(e) => setEligibilityOnly(e.target.checked)}
              className="rounded bg-slate-950 border-slate-700 text-indigo-600 focus:ring-indigo-500 w-3.5 h-3.5"
            />
            <span>Show Eligible Events Only</span>
          </label>
        </div>

        {/* Search and Filters */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by event title, keyword, or venue..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>

          <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors ${
                  selectedCategory === cat
                    ? 'bg-indigo-600 text-white'
                    : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-white'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Events Grid */}
        {loading ? (
          <LoadingSpinner message="Fetching campus events catalog..." />
        ) : filteredEvents.length === 0 ? (
          <EmptyState
            title="No events match your criteria"
            description="Try changing your search query or unchecking 'Show Eligible Events Only'."
            actionLabel="Reset Search"
            onAction={() => {
              setSearchQuery('');
              setSelectedCategory('ALL');
              setEligibilityOnly(false);
            }}
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredEvents.map((event) => {
              const capacityPct = Math.min(100, Math.round((event.registeredCount / event.maxCapacity) * 100));

              return (
                <div
                  key={event.id}
                  className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden hover:border-slate-700 transition-all flex flex-col justify-between"
                >
                  <div className="relative h-40 w-full bg-slate-950">
                    <img
                      src={event.eventImageUrl || 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=800&q=80'}
                      alt={event.title}
                      className="w-full h-full object-cover"
                    />
                    <div className="absolute top-3 left-3">
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-900/90 text-indigo-300 backdrop-blur-md border border-indigo-500/30">
                        {event.category}
                      </span>
                    </div>

                    <div className="absolute top-3 right-3">
                      <Badge
                        variant={
                          event.actionStatus === 'REGISTER NOW' ? 'success' :
                          event.actionStatus === 'REGISTRATION NOT STARTED' ? 'warning' :
                          event.actionStatus === 'ALREADY REGISTERED' ? 'purple' : 'default'
                        }
                        size="sm"
                      >
                        {event.actionStatus}
                      </Badge>
                    </div>
                  </div>

                  <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                    <div className="space-y-1.5">
                      <h3 className="text-sm font-bold text-white line-clamp-1">{event.title}</h3>
                      <p className="text-xs text-slate-400 line-clamp-2">{event.description}</p>
                    </div>

                    {/* Meta info */}
                    <div className="text-xs text-slate-300 space-y-1.5 border-t border-slate-800/80 pt-3">
                      <div className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-indigo-400" />
                        <span>{event.eventDate} • {event.startTime}</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <MapPin className="w-3.5 h-3.5 text-indigo-400" />
                        <span className="truncate">{event.venue?.name}</span>
                      </div>
                    </div>

                    {/* Registration window info if NOT_STARTED */}
                    {event.registrationState === 'NOT_STARTED' && (
                      <div className="p-2.5 bg-amber-950/40 border border-amber-800/50 rounded-xl text-[11px] text-amber-300 font-medium">
                        Registration starts on {event.registrationStartDate}
                      </div>
                    )}

                    {/* Eligibility Badge */}
                    <div className="flex items-center justify-between text-[11px] pt-1">
                      <span className="text-slate-500">Eligibility:</span>
                      <span className={`font-semibold flex items-center gap-1 ${event.eligible ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {event.eligible ? <CheckCircle2 className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                        {event.eligible ? 'Eligible' : 'Not Eligible'}
                      </span>
                    </div>

                    {/* Capacity Progress */}
                    <div className="space-y-1">
                      <div className="flex justify-between text-[11px]">
                        <span className="text-slate-400">Seats: {event.remainingSeats} left</span>
                        <span className="text-indigo-400 font-mono">{capacityPct}%</span>
                      </div>
                      <div className="w-full bg-slate-950 rounded-full h-1.5 overflow-hidden">
                        <div
                          className={`h-full ${capacityPct >= 100 ? 'bg-rose-500' : capacityPct >= 80 ? 'bg-amber-500' : 'bg-indigo-500'}`}
                          style={{ width: `${capacityPct}%` }}
                        />
                      </div>
                    </div>

                    {/* CTA */}
                    <Link to={`/events/${event.id}`}>
                      <Button
                        variant={event.actionStatus === 'REGISTER NOW' ? 'primary' : 'secondary'}
                        size="sm"
                        className="w-full justify-between"
                      >
                        <span>{event.actionStatus}</span>
                        <ChevronRight className="w-4 h-4" />
                      </Button>
                    </Link>
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
