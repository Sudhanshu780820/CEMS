import React, { useState, useMemo } from 'react';
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
    startYear: '2026',
    endYear: '2030',
    currentYear: '1st Year',
    currentSemester: 'Semester 1',
    section: 'A',
    phoneNumber: '',
  });

  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [registeredSuccess, setRegisteredSuccess] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  // Static Start Year options: e.g. 2022 to 2032
  const startYearOptions = useMemo(() => {
    return Array.from({ length: 11 }, (_, i) => {
      const yr = String(2022 + i);
      return { value: yr, label: yr };
    });
  }, []);

  // Dynamic End Year options: strictly startYear + 1 to startYear + 5 (max 5 years gap)
  const endYearOptions = useMemo(() => {
    const start = parseInt(formData.startYear, 10);
    if (isNaN(start)) {
      return [{ value: '', label: 'Select Start Year first' }];
    }
    const options = [];
    for (let i = 1; i <= 5; i++) {
      const yr = String(start + i);
      options.push({ value: yr, label: yr });
    }
    return options;
  }, [formData.startYear]);

  // Session duration (in years)
  const sessionDuration = useMemo(() => {
    const start = parseInt(formData.startYear, 10);
    const end = parseInt(formData.endYear, 10);
    if (isNaN(start) || isNaN(end) || end <= start) return 0;
    return end - start;
  }, [formData.startYear, formData.endYear]);

  // Dynamic Current Year options based on session duration
  const currentYearOptions = useMemo(() => {
    if (sessionDuration <= 0) {
      return [{ value: '', label: 'Select valid session first' }];
    }
    const years = [];
    years.push({ value: '1st Year', label: '1st Year' });
    if (sessionDuration >= 2) years.push({ value: '2nd Year', label: '2nd Year' });
    if (sessionDuration >= 3) years.push({ value: '3rd Year', label: '3rd Year' });
    if (sessionDuration >= 4) years.push({ value: '4th Year', label: '4th Year' });
    return years;
  }, [sessionDuration]);

  // Dynamic Current Semester options based on Current Year
  const currentSemesterOptions = useMemo(() => {
    switch (formData.currentYear) {
      case '1st Year':
        return [
          { value: 'Semester 1', label: 'Semester 1' },
          { value: 'Semester 2', label: 'Semester 2' },
        ];
      case '2nd Year':
        return [
          { value: 'Semester 3', label: 'Semester 3' },
          { value: 'Semester 4', label: 'Semester 4' },
        ];
      case '3rd Year':
        return [
          { value: 'Semester 5', label: 'Semester 5' },
          { value: 'Semester 6', label: 'Semester 6' },
        ];
      case '4th Year':
        return [
          { value: 'Semester 7', label: 'Semester 7' },
          { value: 'Semester 8', label: 'Semester 8' },
        ];
      default:
        return [{ value: '', label: 'Select Current Year first' }];
    }
  }, [formData.currentYear]);

  const branchOptions = [
    { value: 'CSE', label: 'Computer Science & Engineering (CSE)' },
    { value: 'IT', label: 'Information Technology (IT)' },
    { value: 'AI/ML', label: 'Artificial Intelligence & Machine Learning (AI/ML)' },
    { value: 'ECE', label: 'Electronics & Communication (ECE)' },
    { value: 'ME', label: 'Mechanical Engineering (ME)' },
    { value: 'Civil', label: 'Civil Engineering' },
  ];

  const sectionOptions = [
    { value: 'A', label: 'Section A' },
    { value: 'B', label: 'Section B' },
    { value: 'C', label: 'Section C' },
    { value: 'D', label: 'Section D' },
  ];

  // Field validation rules
  const validateField = (field, value, currentData = formData) => {
    switch (field) {
      case 'fullName':
        if (!value || !value.trim()) return 'Full Name is required';
        return '';
      case 'enrollmentId':
        if (!value || !value.trim()) return 'Enrollment ID is required';
        return '';
      case 'email':
        if (!value || !value.trim()) return 'Email is required';
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())) return 'Email should be valid';
        return '';
      case 'password':
        if (!value) return 'Password is required';
        if (value.length < 6 || !/[a-zA-Z]/.test(value) || !/[0-9]/.test(value)) {
          return 'Password must contain at least one letter and one number.';
        }
        return '';
      case 'startYear':
        if (!value) return 'Start year is required';
        return '';
      case 'endYear': {
        if (!value) return 'End year is required';
        const start = parseInt(currentData.startYear, 10);
        const end = parseInt(value, 10);
        if (isNaN(start) || isNaN(end) || end <= start) {
          return 'End year must be after start year.';
        }
        const diff = end - start;
        if (diff < 1 || diff > 5) {
          return 'Academic session must be between 1 and 5 years.';
        }
        return '';
      }
      case 'currentYear':
        if (!value) return 'Current Year is required';
        return '';
      case 'currentSemester':
        if (!value) return 'Current Semester is required';
        return '';
      case 'branch':
        if (!value) return 'Branch is required';
        return '';
      case 'section':
        if (!value) return 'Section is required';
        return '';
      case 'phoneNumber':
        if (value && value.trim()) {
          if (!/^[0-9]{10}$/.test(value.trim())) {
            return 'Phone number must contain exactly 10 digits.';
          }
        }
        return '';
      default:
        return '';
    }
  };

  const handleBlur = (field) => {
    setTouched((prev) => ({ ...prev, [field]: true }));
    const errorMsg = validateField(field, formData[field]);
    setErrors((prev) => ({ ...prev, [field]: errorMsg }));
  };

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (touched[field] || errors[field]) {
      const errorMsg = validateField(field, value);
      setErrors((prev) => ({ ...prev, [field]: errorMsg }));
    }
  };

  // Cascading Start Year change
  const handleStartYearChange = (newStart) => {
    const start = parseInt(newStart, 10);
    let newEnd = formData.endYear;
    const end = parseInt(newEnd, 10);

    // If current endYear is outside [start + 1, start + 5], adjust or reset
    if (isNaN(start) || isNaN(end) || end <= start || end > start + 5) {
      newEnd = !isNaN(start) ? String(start + 4) : '';
    }

    const duration = parseInt(newEnd, 10) - start;
    let newYear = formData.currentYear;
    const maxYearNum = duration >= 4 ? 4 : duration;
    const currentYearNum = parseInt(newYear, 10) || 1;
    if (currentYearNum > maxYearNum) {
      newYear = '1st Year';
    }

    const newSem = newYear === '1st Year' ? 'Semester 1' : formData.currentSemester;

    setFormData((prev) => ({
      ...prev,
      startYear: newStart,
      endYear: newEnd,
      currentYear: newYear,
      currentSemester: newSem,
    }));

    setErrors((prev) => ({
      ...prev,
      startYear: '',
      endYear: '',
      currentYear: '',
      currentSemester: '',
    }));
  };

  // Cascading End Year change
  const handleEndYearChange = (newEnd) => {
    const start = parseInt(formData.startYear, 10);
    const end = parseInt(newEnd, 10);
    const duration = end - start;

    let newYear = formData.currentYear;
    const maxYearNum = duration >= 4 ? 4 : duration;
    const currentYearNum = parseInt(newYear, 10) || 1;
    if (currentYearNum > maxYearNum) {
      newYear = '1st Year';
    }

    const newSem = newYear === '1st Year' ? 'Semester 1' : formData.currentSemester;

    setFormData((prev) => ({
      ...prev,
      endYear: newEnd,
      currentYear: newYear,
      currentSemester: newSem,
    }));

    setErrors((prev) => ({
      ...prev,
      endYear: '',
      currentYear: '',
      currentSemester: '',
    }));
  };

  // Cascading Current Year change -> resets semester to valid default
  const handleCurrentYearChange = (newYear) => {
    let defaultSem = 'Semester 1';
    switch (newYear) {
      case '1st Year':
        defaultSem = 'Semester 1';
        break;
      case '2nd Year':
        defaultSem = 'Semester 3';
        break;
      case '3rd Year':
        defaultSem = 'Semester 5';
        break;
      case '4th Year':
        defaultSem = 'Semester 7';
        break;
      default:
        defaultSem = '';
    }

    setFormData((prev) => ({
      ...prev,
      currentYear: newYear,
      currentSemester: defaultSem,
    }));

    setErrors((prev) => ({
      ...prev,
      currentYear: '',
      currentSemester: '',
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    // Mark all fields as touched
    const allTouched = {};
    const validationErrors = {};

    Object.keys(formData).forEach((field) => {
      allTouched[field] = true;
      const errorMsg = validateField(field, formData[field]);
      if (errorMsg) {
        validationErrors[field] = errorMsg;
      }
    });

    setTouched(allTouched);
    setErrors(validationErrors);

    // Stop if any field has errors
    if (Object.keys(validationErrors).length > 0) {
      const firstError = Object.values(validationErrors)[0];
      setErrorMessage(firstError || 'Please fix the errors before submitting.');
      return;
    }

    setLoading(true);

    try {
      // Format payload for backend: academicYear as "startYear-endYear"
      const payload = {
        fullName: formData.fullName.trim(),
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
        enrollmentId: formData.enrollmentId.trim().toUpperCase(),
        branch: formData.branch,
        academicYear: `${formData.startYear}-${formData.endYear}`,
        currentYear: formData.currentYear,
        currentSemester: formData.currentSemester,
        section: formData.section,
        phoneNumber: formData.phoneNumber.trim() || undefined,
      };

      await register(payload);
      toast.success('Registration submitted! Awaiting administrator approval.');
      setRegisteredSuccess(true);
    } catch (err) {
      const resData = err.response?.data;
      const msg = resData?.message || 'Registration failed. Check details.';

      // Map backend field errors if present
      if (resData?.details && typeof resData.details === 'object') {
        setErrors((prev) => ({ ...prev, ...resData.details }));
      }

      setErrorMessage(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

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

              <form onSubmit={handleSubmit} className="space-y-4" noValidate>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="Full Name"
                    placeholder="e.g. Rahul Sharma"
                    value={formData.fullName}
                    onChange={(e) => handleChange('fullName', e.target.value)}
                    onBlur={() => handleBlur('fullName')}
                    error={errors.fullName}
                    icon={User}
                    required
                  />

                  <Input
                    label="Enrollment ID"
                    placeholder="e.g. EN2024CSE042"
                    value={formData.enrollmentId}
                    onChange={(e) => handleChange('enrollmentId', e.target.value.toUpperCase())}
                    onBlur={() => handleBlur('enrollmentId')}
                    error={errors.enrollmentId}
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
                    onBlur={() => handleBlur('email')}
                    error={errors.email}
                    icon={Mail}
                    required
                  />

                  <Input
                    label="Password"
                    type="password"
                    placeholder="Min 6 chars with letters & numbers"
                    value={formData.password}
                    onChange={(e) => handleChange('password', e.target.value)}
                    onBlur={() => handleBlur('password')}
                    error={errors.password}
                    helperText="At least 6 characters, including at least one letter and one number"
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
                    onBlur={() => handleBlur('branch')}
                    error={errors.branch}
                  />

                  <Select
                    label="Section"
                    options={sectionOptions}
                    value={formData.section}
                    onChange={(e) => handleChange('section', e.target.value)}
                    onBlur={() => handleBlur('section')}
                    error={errors.section}
                  />
                </div>

                {/* Academic Session: Start Year & End Year */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Select
                    label="Academic Session Start Year"
                    options={startYearOptions}
                    value={formData.startYear}
                    onChange={(e) => handleStartYearChange(e.target.value)}
                    onBlur={() => handleBlur('startYear')}
                    error={errors.startYear}
                    helperText="Starting year of your degree program"
                  />

                  <Select
                    label="Academic Session End Year"
                    options={endYearOptions}
                    value={formData.endYear}
                    onChange={(e) => handleEndYearChange(e.target.value)}
                    onBlur={() => handleBlur('endYear')}
                    error={errors.endYear}
                    helperText={
                      formData.startYear && formData.endYear && sessionDuration > 0
                        ? `Cohort: ${formData.startYear}-${formData.endYear} (${sessionDuration} ${sessionDuration === 1 ? 'year' : 'years'})`
                        : 'Between 1 and 5 years duration'
                    }
                  />
                </div>

                {/* Current Year & Current Semester */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Select
                    label="Current Year"
                    options={currentYearOptions}
                    value={formData.currentYear}
                    onChange={(e) => handleCurrentYearChange(e.target.value)}
                    onBlur={() => handleBlur('currentYear')}
                    error={errors.currentYear}
                    helperText="Determined by session duration"
                  />

                  <Select
                    label="Current Semester"
                    options={currentSemesterOptions}
                    value={formData.currentSemester}
                    onChange={(e) => handleChange('currentSemester', e.target.value)}
                    onBlur={() => handleBlur('currentSemester')}
                    error={errors.currentSemester}
                    helperText="Determined by current year"
                  />
                </div>

                <Input
                  label="Phone Number (Optional)"
                  type="tel"
                  placeholder="10-digit number e.g. 9876543210"
                  value={formData.phoneNumber}
                  onChange={(e) => handleChange('phoneNumber', e.target.value)}
                  onBlur={() => handleBlur('phoneNumber')}
                  error={errors.phoneNumber}
                  icon={Phone}
                  helperText="Optional: exactly 10 digits"
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

