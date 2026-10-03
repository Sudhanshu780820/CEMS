import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  Calendar,
  Clock,
  MapPin,
  Building2,
  Users,
  GraduationCap,
  ArrowLeft,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  QrCode,
  CheckCheck,
  Share2,
  Sparkles
} from 'lucide-react';
import api from '../../api/client';
import PublicNavbar from '../../components/layout/PublicNavbar';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import QrScannerModal from '../../components/qr/QrScannerModal';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatDate, formatTime, formatDateTime, formatTimeRange } from '../../utils/dateUtils';

export default function EventDetailPage() {
  const { id } = useParams();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [registering, setRegistering] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [qrModalOpen, setQrModalOpen] = useState(false);
  const { user, isAuthenticated, isStudent } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  const fetchEvent = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/events/${id}`);
      setEvent(res.data);
    } catch (err) {
      toast.error('Event not found or unavailable');
      navigate('/events');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvent();
  }, [id]);

  const handleRegister = async () => {
    if (!isAuthenticated) {
      navigate(`/login?redirect=/events/${id}`);
      return;
    }

    if (!isStudent) {
      toast.warning('Only registered college students can register for events.');
      return;
    }

    setRegistering(true);
    try {
      await api.post(`/events/${id}/register`);
      toast.success('Successfully registered for this event!');
      fetchEvent();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to register. Please check eligibility.');
    } finally {
      setRegistering(false);
    }
  };

  const handleCancelRegistration = async () => {
    if (!window.confirm('Are you sure you want to cancel your event registration? Your reserved seat will be released.')) {
      return;
    }

    setCancelling(true);
    try {
      await api.delete(`/events/${id}/register`);
      toast.info('Registration cancelled.');
      fetchEvent();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel registration.');
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col">
        <PublicNavbar />
        <div className="flex-1 flex items-center justify-center">
          <LoadingSpinner message="Loading event details..." size="lg" />
        </div>
      </div>
    );
  }

  if (!event) return null;

  const capacityPct = Math.min(100, Math.round((event.registeredCount / event.maxCapacity) * 100));

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      <PublicNavbar />

      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-6">
        {/* Navigation Breadcrumb */}
        <Link
          to="/events"
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to all events
        </Link>

        {/* Hero Banner Header */}
        <div className="relative rounded-3xl overflow-hidden bg-slate-900 border border-slate-800 shadow-2xl">
          <div className="h-64 sm:h-80 w-full relative">
            <img
              src={event.eventImageUrl || 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1400&q=80'}
              alt={event.title}
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/60 to-transparent" />

            {/* Badges on image */}
            <div className="absolute top-4 left-4 flex gap-2">
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-950/90 text-indigo-300 backdrop-blur-md border border-indigo-500/30">
                {event.category}
              </span>
              <Badge
                variant={
                  event.status === 'COMPLETED' ? 'default' :
                  event.registrationState === 'OPEN' ? 'success' :
                  event.registrationState === 'NOT_STARTED' ? 'warning' : 'danger'
                }
                size="md"
              >
                {event.status === 'COMPLETED' ? 'COMPLETED' :
                 event.registrationState === 'NOT_STARTED' ? 'REGISTRATION NOT STARTED' :
                 event.registrationState === 'OPEN' ? 'REGISTRATION OPEN' : 'REGISTRATION CLOSED'}
              </Badge>
            </div>
          </div>

          <div className="p-6 sm:p-8 -mt-20 relative z-10 space-y-4">
            <h1 className="text-2xl sm:text-4xl font-extrabold text-white tracking-tight">
              {event.title}
            </h1>

            {/* Core details pill strip */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
              <div className="p-3 bg-slate-900/90 border border-slate-800 rounded-xl backdrop-blur-sm">
                <p className="text-[11px] text-slate-400 font-medium">Date</p>
                <p className="text-sm font-semibold text-white mt-0.5">{formatDate(event.eventDate)}</p>
              </div>

              <div className="p-3 bg-slate-900/90 border border-slate-800 rounded-xl backdrop-blur-sm">
                <p className="text-[11px] text-slate-400 font-medium">Time</p>
                <p className="text-sm font-semibold text-white mt-0.5">{formatTimeRange(event.startTime, event.endTime) || `${formatTime(event.startTime)} - ${formatTime(event.endTime)}`}</p>
              </div>

              <div className="p-3 bg-slate-900/90 border border-slate-800 rounded-xl backdrop-blur-sm">
                <p className="text-[11px] text-slate-400 font-medium">Venue</p>
                <p className="text-sm font-semibold text-white mt-0.5 truncate">{event.venue?.name}</p>
              </div>

              <div className="p-3 bg-slate-900/90 border border-slate-800 rounded-xl backdrop-blur-sm">
                <p className="text-[11px] text-slate-400 font-medium">Available Seats</p>
                <p className="text-sm font-semibold text-indigo-400 mt-0.5 font-mono">
                  {event.remainingSeats} / {event.maxCapacity}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Two-Column Grid: Details & Registration Action Panel */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Content (2 Cols) */}
          <div className="lg:col-span-2 space-y-6">
            {/* Description */}
            <div className="p-6 bg-slate-900 border border-slate-800 rounded-2xl space-y-3">
              <h3 className="text-base font-bold text-white">About the Event</h3>
              <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-line">
                {event.description}
              </p>
            </div>

            {/* Eligibility Breakdown */}
            <div className="p-6 bg-slate-900 border border-slate-800 rounded-2xl space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-base font-bold text-white">Eligibility Criteria</h3>
                {isStudent && (
                  <Badge variant={event.eligible ? 'success' : 'danger'} size="sm">
                    {event.eligible ? 'You are Eligible' : 'Not Eligible'}
                  </Badge>
                )}
              </div>

              <div className="space-y-3 divide-y divide-slate-800/80 text-xs">
                <div className="pt-2 flex justify-between items-center">
                  <span className="text-slate-400">Branches / Departments:</span>
                  <span className="font-semibold text-slate-200">
                    {event.eligibleBranches || 'All Branches Welcome'}
                  </span>
                </div>

                <div className="pt-3 flex justify-between items-center">
                  <span className="text-slate-400">Academic Years:</span>
                  <span className="font-semibold text-slate-200">
                    {event.eligibleAcademicYears || 'All Academic Years'}
                  </span>
                </div>

                <div className="pt-3 flex justify-between items-center">
                  <span className="text-slate-400">Sections:</span>
                  <span className="font-semibold text-slate-200">
                    {event.eligibleSections || 'All Sections'}
                  </span>
                </div>
              </div>

              {isStudent && !event.eligible && (
                <div className="p-3 bg-rose-950/60 border border-rose-800 rounded-xl text-xs text-rose-300 flex items-start gap-2">
                  <XCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
                  <span>
                    You are not eligible to register for this event. Your profile ({user?.branch}) does not match the event eligibility criteria.
                  </span>
                </div>
              )}
            </div>

            {/* Venue & Logistics Details */}
            <div className="p-6 bg-slate-900 border border-slate-800 rounded-2xl space-y-3">
              <h3 className="text-base font-bold text-white">Venue Location</h3>
              <div className="space-y-2 text-xs text-slate-300">
                <div className="flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-indigo-400 shrink-0" />
                  <span><strong>Building:</strong> {event.venue?.building}</span>
                </div>
                <div className="flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-indigo-400 shrink-0" />
                  <span><strong>Room / Hall:</strong> {event.venue?.roomNumber}</span>
                </div>
                {event.venue?.description && (
                  <p className="text-slate-400 mt-2 italic">{event.venue.description}</p>
                )}
              </div>
            </div>
          </div>

          {/* Right Action Sidebar (1 Col) */}
          <div className="space-y-6">
            {/* Registration Action Card */}
            <div className="p-6 bg-slate-900 border border-slate-800 rounded-2xl space-y-5 shadow-xl sticky top-24">
              <h3 className="text-base font-bold text-white">Event Registration</h3>

              {/* Registration Dates Window */}
              <div className="p-3 bg-slate-950/80 border border-slate-800/80 rounded-xl space-y-1.5 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-400">Registration Opens:</span>
                  <span className="text-slate-200 font-medium">{event.registrationStartDate}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Registration Closes:</span>
                  <span className="text-slate-200 font-medium">{event.registrationEndDate}</span>
                </div>
              </div>

              {/* Capacity Progress Bar */}
              <div className="space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-slate-400">Capacity Occupancy</span>
                  <span className="text-indigo-400 font-mono font-bold">{capacityPct}%</span>
                </div>
                <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-800">
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
                <p className="text-[11px] text-slate-400">
                  {event.remainingSeats > 0 ? (
                    <span><strong>{event.remainingSeats}</strong> seats remaining out of {event.maxCapacity}</span>
                  ) : (
                    <span className="text-rose-400 font-semibold">Event is fully booked!</span>
                  )}
                </p>
              </div>

              {/* Dynamic Action Buttons */}
              {!isAuthenticated ? (
                <div className="space-y-2 pt-2">
                  <Button
                    variant="primary"
                    className="w-full"
                    onClick={() => navigate(`/login?redirect=/events/${id}`)}
                  >
                    Sign in to Register
                  </Button>
                  <p className="text-[11px] text-slate-500 text-center">
                    College login required to verify enrollment eligibility
                  </p>
                </div>
              ) : isStudent ? (
                <div className="space-y-3 pt-2">
                  {event.registered ? (
                    <div className="space-y-3">
                      <div className="p-3 bg-emerald-950/60 border border-emerald-800 rounded-xl text-emerald-300 text-xs flex items-center gap-2">
                        <CheckCheck className="w-4 h-4 text-emerald-400 shrink-0" />
                        <span>You are registered for this event!</span>
                      </div>

                      {/* Attendance Status & Actions */}
                      {event.studentAttendanceStatus === 'PRESENT' || event.attendanceState === 'PRESENT' ? (
                        <div className="p-3 bg-emerald-950/80 border border-emerald-700/60 rounded-xl text-emerald-400 text-xs font-semibold flex items-center justify-center gap-2">
                          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                          <span>Attendance Verified: PRESENT ✓</span>
                        </div>
                      ) : event.attendanceState === 'CHECK_IN_OPEN' || (event.checkInAllowed && event.attendanceActive) ? (
                        <Button
                          variant="success"
                          className="w-full animate-bounce"
                          icon={QrCode}
                          onClick={() => setQrModalOpen(true)}
                        >
                          Scan QR Check-in
                        </Button>
                      ) : event.attendanceState === 'NOT_STARTED' ? (
                        <div className="p-2.5 bg-blue-950/40 border border-blue-800/50 rounded-xl text-[11px] text-blue-300 text-center font-medium">
                          Check-in opens: {formatDateTime(event.eventDate, event.startTime)}
                        </div>
                      ) : (
                        <div className="p-2.5 bg-rose-950/40 border border-rose-800/50 rounded-xl text-[11px] text-rose-300 text-center font-medium">
                          Check-in is closed (24-hour window ended)
                        </div>
                      )}

                      <Button
                        variant="danger"
                        size="sm"
                        className="w-full"
                        loading={cancelling}
                        onClick={handleCancelRegistration}
                      >
                        Cancel My Registration
                      </Button>
                    </div>
                  ) : !event.eligible ? (
                    <Button variant="secondary" className="w-full cursor-not-allowed opacity-75" disabled>
                      Not Eligible for Your Branch/Year
                    </Button>
                  ) : event.registrationState === 'NOT_STARTED' ? (
                    <div className="space-y-2">
                      <Button variant="secondary" className="w-full cursor-not-allowed opacity-75" disabled>
                        Registration Not Started
                      </Button>
                      <p className="text-[11px] text-amber-400 text-center font-medium">
                        Registration starts on {formatDate(event.registrationStartDate)}
                      </p>
                    </div>
                  ) : event.registrationState === 'CLOSED' || !event.registrationOpen ? (
                    <div className="space-y-2">
                      <Button variant="secondary" className="w-full cursor-not-allowed opacity-75" disabled>
                        Registration Closed
                      </Button>
                      <p className="text-[11px] text-rose-400 text-center font-medium">
                        Registration ended on {formatDate(event.registrationEndDate)}
                      </p>
                    </div>
                  ) : event.remainingSeats <= 0 ? (
                    <Button variant="secondary" className="w-full cursor-not-allowed opacity-75" disabled>
                      Event at Full Capacity
                    </Button>
                  ) : (
                    <Button
                      variant="primary"
                      className="w-full"
                      loading={registering}
                      onClick={handleRegister}
                    >
                      Register Now
                    </Button>
                  )}
                </div>
              ) : (
                <div className="p-3 bg-slate-950 rounded-xl text-xs text-slate-400 text-center">
                  Logged in as {user?.role?.replace('ROLE_', '')}. Student privileges required to register for attendance.
                </div>
              )}

              {/* Organizer Information */}
              <div className="border-t border-slate-800/80 pt-4 space-y-2">
                <p className="text-[11px] uppercase font-bold text-slate-500 tracking-wider">
                  Organized By:
                </p>
                <div className="text-xs text-slate-300 space-y-1">
                  <p className="font-semibold text-white">{event.organizer?.name}</p>
                  <p className="text-slate-400">{event.organizer?.department}</p>
                  <p className="text-indigo-400 font-mono">{event.organizer?.contactEmail}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* QR Scanner Modal for student check-in */}
      <QrScannerModal
        isOpen={qrModalOpen}
        onClose={() => setQrModalOpen(false)}
        event={event}
        onSuccess={() => fetchEvent()}
      />
    </div>
  );
}
