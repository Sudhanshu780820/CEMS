import React from 'react';
import { User, Mail, Hash, BookOpen, Calendar, ShieldCheck, Phone } from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import { useAuth } from '../../context/AuthContext';

export default function StudentProfilePage() {
  const { user } = useAuth();

  return (
    <DashboardLayout>
      <div className="max-w-3xl space-y-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">Student Profile</h1>
          <p className="text-xs text-slate-400">
            Your verified academic enrollment credentials and campus standing
          </p>
        </div>

        <Card className="space-y-6">
          {/* Header Card Profile Banner */}
          <div className="flex items-center gap-4 pb-6 border-b border-slate-800">
            <div className="w-16 h-16 rounded-2xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center font-bold text-xl">
              {user?.name ? user.name.charAt(0).toUpperCase() : 'S'}
            </div>
            <div className="space-y-1">
              <h2 className="text-lg font-bold text-white">{user?.name}</h2>
              <div className="flex items-center gap-2">
                <Badge variant="success" size="sm">
                  {user?.studentStatus || 'APPROVED'}
                </Badge>
                <span className="text-xs text-slate-400 font-mono">
                  {user?.email}
                </span>
              </div>
            </div>
          </div>

          {/* Academic Details Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 space-y-1">
              <p className="text-slate-500 uppercase tracking-wider font-semibold">Enrollment ID</p>
              <p className="text-sm font-bold font-mono text-indigo-300">{user?.enrollmentId || 'N/A'}</p>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 space-y-1">
              <p className="text-slate-500 uppercase tracking-wider font-semibold">Branch / Department</p>
              <p className="text-sm font-bold text-white">{user?.branch || 'N/A'}</p>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 space-y-1">
              <p className="text-slate-500 uppercase tracking-wider font-semibold">Account Role</p>
              <p className="text-sm font-bold text-white">Verified Student</p>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 space-y-1">
              <p className="text-slate-500 uppercase tracking-wider font-semibold">Administrative Standing</p>
              <p className="text-sm font-bold text-emerald-400">Verified by Administration</p>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-indigo-950/30 border border-indigo-900/40 text-xs text-indigo-300 leading-relaxed">
            <ShieldCheck className="w-4 h-4 inline mr-1.5 text-indigo-400" />
            Your branch and enrollment status are synchronized with the central college database. Eligibility for technical workshops, hackathons, and department seminars is determined based on these verified records.
          </div>
        </Card>
      </div>
    </DashboardLayout>
  );
}
