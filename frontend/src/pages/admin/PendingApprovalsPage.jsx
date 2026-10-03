import React, { useState, useEffect } from 'react';
import {
  UserCheck,
  Check,
  X,
  AlertCircle,
  AlertTriangle,
  Clock,
  ShieldAlert,
  Calendar,
  Building2,
  Users,
  CheckCircle2,
  CalendarCheck
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import { useToast } from '../../context/ToastContext';

export default function PendingApprovalsPage() {
  const [activeTab, setActiveTab] = useState('students');
  const [students, setStudents] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRejectStudent, setSelectedRejectStudent] = useState(null);
  const [studentRejectionReason, setStudentRejectionReason] = useState('Enrollment record not found in college database.');
  const [selectedRejectEvent, setSelectedRejectEvent] = useState(null);
  const [eventRejectionReason, setEventRejectionReason] = useState('Venue schedule conflict with an already approved event.');
  const [submittingAction, setSubmittingAction] = useState(false);
  const toast = useToast();

  const fetchPendingData = async () => {
    setLoading(true);
    try {
      const [stuRes, evtRes] = await Promise.all([
        api.get('/admin/students/pending').catch(() => ({ data: [] })),
        api.get('/admin/events/pending').catch(() => ({ data: [] }))
      ]);
      setStudents(stuRes.data || []);
      setEvents(evtRes.data || []);
    } catch (err) {
      toast.error('Failed to load pending queue data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPendingData();
  }, []);

  const handleApproveStudent = async (id, name) => {
    setSubmittingAction(true);
    try {
      await api.put(`/admin/students/${id}/approve`);
      toast.success(`Account approved for ${name}! They can now log in.`);
      fetchPendingData();
    } catch (err) {
      toast.error('Failed to approve student');
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleRejectStudent = async () => {
    if (!selectedRejectStudent) return;
    setSubmittingAction(true);
    try {
      await api.put(`/admin/students/${selectedRejectStudent.id}/reject`, {
        rejectionReason: studentRejectionReason.trim(),
      });
      toast.info(`Registration rejected for ${selectedRejectStudent.fullName}`);
      setSelectedRejectStudent(null);
      fetchPendingData();
    } catch (err) {
      toast.error('Failed to reject registration');
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleApproveEvent = async (id, title) => {
    setSubmittingAction(true);
    try {
      await api.put(`/admin/events/${id}/approve`);
      toast.success(`Event '${title}' approved and published successfully!`);
      fetchPendingData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to approve event');
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleRejectEvent = async () => {
    if (!selectedRejectEvent) return;
    setSubmittingAction(true);
    try {
      await api.put(`/admin/events/${selectedRejectEvent.id}/reject`, {
        rejectionReason: eventRejectionReason.trim(),
      });
      toast.info(`Event '${selectedRejectEvent.title}' rejected.`);
      setSelectedRejectEvent(null);
      fetchPendingData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to reject event');
    } finally {
      setSubmittingAction(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-amber-400" />
            <span className="text-[11px] font-bold text-amber-400 uppercase tracking-wider">
              Verification & Approval Hub
            </span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">Pending Approval Queue</h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Review student registrations and manage event requests with venue scheduling conflicts
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
          <button
            onClick={() => setActiveTab('students')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
              activeTab === 'students'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-white'
            }`}
          >
            <UserCheck className="w-4 h-4" />
            <span>Student Accounts</span>
            {students.length > 0 && (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500 text-slate-950">
                {students.length}
              </span>
            )}
          </button>

          <button
            onClick={() => setActiveTab('events')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
              activeTab === 'events'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-white'
            }`}
          >
            <CalendarCheck className="w-4 h-4" />
            <span>Event Approvals & Venue Conflicts</span>
            {events.length > 0 && (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500 text-white">
                {events.length}
              </span>
            )}
          </button>
        </div>

        {loading ? (
          <LoadingSpinner message="Checking pending approval queue..." />
        ) : activeTab === 'students' ? (
          /* Student Approvals Tab */
          students.length === 0 ? (
            <EmptyState
              icon={CheckCircle2}
              title="All student registrations reviewed!"
              description="There are currently no students in the pending approval queue. All accounts have been verified."
            />
          ) : (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-950/80 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                    <tr>
                      <th className="px-5 py-3.5">Student Name</th>
                      <th className="px-5 py-3.5">Enrollment ID</th>
                      <th className="px-5 py-3.5">Email</th>
                      <th className="px-5 py-3.5">Branch</th>
                      <th className="px-5 py-3.5">Year / Sem</th>
                      <th className="px-5 py-3.5">Section</th>
                      <th className="px-5 py-3.5">Submitted</th>
                      <th className="px-5 py-3.5 text-right">Administrative Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {students.map((stu) => (
                      <tr key={stu.id} className="hover:bg-slate-800/40 transition-colors">
                        <td className="px-5 py-4 font-bold text-white">
                          {stu.fullName}
                        </td>
                        <td className="px-5 py-4 font-mono font-semibold text-indigo-300">
                          {stu.enrollmentId}
                        </td>
                        <td className="px-5 py-4 text-slate-400 font-mono">
                          {stu.email}
                        </td>
                        <td className="px-5 py-4">
                          <span className="px-2 py-0.5 rounded-md bg-slate-950 border border-slate-800 text-slate-200">
                            {stu.branch}
                          </span>
                        </td>
                        <td className="px-5 py-4">
                          {stu.currentYear} ({stu.currentSemester})
                        </td>
                        <td className="px-5 py-4 font-semibold">
                          Sec {stu.section}
                        </td>
                        <td className="px-5 py-4 text-slate-500 font-mono text-[11px]">
                          {new Date(stu.createdAt).toLocaleDateString()}
                        </td>
                        <td className="px-5 py-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Button
                              variant="success"
                              size="sm"
                              icon={Check}
                              disabled={submittingAction}
                              onClick={() => handleApproveStudent(stu.id, stu.fullName)}
                            >
                              Approve
                            </Button>
                            <Button
                              variant="danger"
                              size="sm"
                              icon={X}
                              disabled={submittingAction}
                              onClick={() => setSelectedRejectStudent(stu)}
                            >
                              Reject
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )
        ) : (
          /* Events & Venue Conflicts Tab */
          events.length === 0 ? (
            <EmptyState
              icon={CheckCircle2}
              title="All event requests reviewed!"
              description="There are currently no events awaiting approval or venue conflict resolution."
            />
          ) : (
            <div className="space-y-4">
              {events.map((evt) => (
                <div
                  key={evt.id}
                  className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl space-y-4 hover:border-slate-700 transition-all"
                >
                  {/* Top Bar with Badge */}
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-indigo-950 text-indigo-300 border border-indigo-800">
                        {evt.category}
                      </span>
                      {evt.hasVenueConflict ? (
                        <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-rose-950 text-rose-300 border border-rose-700/60 flex items-center gap-1.5 animate-pulse">
                          <AlertTriangle className="w-3.5 h-3.5 text-rose-400" />
                          VENUE CONFLICT
                        </span>
                      ) : (
                        <Badge variant="warning" size="sm">
                          PENDING APPROVAL
                        </Badge>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      <Button
                        variant="success"
                        size="sm"
                        icon={Check}
                        disabled={submittingAction}
                        onClick={() => handleApproveEvent(evt.id, evt.title)}
                      >
                        Approve (Override)
                      </Button>
                      <Button
                        variant="danger"
                        size="sm"
                        icon={X}
                        disabled={submittingAction}
                        onClick={() => setSelectedRejectEvent(evt)}
                      >
                        Reject Event
                      </Button>
                    </div>
                  </div>

                  {/* Event Details */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
                    <div className="space-y-2">
                      <h3 className="text-base font-bold text-white">{evt.title}</h3>
                      <p className="text-xs text-slate-400 leading-relaxed line-clamp-2">
                        {evt.description}
                      </p>
                      <div className="text-xs text-slate-300 space-y-1 pt-1">
                        <p><strong>Organizer:</strong> {evt.organizer?.name} ({evt.organizer?.department})</p>
                        <p className="text-indigo-300 font-mono text-[11px]">{evt.organizer?.contactEmail}</p>
                      </div>
                    </div>

                    {/* Venue & Time Requested */}
                    <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2 text-xs">
                      <p className="font-semibold text-white uppercase text-[11px] tracking-wider text-slate-400">
                        Requested Booking Details
                      </p>
                      <div className="space-y-1.5 text-slate-300">
                        <div className="flex items-center gap-2">
                          <Building2 className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                          <span><strong>Venue:</strong> {evt.venue?.name} (Capacity: {evt.venue?.capacity})</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Calendar className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                          <span><strong>Date:</strong> {evt.eventDate}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Clock className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                          <span><strong>Time Slot:</strong> {evt.startTime} - {evt.endTime}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Users className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                          <span><strong>Requested Capacity:</strong> {evt.maxCapacity} seats</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Conflicting Event Warning Banner */}
                  {evt.hasVenueConflict && evt.conflictingEvent && (
                    <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-700/60 space-y-2">
                      <div className="flex items-center gap-2 text-rose-300 font-bold text-xs uppercase tracking-wider">
                        <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                        <span>Conflicting Event Details</span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs text-rose-200/90 pt-1">
                        <div>
                          <p className="text-slate-400 text-[11px]">Existing Booked Event:</p>
                          <p className="font-semibold text-white">{evt.conflictingEvent.title}</p>
                        </div>
                        <div>
                          <p className="text-slate-400 text-[11px]">Existing Booked Slot:</p>
                          <p className="font-mono text-amber-300">
                            {evt.conflictingEvent.startTime} - {evt.conflictingEvent.endTime}
                          </p>
                        </div>
                        <div>
                          <p className="text-slate-400 text-[11px]">Booked By Organizer:</p>
                          <p className="font-medium text-white">{evt.conflictingEvent.organizerName}</p>
                        </div>
                      </div>
                      {evt.conflictNotes && (
                        <p className="text-[11px] text-rose-300/80 italic pt-1 border-t border-rose-900/60 mt-2">
                          {evt.conflictNotes}
                        </p>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )
        )}
      </div>

      {/* Student Rejection Dialog */}
      {selectedRejectStudent && (
        <Modal
          isOpen={!!selectedRejectStudent}
          onClose={() => setSelectedRejectStudent(null)}
          title="Reject Student Registration"
          maxWidth="max-w-md"
        >
          <div className="space-y-4">
            <p className="text-xs text-slate-300">
              Rejecting registration for <strong className="text-white">{selectedRejectStudent.fullName}</strong> (
              <span className="font-mono text-indigo-300">{selectedRejectStudent.enrollmentId}</span>).
            </p>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Reason for Rejection
              </label>
              <textarea
                rows={3}
                value={studentRejectionReason}
                onChange={(e) => setStudentRejectionReason(e.target.value)}
                className="w-full rounded-xl bg-slate-950 border border-slate-800 p-3 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-rose-500"
                placeholder="Explain reason (e.g. invalid enrollment ID, year mismatch)..."
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
              <Button variant="secondary" size="sm" onClick={() => setSelectedRejectStudent(null)}>
                Cancel
              </Button>
              <Button variant="danger" size="sm" loading={submittingAction} onClick={handleRejectStudent}>
                Confirm Rejection
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Event Rejection Dialog */}
      {selectedRejectEvent && (
        <Modal
          isOpen={!!selectedRejectEvent}
          onClose={() => setSelectedRejectEvent(null)}
          title="Reject Event Scheduling Request"
          maxWidth="max-w-md"
        >
          <div className="space-y-4">
            <p className="text-xs text-slate-300">
              Rejecting event scheduling request for <strong className="text-white">{selectedRejectEvent.title}</strong> by organizer <span className="font-semibold text-indigo-300">{selectedRejectEvent.organizer?.name}</span>.
            </p>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Reason for Administrative Rejection
              </label>
              <textarea
                rows={3}
                value={eventRejectionReason}
                onChange={(e) => setEventRejectionReason(e.target.value)}
                className="w-full rounded-xl bg-slate-950 border border-slate-800 p-3 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-rose-500"
                placeholder="Explain reason (e.g. venue conflict with primary symposium)..."
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
              <Button variant="secondary" size="sm" onClick={() => setSelectedRejectEvent(null)}>
                Cancel
              </Button>
              <Button variant="danger" size="sm" loading={submittingAction} onClick={handleRejectEvent}>
                Confirm Rejection
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </DashboardLayout>
  );
}
