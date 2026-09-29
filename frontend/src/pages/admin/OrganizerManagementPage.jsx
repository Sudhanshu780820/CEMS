import React, { useState, useEffect } from 'react';
import { User, UserPlus, Mail, Building, Phone, Lock, CheckCircle2, XCircle } from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../context/ToastContext';

export default function OrganizerManagementPage() {
  const [organizers, setOrganizers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  const [formData, setFormData] = useState({
    name: '',
    department: '',
    email: '',
    password: '',
    phoneNumber: '',
  });

  const fetchOrganizers = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/organizers');
      setOrganizers(res.data || []);
    } catch (err) {
      toast.error('Failed to load organizers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrganizers();
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.post('/admin/organizers', formData);
      toast.success(`Organizer profile created for ${formData.name}`);
      setIsAddModalOpen(false);
      setFormData({ name: '', department: '', email: '', password: '', phoneNumber: '' });
      fetchOrganizers();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create organizer');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleStatus = async (id, name, currentActive) => {
    try {
      await api.put(`/admin/organizers/${id}/toggle-status`);
      toast.info(`Organizer account ${currentActive ? 'disabled' : 'enabled'}`);
      fetchOrganizers();
    } catch (err) {
      toast.error('Failed to toggle organizer status');
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white">Event Organizer Directory</h1>
            <p className="text-xs text-slate-400">
              Authorized departments, campus clubs, and society accounts with event creation privileges
            </p>
          </div>

          <Button
            variant="primary"
            icon={UserPlus}
            onClick={() => setIsAddModalOpen(true)}
          >
            Add New Organizer
          </Button>
        </div>

        {loading ? (
          <LoadingSpinner message="Loading organizer profiles..." />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {organizers.map((org) => (
              <Card key={org.id} className="flex flex-col justify-between space-y-4">
                <div className="space-y-2">
                  <div className="flex items-start justify-between">
                    <div className="w-10 h-10 rounded-xl bg-purple-600/20 text-purple-400 border border-purple-500/30 flex items-center justify-center font-bold text-sm">
                      {org.name.charAt(0)}
                    </div>
                    <Badge variant={org.active ? 'success' : 'danger'} size="sm">
                      {org.active ? 'Active' : 'Disabled'}
                    </Badge>
                  </div>

                  <h3 className="text-base font-bold text-white">{org.name}</h3>
                  <p className="text-xs text-indigo-400 font-medium">{org.department}</p>

                  <div className="space-y-1 text-xs text-slate-400 pt-2 border-t border-slate-800">
                    <p className="font-mono text-slate-300 truncate">{org.contactEmail}</p>
                    {org.phoneNumber && <p>{org.phoneNumber}</p>}
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-800 flex justify-between items-center text-xs">
                  <span className="text-slate-500">
                    Registered: {new Date(org.createdAt).toLocaleDateString()}
                  </span>

                  <button
                    onClick={() => handleToggleStatus(org.id, org.name, org.active)}
                    className={`font-semibold px-2.5 py-1 rounded-lg border transition-colors cursor-pointer ${
                      org.active
                        ? 'text-rose-400 border-rose-800/60 bg-rose-950/30 hover:bg-rose-950'
                        : 'text-emerald-400 border-emerald-800/60 bg-emerald-950/30 hover:bg-emerald-950'
                    }`}
                  >
                    {org.active ? 'Disable Account' : 'Enable Account'}
                  </button>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Add Organizer Modal */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        title="Add Event Organizer"
        maxWidth="max-w-md"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Department / Society Name"
            placeholder="e.g. Computer Science Dept"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            icon={Building}
            required
          />

          <Input
            label="Parent Department / Faculty"
            placeholder="e.g. School of Engineering"
            value={formData.department}
            onChange={(e) => setFormData({ ...formData, department: e.target.value })}
            required
          />

          <Input
            label="Official Contact Email"
            type="email"
            placeholder="dept.head@college.edu"
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            icon={Mail}
            required
          />

          <Input
            label="Temporary Password"
            type="password"
            placeholder="Minimum 6 characters"
            value={formData.password}
            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
            icon={Lock}
            required
          />

          <Input
            label="Phone Number"
            placeholder="+91 98765 43210"
            value={formData.phoneNumber}
            onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
            icon={Phone}
          />

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
            <Button variant="secondary" size="sm" onClick={() => setIsAddModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" loading={submitting}>
              Create Organizer
            </Button>
          </div>
        </form>
      </Modal>
    </DashboardLayout>
  );
}
