import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Calendar,
  Clock,
  MapPin,
  Users,
  Search,
  Filter,
  ArrowUpDown,
  GraduationCap,
  Sparkles,
  ChevronRight
} from 'lucide-react';
import api from '../../api/client';
import PublicNavbar from '../../components/layout/PublicNavbar';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import EventDateBlock from '../../components/common/EventDateBlock';
import { formatDate, formatTime, formatDateTime, formatTimeRange } from '../../utils/dateUtils';
import { useAuth } from '../../context/AuthContext';

export default function PublicEventsPage() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedBranch, setSelectedBranch] = useState('ALL');
  const [sortBy, setSortBy] = useState('EARLIEST');
  const { isAuthenticated, isStudent } = useAuth();
  const navigate = useNavigate();

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const res = await api.get('/events');
      setEvents(res.data || []);
    } catch (err) {
      console.error('Failed to load events:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  const categories = ['ALL', 'Technical', 'Workshop', 'Cultural', 'Seminar', 'Sports'];
  const branches = ['ALL', 'CSE', 'IT', 'AI/ML', 'ECE', 'ME', 'Civil'];

  const isCompletedEvent = (e) => {
    if (e.status === 'COMPLETED' || e.status === 'CANCELLED') return true;
    if (!e.eventDate) return false;
    const now = new Date();
    const eventEnd = new Date(`${e.eventDate}T${e.endTime || '23:59:59'}`);
    return eventEnd < now;
  };

  // Client-side filtering & sorting
  const filteredEvents = events.filter((e) => {
    if (isCompletedEvent(e)) return false;

    const matchesSearch = !searchQuery.trim() ||
      e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.venue?.name?.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesCategory = selectedCategory === 'ALL' || e.category === selectedCategory;

    const matchesBranch = selectedBranch === 'ALL' ||
      !e.eligibleBranches ||
      e.eligibleBranches.toLowerCase().includes(selectedBranch.toLowerCase());

    return matchesSearch && matchesCategory && matchesBranch;
  }).sort((a, b) => {
    if (sortBy === 'EARLIEST') {
      return new Date(a.eventDate) - new Date(b.eventDate);
    }
    return new Date(b.eventDate) - new Date(a.eventDate);
  });

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      <PublicNavbar />

      {/* Hero Banner */}
      <section className="relative overflow-hidden py-12 px-4 sm:px-6 lg:px-8 border-b border-slate-800/80 bg-gradient-to-b from-indigo-950/30 via-slate-950 to-slate-950">
        <div className="max-w-7xl mx-auto text-center space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-900/40 border border-indigo-700/50 text-indigo-300 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5" />
            Official Campus Events Platform
          </div>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight">
            Discover Campus Happenings
          </h1>
          <p className="text-sm text-slate-400 max-w-xl mx-auto">
            Explore workshops, hackathons, guest lectures, and cultural events. Filter by eligibility and reserve your seat online.
          </p>
        </div>
      </section>

      {/* Discovery Filters Bar */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 w-full space-y-4">
        <div className="flex flex-col md:flex-row gap-3">
          {/* Search Box */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by event title, keyword, or venue..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>

          {/* Branch Filter */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 font-medium shrink-0">Branch:</span>
            <select
              value={selectedBranch}
              onChange={(e) => setSelectedBranch(e.target.value)}
              className="bg-slate-900 border border-slate-800 text-slate-200 text-xs rounded-xl px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {branches.map((b) => (
                <option key={b} value={b} className="bg-slate-900">
                  {b === 'ALL' ? 'All Branches' : b}
                </option>
              ))}
            </select>
          </div>

          {/* Sort Control */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 font-medium shrink-0">Sort:</span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="bg-slate-900 border border-slate-800 text-slate-200 text-xs rounded-xl px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="EARLIEST" className="bg-slate-900">Upcoming First</option>
              <option value="LATEST" className="bg-slate-900">Later Dates</option>
            </select>
          </div>
        </div>

        {/* Category Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-colors cursor-pointer ${
                selectedCategory === cat
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                  : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-white hover:border-slate-700'
              }`}
            >
              {cat === 'ALL' ? 'All Categories' : cat}
            </button>
          ))}
        </div>
      </div>

      {/* Events Grid */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-16 flex-1 w-full">
        {loading ? (
          <LoadingSpinner message="Loading campus events..." />
        ) : filteredEvents.length === 0 ? (
          <EmptyState
            title="No events found"
            description="Try clearing your search query or selecting a different category filter."
            actionLabel="Reset Filters"
            onAction={() => {
              setSearchQuery('');
              setSelectedCategory('ALL');
              setSelectedBranch('ALL');
            }}
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredEvents.map((event) => {
              const capacityPct = Math.min(100, Math.round((event.registeredCount / event.maxCapacity) * 100));
              const isFull = event.remainingSeats <= 0;

              return (
                <div
                  key={event.id}
                  className="group bg-slate-900 border border-slate-800/80 rounded-2xl overflow-hidden hover:border-slate-700 transition-all duration-200 flex flex-col hover:shadow-xl hover:shadow-indigo-500/5"
                >
                  {/* Event Image Banner */}
                  <div className="relative h-44 w-full bg-slate-950 overflow-hidden">
                    <img
                      src={event.eventImageUrl || 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=800&q=80'}
                      alt={event.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-slate-900 via-transparent to-black/40" />

                    {/* Category Badge Top Left */}
                    <div className="absolute top-3 left-3">
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-slate-900/90 text-indigo-300 backdrop-blur-md border border-indigo-500/30">
                        {event.category}
                      </span>
                    </div>

                    {/* Action Status Top Right */}
                    <div className="absolute top-3 right-3">
                      <Badge
                        variant={
                          event.actionStatus === 'REGISTER NOW' ? 'success' :
                          event.actionStatus === 'REGISTRATION NOT STARTED' ? 'warning' :
                          event.actionStatus === 'ALREADY REGISTERED' ? 'purple' : 'default'
                        }
                        size="sm"
                      >
                        {event.actionStatus || (isFull ? 'FULL' : 'REGISTER NOW')}
                      </Badge>
                    </div>
                  </div>

                  {/* Body Content */}
                  <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                    <div className="flex items-start gap-3">
                      <EventDateBlock date={event.eventDate} />
                      <div className="space-y-1 flex-1 min-w-0">
                        <h3 className="text-base font-bold text-white group-hover:text-indigo-400 transition-colors line-clamp-2 leading-snug">
                          {event.title}
                        </h3>
                        <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">
                          {event.description}
                        </p>
                      </div>
                    </div>

                    {/* Event Metadata details */}
                    <div className="space-y-2 text-xs text-slate-300 border-t border-slate-800/60 pt-3">
                      <div className="flex items-center gap-2">
                        <Calendar className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                        <span>{formatDate(event.eventDate)}</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <Clock className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                        <span>{formatTimeRange(event.startTime, event.endTime)}</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <MapPin className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                        <span className="truncate">{event.venue?.name} ({event.venue?.building})</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <GraduationCap className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                        <span className="truncate">By: {event.organizer?.name}</span>
                      </div>
                    </div>

                    {/* Eligibility Pills */}
                    <div className="pt-1">
                      <p className="text-[10px] uppercase font-bold text-slate-500 tracking-wider mb-1">
                        Eligibility:
                      </p>
                      <div className="flex flex-wrap gap-1">
                        {event.eligibleBranches ? (
                          event.eligibleBranches.split(',').map((b) => (
                            <span key={b} className="text-[10px] px-2 py-0.5 rounded bg-slate-950 border border-slate-800 text-indigo-300">
                              {b}
                            </span>
                          ))
                        ) : (
                          <span className="text-[10px] px-2 py-0.5 rounded bg-slate-950 border border-slate-800 text-emerald-400">
                            All Branches
                          </span>
                        )}
                        {event.eligibleAcademicYears && (
                          <span className="text-[10px] px-2 py-0.5 rounded bg-slate-950 border border-slate-800 text-amber-300">
                            {event.eligibleAcademicYears}
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Seat Capacity Progress */}
                    <div className="pt-1 space-y-1.5">
                      <div className="flex justify-between text-[11px]">
                        <span className="text-slate-400">
                          Available Seats:{' '}
                          <strong className="text-white">
                            {event.remainingSeats} / {event.maxCapacity}
                          </strong>
                        </span>
                        <span className="text-indigo-400 font-mono font-semibold">{capacityPct}% booked</span>
                      </div>
                      <div className="w-full bg-slate-950 rounded-full h-1.5 overflow-hidden border border-slate-800">
                        <div
                          className={`h-full transition-all duration-300 ${
                            capacityPct >= 100
                              ? 'bg-rose-500'
                              : capacityPct >= 80
                              ? 'bg-amber-500'
                              : 'bg-indigo-500'
                          }`}
                          style={{ width: `${capacityPct}%` }}
                        />
                      </div>
                    </div>

                    {/* Card Footer CTA */}
                    <div className="pt-2">
                      <Link to={`/events/${event.id}`}>
                        <Button
                          variant={event.actionStatus === 'REGISTER NOW' ? 'primary' : 'secondary'}
                          size="sm"
                          className="w-full justify-between"
                        >
                          <span>{event.actionStatus || 'View Details'}</span>
                          <ChevronRight className="w-4 h-4" />
                        </Button>
                      </Link>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
