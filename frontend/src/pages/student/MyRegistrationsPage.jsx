import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  Clock,
  MapPin,
  QrCode,
  XCircle,
  ExternalLink,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import QrScannerModal from '../../components/qr/QrScannerModal';
import { useToast } from '../../context/ToastContext';

export default function MyRegistrationsPage() {
  const [registrations, setRegistrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedScanEvent, setSelectedScanEvent] = useState(null);
  const [cancellingId, setCancellingId] = useState(null);
  const toast = useToast();

  const fetchRegistrations = async () => {
    setLoading(true);
    try {
      const res = await api.get('/student/registrations');
      const active = (res.data || []).filter((r) => r.status !== 'CANCELLED');
      setRegistrations(active);
    } catch (err) {
      toast.error('Failed to load registrations');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRegistrations();
  }, []);

  const handleCancelRegistration = async (eventId, eventTitle) => {
    if (!window.confirm(`Are you sure you want to cancel your registration for "${eventTitle}"?`)) {
      return;
    }

    setCancellingId(eventId);
    try {
      await api.delete(`/events/${eventId}/register`);
      toast.info('Registration cancelled');
      fetchRegistrations();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel registration');
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">My Registered Events</h1>
          <p className="text-xs text-slate-400">
            View your event registrations, check-in statuses, and take attendance via QR code
          </p>
        </div>

        {loading ? (
          <LoadingSpinner message="Loading your event registrations..." />
        ) : registrations.length === 0 ? (
          <EmptyState
            title="No event registrations"
            description="You have not registered for any events yet. Check out the campus catalog!"
            actionLabel="Discover Events"
            onAction={() => window.location.href = '/student/events'}
          />
        ) : (
          <div className="space-y-4">
            {registrations.map((reg) => (
              <div
                key={reg.id}
                className="p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all flex flex-col md:flex-row md:items-center justify-between gap-5"
              >
                {/* Event Information */}
                <div className="space-y-2 flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-indigo-950 text-indigo-300 border border-indigo-800/60">
                      {reg.category}
                    </span>
                    <Badge variant="default" size="sm">
                      {reg.status}
                    </Badge>
                    <Badge
                      variant={reg.attendanceStatus === 'PRESENT' ? 'success' : 'default'}
                      size="sm"
                    >
                      Attendance: {reg.attendanceStatus || 'PENDING'}
                    </Badge>
                  </div>

                  <Link to={`/events/${reg.eventId}`} className="hover:text-indigo-400 transition-colors block">
                    <h3 className="text-base font-bold text-white truncate">{reg.eventTitle}</h3>
                  </Link>

                  <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-slate-400">
                    <div className="flex items-center gap-1.5">
                      <Calendar className="w-3.5 h-3.5 text-indigo-400" />
                      <span>{reg.eventDate}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-indigo-400" />
                      <span>{reg.startTime} - {reg.endTime}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-indigo-400" />
                      <span className="truncate">{reg.venueName}</span>
                    </div>
                  </div>

                  <p className="text-[11px] text-slate-500 font-mono">
                    Registered on: {new Date(reg.registeredAt).toLocaleDateString()} at{' '}
                    {new Date(reg.registeredAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>

                {/* Actions Button Strip */}
                <div className="flex items-center gap-2 shrink-0 self-end md:self-center">
                  <Button
                    variant="primary"
                    size="sm"
                    icon={QrCode}
                    onClick={() => {
                      setSelectedScanEvent({
                        id: reg.eventId,
                        title: reg.eventTitle,
                        venue: { name: reg.venueName }
                      });
                    }}
                  >
                    Scan QR Check-in
                  </Button>

                  <Link to={`/events/${reg.eventId}`}>
                    <Button variant="secondary" size="sm" icon={ExternalLink}>
                      Details
                    </Button>
                  </Link>

                  {reg.status !== 'CANCELLED' && (
                    <Button
                      variant="danger"
                      size="sm"
                      loading={cancellingId === reg.eventId}
                      onClick={() => handleCancelRegistration(reg.eventId, reg.eventTitle)}
                    >
                      Cancel
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* QR Scanner Dialog */}
      {selectedScanEvent && (
        <QrScannerModal
          isOpen={!!selectedScanEvent}
          onClose={() => setSelectedScanEvent(null)}
          event={selectedScanEvent}
          onSuccess={() => fetchRegistrations()}
        />
      )}
    </DashboardLayout>
  );
}
