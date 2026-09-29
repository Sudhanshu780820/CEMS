import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { GraduationCap, UserPlus, Mail, Lock, User, Hash, Phone, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Button from '../../components/common/Button';
import PublicNavbar from '../../components/layout/PublicNavbar';

export default function RegisterPage() {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    enrollmentId: '',
    branch: 'CSE',
    academicYear: '2024-2028',
    currentYear: '2nd Year',
    currentSemester: 'Sem 3',
    section: 'A',
    phoneNumber: '',
  });

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [registeredSuccess, setRegisteredSuccess] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setErrorMessage('');

    try {
      await register(formData);
      toast.success('Registration submitted! Awaiting administrator approval.');
      setRegisteredSuccess(true);
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed. Check details.';
      setErrorMessage(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const branchOptions = [
    { value: 'CSE', label: 'Computer Science & Engineering (CSE)' },
    { value: 'IT', label: 'Information Technology (IT)' },
    { value: 'AI/ML', label: 'Artificial Intelligence & Machine Learning (AI/ML)' },
    { value: 'ECE', label: 'Electronics & Communication (ECE)' },
    { value: 'ME', label: 'Mechanical Engineering (ME)' },
    { value: 'Civil', label: 'Civil Engineering' },
  ];

  const yearOptions = [
    { value: '1st Year', label: '1st Year' },
    { value: '2nd Year', label: '2nd Year' },
    { value: '3rd Year', label: '3rd Year' },
    { value: '4th Year', label: '4th Year' },
  ];

  const semesterOptions = [
    { value: 'Sem 1', label: 'Semester 1' },
    { value: 'Sem 2', label: 'Semester 2' },
    { value: 'Sem 3', label: 'Semester 3' },
    { value: 'Sem 4', label: 'Semester 4' },
    { value: 'Sem 5', label: 'Semester 5' },
    { value: 'Sem 6', label: 'Semester 6' },
    { value: 'Sem 7', label: 'Semester 7' },
    { value: 'Sem 8', label: 'Semester 8' },
  ];

  const sectionOptions = [
    { value: 'A', label: 'Section A' },
    { value: 'B', label: 'Section B' },
    { value: 'C', label: 'Section C' },
    { value: 'D', label: 'Section D' },
  ];

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      <PublicNavbar />

      <div className="flex-1 flex items-center justify-center p-4 sm:p-6 lg:p-8">
        <div className="max-w-2xl w-full space-y-6">
          {/* Header */}
          <div className="text-center space-y-2">
            <div className="inline-flex w-14 h-14 rounded-2xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400 items-center justify-center mb-2 shadow-inner">
              <GraduationCap className="w-8 h-8" />
            </div>
            <h2 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">
              Student Registration
            </h2>
            <p className="text-xs text-slate-400 max-w-md mx-auto">
              Create your college account. Accounts require administrative verification against campus enrollment records before login access is granted.
            </p>
          </div>

          {registeredSuccess ? (
            <div className="bg-slate-900 border border-emerald-800/80 rounded-2xl p-8 text-center space-y-4 shadow-2xl">
              <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-9 h-9" />
              </div>
              <h3 className="text-xl font-bold text-white">Registration Submitted!</h3>
              <p className="text-sm text-slate-300 max-w-md mx-auto leading-relaxed">
                Your profile for <span className="text-white font-semibold">{formData.fullName}</span> (
                <span className="font-mono text-indigo-400">{formData.enrollmentId}</span>) has been saved with status:{' '}
                <span className="inline-block px-2 py-0.5 rounded-full bg-amber-950 text-amber-300 border border-amber-800 text-xs font-bold">
                  PENDING APPROVAL
                </span>
              </p>
              <p className="text-xs text-slate-400 max-w-md mx-auto">
                The college administrator will review your enrollment credentials. Once approved, you will be able to log in with your email or enrollment ID.
              </p>
              <div className="pt-4">
                <Button variant="primary" onClick={() => navigate('/login?registered=true')}>
                  Go to Sign In
                </Button>
              </div>
            </div>
          ) : (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-2xl shadow-black/40">
              {errorMessage && (
                <div className="mb-5 p-3.5 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-200 text-xs flex items-start gap-2.5">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
                  <span>{errorMessage}</span>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="Full Name"
                    placeholder="e.g. Rahul Sharma"
                    value={formData.fullName}
                    onChange={(e) => handleChange('fullName', e.target.value)}
                    icon={User}
                    required
                  />

                  <Input
                    label="Enrollment ID"
                    placeholder="e.g. EN2024CSE042"
                    value={formData.enrollmentId}
                    onChange={(e) => handleChange('enrollmentId', e.target.value.toUpperCase())}
                    icon={Hash}
                    helperText="College unique student identification"
                    required
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="College Email"
                    type="email"
                    placeholder="name@college.edu"
                    value={formData.email}
                    onChange={(e) => handleChange('email', e.target.value)}
                    icon={Mail}
                    required
                  />

                  <Input
                    label="Password"
                    type="password"
                    placeholder="Minimum 6 characters"
                    value={formData.password}
                    onChange={(e) => handleChange('password', e.target.value)}
                    icon={Lock}
                    required
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Select
                    label="Branch / Major"
                    options={branchOptions}
                    value={formData.branch}
                    onChange={(e) => handleChange('branch', e.target.value)}
                  />

                  <Input
                    label="Academic Year Cohort"
                    placeholder="e.g. 2024-2028"
                    value={formData.academicYear}
                    onChange={(e) => handleChange('academicYear', e.target.value)}
                    required
                  />
                </div>

                <div className="grid grid-cols-3 gap-3">
                  <Select
                    label="Current Year"
                    options={yearOptions}
                    value={formData.currentYear}
                    onChange={(e) => handleChange('currentYear', e.target.value)}
                  />

                  <Select
                    label="Current Semester"
                    options={semesterOptions}
                    value={formData.currentSemester}
                    onChange={(e) => handleChange('currentSemester', e.target.value)}
                  />

                  <Select
                    label="Section"
                    options={sectionOptions}
                    value={formData.section}
                    onChange={(e) => handleChange('section', e.target.value)}
                  />
                </div>

                <Input
                  label="Phone Number (Optional)"
                  type="tel"
                  placeholder="+91 98765 43210"
                  value={formData.phoneNumber}
                  onChange={(e) => handleChange('phoneNumber', e.target.value)}
                  icon={Phone}
                />

                <div className="pt-2">
                  <Button
                    type="submit"
                    variant="primary"
                    className="w-full"
                    size="md"
                    loading={loading}
                    icon={UserPlus}
                  >
                    Submit Registration for Approval
                  </Button>
                </div>
              </form>

              <div className="mt-6 text-center text-xs text-slate-400">
                Already registered?{' '}
                <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-semibold underline-offset-4 hover:underline">
                  Sign in here
                </Link>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
