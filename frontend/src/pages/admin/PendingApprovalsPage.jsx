import React, { useState, useEffect } from 'react';
import {
  UserCheck,
  Check,
  X,
  AlertCircle,
  Clock,
  ShieldAlert,
  Search,
  CheckCircle2
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
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRejectStudent, setSelectedRejectStudent] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('Enrollment record not found in college database.');
  const [submittingAction, setSubmittingAction] = useState(false);
  const toast = useToast();

  const fetchPendingStudents = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/students/pending');
      setStudents(res.data || []);
    } catch (err) {
      toast.error('Failed to load pending students');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPendingStudents();
  }, []);

  const handleApprove = async (id, name) => {
    setSubmittingAction(true);
    try {
      await api.put(`/admin/students/${id}/approve`);
      toast.success(`Account approved for ${name}! They can now log in.`);
      fetchPendingStudents();
    } catch (err) {
      toast.error('Failed to approve student');
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRejectStudent) return;
    setSubmittingAction(true);
    try {
      await api.put(`/admin/students/${selectedRejectStudent.id}/reject`, {
        rejectionReason: rejectionReason.trim(),
      });
      toast.info(`Registration rejected for ${selectedRejectStudent.fullName}`);
      setSelectedRejectStudent(null);
      fetchPendingStudents();
    } catch (err) {
      toast.error('Failed to reject registration');
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
              Verification Queue
            </span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">Pending Student Approvals</h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Verify student identity and college enrollment ID records before granting login privileges
          </p>
        </div>

        {loading ? (
          <LoadingSpinner message="Checking pending student approval queue..." />
        ) : students.length === 0 ? (
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
                            onClick={() => handleApprove(stu.id, stu.fullName)}
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
        )}
      </div>

      {/* Rejection Dialog */}
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
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                className="w-full rounded-xl bg-slate-950 border border-slate-800 p-3 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-rose-500"
                placeholder="Explain reason (e.g. invalid enrollment ID, year mismatch)..."
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
              <Button variant="secondary" size="sm" onClick={() => setSelectedRejectStudent(null)}>
                Cancel
              </Button>
              <Button variant="danger" size="sm" loading={submittingAction} onClick={handleReject}>
                Confirm Rejection
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </DashboardLayout>
  );
}
