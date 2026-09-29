import React, { useState, useEffect } from 'react';
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Legend
} from 'recharts';
import { BarChart3, TrendingUp, Users, Calendar, Award } from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../context/ToastContext';

export default function AnalyticsReportsPage() {
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  useEffect(() => {
    const fetchReports = async () => {
      try {
        const res = await api.get('/admin/reports');
        setReportData(res.data);
      } catch (err) {
        toast.error('Failed to load institutional reports');
      } finally {
        setLoading(false);
      }
    };
    fetchReports();
  }, []);

  if (loading) {
    return (
      <DashboardLayout>
        <LoadingSpinner message="Generating analytical charts and institutional metrics..." />
      </DashboardLayout>
    );
  }

  // Format data for Recharts
  const categoryData = Object.entries(reportData?.eventsByCategory || {}).map(([name, value]) => ({
    name,
    value,
  }));

  const branchData = Object.entries(reportData?.eventsByBranch || {}).map(([name, count]) => ({
    name,
    count,
  }));

  const participationData = Object.entries(reportData?.studentParticipationByBranch || {}).map(([branch, count]) => ({
    branch,
    registrations: count,
  }));

  const COLORS = ['#6366f1', '#a855f7', '#10b981', '#f59e0b', '#ec4899', '#06b6d4', '#3b82f6'];

  return (
    <DashboardLayout>
      <div className="space-y-8">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">Institutional Analytics & Reports</h1>
          <p className="text-xs text-slate-400">
            Real-time visual reports on student participation, event distributions, and campus engagement
          </p>
        </div>

        {/* Top KPI Cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="p-4 space-y-1">
            <span className="text-xs text-slate-400 font-medium">Total Events</span>
            <p className="text-2xl font-bold text-white mt-1">{reportData?.totalEvents || 0}</p>
          </Card>

          <Card className="p-4 space-y-1">
            <span className="text-xs text-slate-400 font-medium">Total Registrations</span>
            <p className="text-2xl font-bold text-indigo-400 mt-1">{reportData?.totalRegistrations || 0}</p>
          </Card>

          <Card className="p-4 space-y-1">
            <span className="text-xs text-slate-400 font-medium">Total Attendance (Check-ins)</span>
            <p className="text-2xl font-bold text-emerald-400 mt-1">{reportData?.totalAttendance || 0}</p>
          </Card>

          <Card className="p-4 space-y-1">
            <span className="text-xs text-slate-400 font-medium">Overall Attendance Rate</span>
            <p className="text-2xl font-bold text-amber-400 mt-1">{reportData?.overallAttendancePercentage || 0}%</p>
          </Card>
        </div>

        {/* Charts Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Chart 1: Events by Category */}
          <Card className="space-y-4">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Calendar className="w-4 h-4 text-indigo-400" />
              <span>Events by Category</span>
            </h3>

            <div className="h-64 w-full">
              {categoryData.length === 0 ? (
                <div className="h-full flex items-center justify-center text-xs text-slate-500">
                  No event categories recorded
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={categoryData}
                      cx="50%"
                      cy="50%"
                      outerRadius={80}
                      innerRadius={45}
                      paddingAngle={4}
                      dataKey="value"
                      label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}
                    >
                      {categoryData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', fontSize: '12px' }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
          </Card>

          {/* Chart 2: Student Participation by Branch */}
          <Card className="space-y-4">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Users className="w-4 h-4 text-emerald-400" />
              <span>Student Participation by Branch</span>
            </h3>

            <div className="h-64 w-full">
              {participationData.length === 0 ? (
                <div className="h-full flex items-center justify-center text-xs text-slate-500">
                  No student registrations recorded yet
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={participationData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                    <XAxis dataKey="branch" stroke="#64748b" fontSize={11} />
                    <YAxis stroke="#64748b" fontSize={11} />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', fontSize: '12px' }}
                    />
                    <Bar dataKey="registrations" fill="#6366f1" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </Card>
        </div>

        {/* Most Popular Events Table */}
        <Card className="space-y-4">
          <h3 className="text-sm font-bold text-white flex items-center gap-2">
            <Award className="w-4 h-4 text-amber-400" />
            <span>Most Popular Campus Events (By Registrations)</span>
          </h3>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Event Name</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Registered Students</th>
                  <th className="py-3 px-4">Max Capacity</th>
                  <th className="py-3 px-4">Capacity Utilization</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {(reportData?.mostPopularEvents || []).map((evt) => {
                  const pct = Math.min(100, Math.round((evt.registeredCount / evt.maxCapacity) * 100));
                  return (
                    <tr key={evt.id} className="hover:bg-slate-800/30">
                      <td className="py-3.5 px-4 font-bold text-white">{evt.title}</td>
                      <td className="py-3.5 px-4">
                        <span className="px-2 py-0.5 rounded bg-indigo-950 text-indigo-300 border border-indigo-800/60 text-[10px] font-semibold">
                          {evt.category}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-mono font-bold text-indigo-400">{evt.registeredCount}</td>
                      <td className="py-3.5 px-4 font-mono">{evt.maxCapacity}</td>
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-24 bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-800">
                            <div className="bg-indigo-500 h-full" style={{ width: `${pct}%` }} />
                          </div>
                          <span className="font-mono text-slate-300 font-semibold">{pct}%</span>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </DashboardLayout>
  );
}
