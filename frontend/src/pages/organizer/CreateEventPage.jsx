import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  CalendarPlus,
  ArrowLeft,
  Calendar,
  Clock,
  MapPin,
  Users,
  Image,
  AlertCircle,
  Sparkles
} from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Button from '../../components/common/Button';
import { useToast } from '../../context/ToastContext';

export default function CreateEventPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [venues, setVenues] = useState([]);
  const [loadingVenues, setLoadingVenues] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: 'Workshop',
    eventDate: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0],
    startTime: '10:00',
    endTime: '13:00',
    venueId: '',
    maxCapacity: 50,
    registrationStartDate: new Date().toISOString().split('T')[0],
    registrationEndDate: new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0],
    eligibleBranches: [],
    eligibleAcademicYears: [],
    eligibleSections: [],
    eventImageUrl: 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80',
  });

  useEffect(() => {
    const fetchVenues = async () => {
      try {
        const res = await api.get('/venues/available');
        setVenues(res.data || []);
        if (res.data?.length > 0) {
          setFormData((prev) => ({
            ...prev,
            venueId: res.data[0].id.toString(),
            maxCapacity: Math.min(prev.maxCapacity, res.data[0].capacity),
          }));
        }
      } catch (err) {
        toast.error('Failed to load campus venues');
      } finally {
        setLoadingVenues(false);
      }
    };
    fetchVenues();
  }, []);

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const toggleBranch = (branch) => {
    setFormData((prev) => {
      const exists = prev.eligibleBranches.includes(branch);
      return {
        ...prev,
        eligibleBranches: exists
          ? prev.eligibleBranches.filter((b) => b !== branch)
          : [...prev.eligibleBranches, branch],
      };
    });
  };

  const toggleYear = (year) => {
    setFormData((prev) => {
      const exists = prev.eligibleAcademicYears.includes(year);
      return {
        ...prev,
        eligibleAcademicYears: exists
          ? prev.eligibleAcademicYears.filter((y) => y !== year)
          : [...prev.eligibleAcademicYears, year],
      };
    });
  };

  const toggleSection = (sec) => {
    setFormData((prev) => {
      const exists = prev.eligibleSections.includes(sec);
      return {
        ...prev,
        eligibleSections: exists
          ? prev.eligibleSections.filter((s) => s !== sec)
          : [...prev.eligibleSections, sec],
      };
    });
  };

  const selectedVenue = venues.find((v) => v.id.toString() === formData.venueId.toString());

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMessage('');

    if (selectedVenue && formData.maxCapacity > selectedVenue.capacity) {
      setErrorMessage(`Max capacity cannot exceed venue capacity (${selectedVenue.capacity} seats).`);
      setSubmitting(false);
      return;
    }

    try {
      const payload = {
        ...formData,
        venueId: parseInt(formData.venueId),
        maxCapacity: parseInt(formData.maxCapacity),
        startTime: formData.startTime.length === 5 ? `${formData.startTime}:00` : formData.startTime,
        endTime: formData.endTime.length === 5 ? `${formData.endTime}:00` : formData.endTime,
        eligibleBranches: formData.eligibleBranches.join(','),
        eligibleAcademicYears: formData.eligibleAcademicYears.join(','),
        eligibleSections: formData.eligibleSections.join(','),
      };

      await api.post('/organizer/events', payload);
      toast.success('Event successfully scheduled!');
      navigate('/organizer/events');
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to create event. Check schedule conflicts.';
      setErrorMessage(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const branchList = ['CSE', 'IT', 'AI/ML', 'ECE', 'ME', 'Civil'];
  const yearList = ['1st Year', '2nd Year', '3rd Year', '4th Year'];
  const sectionList = ['A', 'B', 'C', 'D'];

  const imagePresets = [
    { label: 'Technical / AI', url: 'https://images.unsplash.com/photo-1677442136019-21780efad99a?auto=format&fit=crop&w=1200&q=80' },
    { label: 'Hackathon / Code', url: 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80' },
    { label: 'Robotics', url: 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=1200&q=80' },
    { label: 'Cultural Fest', url: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80' },
    { label: 'Cloud / Seminar', url: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80' }
  ];

  return (
    <DashboardLayout>
      <div className="max-w-4xl space-y-6">
        <Link
          to="/organizer/events"
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to my events
        </Link>

        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">Create New Event</h1>
          <p className="text-xs text-slate-400">
            Configure schedule, assign campus venues, define eligibility constraints, and open registrations
          </p>
        </div>

        {errorMessage && (
          <div className="p-4 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-200 text-xs flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <Card className="space-y-4">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider text-indigo-400 border-b border-slate-800 pb-2">
              1. Event Basics
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="sm:col-span-2">
                <Input
                  label="Event Title"
                  placeholder="e.g. NextGen Web & AI Symposium"
                  value={formData.title}
                  onChange={(e) => handleChange('title', e.target.value)}
                  required
                />
              </div>

              <Select
                label="Event Category"
                options={[
                  { value: 'Workshop', label: 'Workshop' },
                  { value: 'Technical', label: 'Technical Hackathon' },
                  { value: 'Seminar', label: 'Seminar / Lecture' },
                  { value: 'Cultural', label: 'Cultural / Fest' },
                  { value: 'Sports', label: 'Sports Meet' },
                ]}
                value={formData.category}
                onChange={(e) => handleChange('category', e.target.value)}
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Event Description
              </label>
              <textarea
                rows={3}
                placeholder="Detailed schedule, speaker bio, instructions for participants..."
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                className="block w-full rounded-xl bg-slate-950/70 border border-slate-800 p-3 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                required
              />
            </div>
          </Card>

          <Card className="space-y-4">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider text-indigo-400 border-b border-slate-800 pb-2">
              2. Venue & Scheduling
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <Input
                label="Event Date"
                type="date"
                value={formData.eventDate}
                onChange={(e) => handleChange('eventDate', e.target.value)}
                required
              />

              <Input
                label="Start Time"
                type="time"
                value={formData.startTime}
                onChange={(e) => handleChange('startTime', e.target.value)}
                required
              />

              <Input
                label="End Time"
                type="time"
                value={formData.endTime}
                onChange={(e) => handleChange('endTime', e.target.value)}
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                  Campus Venue (With Collision Detection)
                </label>
                <select
                  value={formData.venueId}
                  onChange={(e) => handleChange('venueId', e.target.value)}
                  className="block w-full rounded-xl bg-slate-950/70 border border-slate-800 text-sm text-slate-100 p-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                >
                  {venues.map((v) => (
                    <option key={v.id} value={v.id} className="bg-slate-900">
                      {v.name} ({v.building} - Cap: {v.capacity})
                    </option>
                  ))}
                </select>
                {selectedVenue && (
                  <p className="text-[11px] text-slate-400 mt-1">
                    Selected hall capacity: <strong className="text-white">{selectedVenue.capacity} seats</strong>
                  </p>
                )}
              </div>

              <Input
                label="Max Seat Capacity"
                type="number"
                min={1}
                max={selectedVenue?.capacity || 500}
                value={formData.maxCapacity}
                onChange={(e) => handleChange('maxCapacity', e.target.value)}
                helperText={`Cannot exceed ${selectedVenue?.capacity || 'venue'} seats`}
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
              <Input
                label="Registration Opens"
                type="date"
                value={formData.registrationStartDate}
                onChange={(e) => handleChange('registrationStartDate', e.target.value)}
                required
              />

              <Input
                label="Registration Closes"
                type="date"
                value={formData.registrationEndDate}
                onChange={(e) => handleChange('registrationEndDate', e.target.value)}
                required
              />
            </div>
          </Card>

          <Card className="space-y-4">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider text-indigo-400 border-b border-slate-800 pb-2">
              3. Student Eligibility Restrictions
            </h3>
            <p className="text-xs text-slate-400">
              Leave all unselected to make this event open to ALL students across the entire institution.
            </p>

            {/* Branches Selection */}
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Eligible Branches ({formData.eligibleBranches.length === 0 ? 'All Branches' : formData.eligibleBranches.join(', ')})
              </p>
              <div className="flex flex-wrap gap-2">
                {branchList.map((b) => {
                  const selected = formData.eligibleBranches.includes(b);
                  return (
                    <button
                      key={b}
                      type="button"
                      onClick={() => toggleBranch(b)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                        selected
                          ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                          : 'bg-slate-950 border border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                      }`}
                    >
                      {b}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Academic Years Selection */}
            <div className="pt-2">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Eligible Years ({formData.eligibleAcademicYears.length === 0 ? 'All Years' : formData.eligibleAcademicYears.join(', ')})
              </p>
              <div className="flex flex-wrap gap-2">
                {yearList.map((y) => {
                  const selected = formData.eligibleAcademicYears.includes(y);
                  return (
                    <button
                      key={y}
                      type="button"
                      onClick={() => toggleYear(y)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                        selected
                          ? 'bg-purple-600 text-white shadow-md shadow-purple-600/20'
                          : 'bg-slate-950 border border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                      }`}
                    >
                      {y}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Sections Selection */}
            <div className="pt-2">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Eligible Sections ({formData.eligibleSections.length === 0 ? 'All Sections' : formData.eligibleSections.join(', ')})
              </p>
              <div className="flex flex-wrap gap-2">
                {sectionList.map((s) => {
                  const selected = formData.eligibleSections.includes(s);
                  return (
                    <button
                      key={s}
                      type="button"
                      onClick={() => toggleSection(s)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                        selected
                          ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/20'
                          : 'bg-slate-950 border border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                      }`}
                    >
                      Section {s}
                    </button>
                  );
                })}
              </div>
            </div>
          </Card>

          <Card className="space-y-4">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider text-indigo-400 border-b border-slate-800 pb-2">
              4. Event Banner Image
            </h3>

            <Input
              label="Image URL"
              placeholder="https://images.unsplash.com/..."
              value={formData.eventImageUrl}
              onChange={(e) => handleChange('eventImageUrl', e.target.value)}
            />

            <div>
              <p className="text-xs text-slate-400 mb-2">Or choose from presets:</p>
              <div className="flex flex-wrap gap-2">
                {imagePresets.map((p) => (
                  <button
                    key={p.label}
                    type="button"
                    onClick={() => handleChange('eventImageUrl', p.url)}
                    className="text-xs px-2.5 py-1 rounded-lg bg-slate-950 border border-slate-800 hover:border-slate-700 text-slate-300 hover:text-white cursor-pointer"
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            </div>
          </Card>

          <div className="flex items-center justify-end gap-3 pt-4">
            <Link to="/organizer/events">
              <Button variant="secondary">Cancel</Button>
            </Link>
            <Button
              type="submit"
              variant="primary"
              loading={submitting}
              icon={CalendarPlus}
            >
              Publish Event & Open Registrations
            </Button>
          </div>
        </form>
      </div>
    </DashboardLayout>
  );
}
