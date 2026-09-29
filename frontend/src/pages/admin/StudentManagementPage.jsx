import React, { useState, useEffect } from 'react';
import {
  Users,
  Search,
  Filter,
  ShieldAlert,
  CheckCircle,
  Eye,
  UserX,
  UserCheck
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import { useToast } from '../../context/ToastContext';

export default function StudentManagementPage() {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [branchFilter, setBranchFilter] = useState('');
  const [yearFilter, setYearFilter] = useState('');
  const [sectionFilter, setSectionFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedStudentProfile, setSelectedStudentProfile] = useState(null);
  const toast = useToast();

  const fetchStudents = async () => {
    setLoading(true);
    try {
      const params = {};
      if (searchQuery.trim()) params.query = searchQuery.trim();
      if (branchFilter) params.branch = branchFilter;
      if (yearFilter) params.currentYear = yearFilter;
      if (sectionFilter) params.section = sectionFilter;
      if (statusFilter) params.status = statusFilter;

      const res = await api.get('/admin/students', { params });
      setStudents(res.data || []);
    } catch (err) {
      toast.error('Failed to load students');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudents();
  }, [branchFilter, yearFilter, sectionFilter, statusFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchStudents();
  };

  const handleStatusChange = async (studentId, newStatus, reason = '') => {
    try {
      await api.put(`/admin/students/${studentId}/status`, {
        status: newStatus,
        rejectionReason: reason,
      });
      toast.success(`Student status updated to ${newStatus}`);
      fetchStudents();
    } catch (err) {
      toast.error('Failed to update student status');
    }
  };

  const branches = ['CSE', 'IT', 'AI/ML', 'ECE', 'ME', 'Civil'];
  const years = ['1st Year', '2nd Year', '3rd Year', '4th Year'];
  const sections = ['A', 'B', 'C', 'D'];
  const statuses = ['APPROVED', 'PENDING', 'REJECTED', 'SUSPENDED'];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">Student Directory & Profiles</h1>
          <p className="text-xs text-slate-400">
            Search, filter by branch/cohort, and manage academic standing of enrolled students
          </p>
        </div>

        {/* Filter Toolbar */}
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
          <form onSubmit={handleSearchSubmit} className="flex gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search by full name, email, or enrollment ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <Button type="submit" variant="primary" size="sm">
              Search
            </Button>
          </form>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1">
            <select
              value={branchFilter}
              onChange={(e) => setBranchFilter(e.target.value)}
              className="bg-slate-950 border border-slate-800 text-slate-200 text-xs rounded-xl px-2.5 py-2"
            >
              <option value="">All Branches</option>
              {branches.map((b) => (
                <option key={b} value={b}>{b}</option>
              ))}
            </select>

            <select
              value={yearFilter}
              onChange={(e) => setYearFilter(e.target.value)}
              className="bg-slate-950 border border-slate-800 text-slate-200 text-xs rounded-xl px-2.5 py-2"
            >
              <option value="">All Years</option>
              {years.map((y) => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>

            <select
              value={sectionFilter}
              onChange={(e) => setSectionFilter(e.target.value)}
              className="bg-slate-950 border border-slate-800 text-slate-200 text-xs rounded-xl px-2.5 py-2"
            >
              <option value="">All Sections</option>
              {sections.map((s) => (
                <option key={s} value={s}>Section {s}</option>
              ))}
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-slate-950 border border-slate-800 text-slate-200 text-xs rounded-xl px-2.5 py-2"
            >
              <option value="">All Statuses</option>
              {statuses.map((st) => (
                <option key={st} value={st}>{st}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Student Records Table */}
        {loading ? (
          <LoadingSpinner message="Searching student records..." />
        ) : students.length === 0 ? (
          <EmptyState
            icon={Users}
            title="No students match the criteria"
            description="Try resetting your filters or search keywords."
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
                    <th className="px-5 py-3.5">Cohort</th>
                    <th className="px-5 py-3.5">Status</th>
                    <th className="px-5 py-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {students.map((stu) => (
                    <tr key={stu.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="px-5 py-3.5 font-bold text-white">{stu.fullName}</td>
                      <td className="px-5 py-3.5 font-mono text-indigo-300">{stu.enrollmentId}</td>
                      <td className="px-5 py-3.5 text-slate-400 font-mono">{stu.email}</td>
                      <td className="px-5 py-3.5 font-semibold">{stu.branch}</td>
                      <td className="px-5 py-3.5 text-slate-400">
                        {stu.currentYear} • Sec {stu.section}
                      </td>
                      <td className="px-5 py-3.5">
                        <Badge variant="default" size="sm">{stu.approvalStatus}</Badge>
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => setSelectedStudentProfile(stu)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
                            title="View Profile"
                          >
                            <Eye className="w-4 h-4" />
                          </button>

                          {stu.approvalStatus === 'APPROVED' ? (
                            <button
                              onClick={() => handleStatusChange(stu.id, 'SUSPENDED', 'Administrative suspension')}
                              className="text-xs text-amber-400 hover:text-amber-300 font-medium px-2 py-1 rounded bg-amber-950/40 border border-amber-800/50"
                              title="Suspend student access"
                            >
                              Suspend
                            </button>
                          ) : stu.approvalStatus === 'SUSPENDED' ? (
                            <button
                              onClick={() => handleStatusChange(stu.id, 'APPROVED')}
                              className="text-xs text-emerald-400 hover:text-emerald-300 font-medium px-2 py-1 rounded bg-emerald-950/40 border border-emerald-800/50"
                              title="Reinstate student access"
                            >
                              Reinstate
                            </button>
                          ) : null}
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

      {/* Profile Details Dialog */}
      {selectedStudentProfile && (
        <Modal
          isOpen={!!selectedStudentProfile}
          onClose={() => setSelectedStudentProfile(null)}
          title="Student Record Profile"
          maxWidth="max-w-md"
        >
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
              <h3 className="text-base font-bold text-white">{selectedStudentProfile.fullName}</h3>
              <p className="text-xs text-indigo-400 font-mono font-semibold">
                Enrollment ID: {selectedStudentProfile.enrollmentId}
              </p>
              <div className="flex items-center gap-2 pt-1">
                <Badge variant="default" size="sm">{selectedStudentProfile.approvalStatus}</Badge>
                <span className="text-xs text-slate-400">{selectedStudentProfile.email}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 uppercase tracking-wider text-[10px] font-bold">Branch</span>
                <p className="font-semibold text-white mt-0.5">{selectedStudentProfile.branch}</p>
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 uppercase tracking-wider text-[10px] font-bold">Academic Cohort</span>
                <p className="font-semibold text-white mt-0.5">{selectedStudentProfile.academicYear}</p>
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 uppercase tracking-wider text-[10px] font-bold">Year / Semester</span>
                <p className="font-semibold text-white mt-0.5">
                  {selectedStudentProfile.currentYear} ({selectedStudentProfile.currentSemester})
                </p>
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 uppercase tracking-wider text-[10px] font-bold">Class Section</span>
                <p className="font-semibold text-white mt-0.5">Section {selectedStudentProfile.section}</p>
              </div>
            </div>

            {selectedStudentProfile.rejectionReason && (
              <div className="p-3 bg-rose-950/60 border border-rose-800 text-rose-300 text-xs rounded-xl">
                <strong>Status Note:</strong> {selectedStudentProfile.rejectionReason}
              </div>
            )}

            <div className="pt-2 flex justify-end">
              <Button variant="secondary" size="sm" onClick={() => setSelectedStudentProfile(null)}>
                Close
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </DashboardLayout>
  );
}
