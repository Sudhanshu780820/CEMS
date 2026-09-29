import React, { useState, useEffect } from 'react';
import { History, Calendar, CheckCircle2, QrCode, UserCheck, Award } from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';

export default function AttendanceHistoryPage() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchAttendanceHistory = async () => {
    setLoading(true);
    try {
      const res = await api.get('/student/attendance');
      setRecords(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAttendanceHistory();
  }, []);

  const presentCount = records.filter((r) => r.status === 'PRESENT').length;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">Attendance History</h1>
          <p className="text-xs text-slate-400">
            Verified check-in records for workshops, seminars, and activities
          </p>
        </div>

        {/* Summary Card */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Card className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-emerald-600/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center">
              <Award className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs text-slate-400 font-medium">Total Events Attended</p>
              <h3 className="text-2xl font-bold text-white mt-0.5">{presentCount}</h3>
            </div>
          </Card>

          <Card className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center">
              <QrCode className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs text-slate-400 font-medium">QR Check-ins Recorded</p>
              <h3 className="text-2xl font-bold text-white mt-0.5">
                {records.filter((r) => r.method === 'QR_SCAN').length}
              </h3>
            </div>
          </Card>
        </div>

        {/* Attendance Ledger Table */}
        {loading ? (
          <LoadingSpinner message="Retrieving attendance history..." />
        ) : records.length === 0 ? (
          <EmptyState
            icon={History}
            title="No attendance records found"
            description="You haven't checked into any events yet. Once you attend and scan the organizer's QR code, your record will appear here."
          />
        ) : (
          <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/80 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                  <tr>
                    <th className="px-5 py-3.5">Event Name</th>
                    <th className="px-5 py-3.5">Check-in Timestamp</th>
                    <th className="px-5 py-3.5">Verification Method</th>
                    <th className="px-5 py-3.5">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {records.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="px-5 py-4 font-semibold text-white">
                        {item.eventTitle}
                      </td>
                      <td className="px-5 py-4 font-mono text-slate-400">
                        {item.checkInTime ? (
                          <>
                            {new Date(item.checkInTime).toLocaleDateString()} at{' '}
                            {new Date(item.checkInTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </>
                        ) : '—'}
                      </td>
                      <td className="px-5 py-4">
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-950 border border-slate-800 font-medium">
                          {item.method === 'QR_SCAN' ? (
                            <>
                              <QrCode className="w-3.5 h-3.5 text-indigo-400" />
                              <span>QR Code Scan</span>
                            </>
                          ) : (
                            <>
                              <UserCheck className="w-3.5 h-3.5 text-amber-400" />
                              <span>Manual Entry</span>
                            </>
                          )}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <Badge variant={item.status === 'PRESENT' ? 'success' : 'danger'}>
                          {item.status}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
