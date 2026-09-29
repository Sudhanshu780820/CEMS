import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Users,
  UserCheck,
  Calendar,
  Building2,
  BarChart3,
  TrendingUp,
  AlertTriangle,
  ArrowRight,
  ShieldCheck,
  UserPlus
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await api.get('/admin/dashboard');
        setStats(res.data);
      } catch (err) {
        console.error('Failed to load admin stats:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) {
    return (
      <DashboardLayout>
        <LoadingSpinner message="Calculating institutional metrics..." />
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Banner */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white">Central Administration Dashboard</h1>
            <p className="text-xs text-slate-400">
              Institutional oversight across student admissions, event authorizations, campus venues, and attendance
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Link to="/admin/reports">
              <Button variant="secondary" icon={BarChart3}>
                Reports
              </Button>
            </Link>
            <Link to="/admin/organizers">
              <Button variant="primary" icon={UserPlus}>
                Add Organizer
              </Button>
            </Link>
          </div>
        </div>

        {/* Pending Approvals Alert Banner */}
        {stats?.pendingStudentApprovals > 0 && (
          <div className="p-5 rounded-2xl bg-amber-950/60 border border-amber-800/80 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
            <div className="flex items-start gap-3">
              <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 shrink-0 mt-0.5">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">
                  {stats.pendingStudentApprovals} Student Registration{stats.pendingStudentApprovals > 1 ? 's' : ''} Awaiting Approval
                </h3>
                <p className="text-xs text-amber-200/80 mt-0.5">
                  Verify student enrollment IDs against university records to grant portal access.
                </p>
              </div>
            </div>

            <Button
              variant="primary"
              size="sm"
              className="shrink-0 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold"
              onClick={() => navigate('/admin/students/pending')}
            >
              Review Approvals ({stats.pendingStudentApprovals})
            </Button>
          </div>
        )}

        {/* Core KPI Metrics Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <Card hover className="space-y-1">
            <div className="flex items-center justify-between text-slate-400">
              <span className="text-xs font-medium">Total Students</span>
              <Users className="w-4 h-4 text-indigo-400" />
            </div>
            <p className="text-2xl font-bold text-white mt-1">{stats?.totalStudents || 0}</p>
            <span className="text-[11px] text-emerald-400 font-medium">
              {stats?.approvedStudents || 0} approved accounts
            </span>
          </Card>

          <Card hover className="space-y-1">
            <div className="flex items-center justify-between text-slate-400">
              <span className="text-xs font-medium">Total Events</span>
              <Calendar className="w-4 h-4 text-purple-400" />
            </div>
            <p className="text-2xl font-bold text-white mt-1">{stats?.totalEvents || 0}</p>
            <span className="text-[11px] text-indigo-400 font-medium">
              {stats?.upcomingEvents || 0} upcoming
            </span>
          </Card>

          <Card hover className="space-y-1">
            <div className="flex items-center justify-between text-slate-400">
              <span className="text-xs font-medium">Total Registrations</span>
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
            </div>
            <p className="text-2xl font-bold text-white mt-1">{stats?.totalRegistrations || 0}</p>
            <span className="text-[11px] text-slate-400">Across campus events</span>
          </Card>

          <Card hover className="space-y-1">
            <div className="flex items-center justify-between text-slate-400">
              <span className="text-xs font-medium">Overall Attendance</span>
              <TrendingUp className="w-4 h-4 text-amber-400" />
            </div>
            <p className="text-2xl font-bold text-white mt-1">{stats?.overallAttendanceRate || 0}%</p>
            <span className="text-[11px] text-amber-400 font-medium">Campus-wide rate</span>
          </Card>
        </div>

        {/* Quick Admin Actions Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          <Link
            to="/admin/students/pending"
            className="p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all block group"
          >
            <div className="w-10 h-10 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center mb-3">
              <UserCheck className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white group-hover:text-amber-400 transition-colors">
              Pending Student Approvals
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Verify new registrations and manage student enrollment standing.
            </p>
          </Link>

          <Link
            to="/admin/venues"
            className="p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all block group"
          >
            <div className="w-10 h-10 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center mb-3">
              <Building2 className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white group-hover:text-indigo-400 transition-colors">
              Campus Venue Management
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Add auditoriums, halls, set capacities, and review booking availability.
            </p>
          </Link>

          <Link
            to="/admin/reports"
            className="p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all block group"
          >
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center mb-3">
              <BarChart3 className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white group-hover:text-emerald-400 transition-colors">
              Institutional Reports
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              View department event distributions, category breakdowns, and participation metrics.
            </p>
          </Link>
        </div>
      </div>
    </DashboardLayout>
  );
}
