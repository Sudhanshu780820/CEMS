import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save, AlertCircle } from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../context/ToastContext';

export default function EditEventPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: 'Workshop',
    eventDate: '',
    startTime: '',
    endTime: '',
    venueId: '',
    maxCapacity: 50,
    registrationStartDate: '',
    registrationEndDate: '',
    eligibleBranches: '',
    eligibleAcademicYears: '',
    eligibleSections: '',
    eventImageUrl: '',
    status: 'PUBLISHED',
  });

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const [venuesRes, eventRes] = await Promise.all([
          api.get('/venues/available'),
          api.get(`/events/${id}`)
        ]);
        setVenues(venuesRes.data || []);
        const evt = eventRes.data;
        setFormData({
          title: evt.title || '',
          description: evt.description || '',
          category: evt.category || 'Workshop',
          eventDate: evt.eventDate || '',
          startTime: evt.startTime?.substring(0, 5) || '',
          endTime: evt.endTime?.substring(0, 5) || '',
          venueId: evt.venue?.id?.toString() || '',
          maxCapacity: evt.maxCapacity || 50,
          registrationStartDate: evt.registrationStartDate || '',
          registrationEndDate: evt.registrationEndDate || '',
          eligibleBranches: evt.eligibleBranches || '',
          eligibleAcademicYears: evt.eligibleAcademicYears || '',
          eligibleSections: evt.eligibleSections || '',
          eventImageUrl: evt.eventImageUrl || '',
          status: evt.status || 'PUBLISHED',
        });
      } catch (err) {
        toast.error('Failed to load event data');
        navigate('/organizer/events');
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [id]);

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMessage('');

    try {
      const payload = {
        ...formData,
        venueId: parseInt(formData.venueId),
        maxCapacity: parseInt(formData.maxCapacity),
        startTime: formData.startTime.length === 5 ? `${formData.startTime}:00` : formData.startTime,
        endTime: formData.endTime.length === 5 ? `${formData.endTime}:00` : formData.endTime,
      };

      await api.put(`/organizer/events/${id}`, payload);
      toast.success('Event updated successfully!');
      navigate('/organizer/events');
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update event.';
      setErrorMessage(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <DashboardLayout>
        <LoadingSpinner message="Loading event configuration..." />
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="max-w-4xl space-y-6">
        <Link
          to="/organizer/events"
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to events
        </Link>

        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">Edit Event Details</h1>
          <p className="text-xs text-slate-400">
            Updating the schedule or venue will automatically notify all registered participants
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
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="sm:col-span-2">
                <Input
                  label="Event Title"
                  value={formData.title}
                  onChange={(e) => handleChange('title', e.target.value)}
                  required
                />
              </div>

              <Select
                label="Category"
                options={[
                  { value: 'Workshop', label: 'Workshop' },
                  { value: 'Technical', label: 'Technical' },
                  { value: 'Seminar', label: 'Seminar' },
                  { value: 'Cultural', label: 'Cultural' },
                  { value: 'Sports', label: 'Sports' },
                ]}
                value={formData.category}
                onChange={(e) => handleChange('category', e.target.value)}
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Description
              </label>
              <textarea
                rows={3}
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                className="block w-full rounded-xl bg-slate-950/70 border border-slate-800 p-3 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>

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
                  Campus Venue
                </label>
                <select
                  value={formData.venueId}
                  onChange={(e) => handleChange('venueId', e.target.value)}
                  className="block w-full rounded-xl bg-slate-950/70 border border-slate-800 text-sm text-slate-100 p-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                >
                  {venues.map((v) => (
                    <option key={v.id} value={v.id} className="bg-slate-900">
                      {v.name} ({v.building} - Capacity: {v.capacity})
                    </option>
                  ))}
                </select>
              </div>

              <Input
                label="Max Seat Capacity"
                type="number"
                min={1}
                value={formData.maxCapacity}
                onChange={(e) => handleChange('maxCapacity', e.target.value)}
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Registration Start Date"
                type="date"
                value={formData.registrationStartDate}
                onChange={(e) => handleChange('registrationStartDate', e.target.value)}
                required
              />

              <Input
                label="Registration End Date"
                type="date"
                value={formData.registrationEndDate}
                onChange={(e) => handleChange('registrationEndDate', e.target.value)}
                required
              />
            </div>

            <Input
              label="Eligible Branches (Comma-separated or empty for ALL)"
              placeholder="e.g. CSE, IT, AI/ML"
              value={formData.eligibleBranches}
              onChange={(e) => handleChange('eligibleBranches', e.target.value)}
            />

            <Input
              label="Eligible Academic Years (Comma-separated)"
              placeholder="e.g. 2nd Year, 3rd Year"
              value={formData.eligibleAcademicYears}
              onChange={(e) => handleChange('eligibleAcademicYears', e.target.value)}
            />

            <Input
              label="Banner Image URL"
              value={formData.eventImageUrl}
              onChange={(e) => handleChange('eventImageUrl', e.target.value)}
            />

            <Select
              label="Event Status"
              options={[
                { value: 'PUBLISHED', label: 'Published' },
                { value: 'COMPLETED', label: 'Completed' },
                { value: 'CANCELLED', label: 'Cancelled' },
              ]}
              value={formData.status}
              onChange={(e) => handleChange('status', e.target.value)}
            />
          </Card>

          <div className="flex items-center justify-end gap-3">
            <Link to="/organizer/events">
              <Button variant="secondary">Cancel</Button>
            </Link>
            <Button type="submit" variant="primary" loading={submitting} icon={Save}>
              Save & Update Event
            </Button>
          </div>
        </form>
      </div>
    </DashboardLayout>
  );
}
