import React, { useState, useEffect } from 'react';
import { Building2, Plus, Edit, Trash2, Users, MapPin, CheckCircle2, Navigation, Compass } from 'lucide-react';
import api from '../../api/client';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import VenueMapPicker, { formatCoordinates } from '../../components/common/VenueMapPicker';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';

export default function VenueManagementPage() {
  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVenue, setEditingVenue] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const { user, isAdmin } = useAuth();
  const toast = useToast();

  const [formData, setFormData] = useState({
    name: '',
    building: '',
    roomNumber: '',
    capacity: 100,
    description: '',
    latitude: null,
    longitude: null,
  });

  const apiBasePath = isAdmin ? '/admin/venues' : '/organizer/venues';

  const fetchVenues = async () => {
    setLoading(true);
    try {
      const res = await api.get(apiBasePath);
      setVenues(res.data || []);
    } catch (err) {
      toast.error('Failed to load campus venues');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVenues();
  }, [isAdmin]);

  const openAddModal = () => {
    setEditingVenue(null);
    setFormData({
      name: '',
      building: '',
      roomNumber: '',
      capacity: 100,
      description: '',
      latitude: null,
      longitude: null,
    });
    setModalOpen(true);
  };

  const openEditModal = (venue) => {
    setEditingVenue(venue);
    setFormData({
      name: venue.name,
      building: venue.building,
      roomNumber: venue.roomNumber,
      capacity: venue.capacity,
      description: venue.description || '',
      latitude: venue.latitude ?? null,
      longitude: venue.longitude ?? null,
    });
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      if (editingVenue) {
        await api.put(`${apiBasePath}/${editingVenue.id}`, formData);
        toast.success('Venue updated successfully with geofencing details');
      } else {
        await api.post(apiBasePath, formData);
        toast.success('Campus venue added successfully with geofencing perimeter');
      }
      setModalOpen(false);
      fetchVenues();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save venue');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Are you sure you want to deactivate venue "${name}"? Existing events will be preserved.`)) {
      return;
    }

    try {
      await api.delete(`${apiBasePath}/${id}`);
      toast.info('Venue deactivated');
      fetchVenues();
    } catch (err) {
      toast.error('Failed to deactivate venue');
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white">Campus Venues & Facilities</h1>
            <p className="text-xs text-slate-400">
              Manage auditoriums, halls, seating capacities, and 300m GPS attendance geofencing perimeters
            </p>
          </div>

          <Button variant="primary" icon={Plus} onClick={openAddModal}>
            Add New Venue
          </Button>
        </div>

        {loading ? (
          <LoadingSpinner message="Loading campus facilities and coordinates..." />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {venues.map((venue) => (
              <Card key={venue.id} className="flex flex-col justify-between space-y-4">
                <div className="space-y-3">
                  <div className="flex items-start justify-between">
                    <span className="text-xs font-mono font-bold text-indigo-400 bg-indigo-950/80 border border-indigo-800/60 px-2.5 py-0.5 rounded-lg">
                      {venue.roomNumber}
                    </span>
                    <Badge variant={venue.active ? 'success' : 'default'} size="sm">
                      {venue.active ? 'Active' : 'Deactivated'}
                    </Badge>
                  </div>

                  <h3 className="text-base font-bold text-white">{venue.name}</h3>

                  <div className="space-y-2 text-xs text-slate-300 pt-1">
                    <div className="flex items-center gap-1.5 text-slate-400">
                      <Building2 className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                      <span>{venue.building}</span>
                    </div>

                    <div className="flex items-center gap-1.5 text-slate-400">
                      <Users className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                      <span>Physical Capacity: <strong className="text-white">{venue.capacity} seats</strong></span>
                    </div>

                    {/* Geofence Coordinate Status Badge */}
                    {venue.latitude != null && venue.longitude != null ? (
                      <div className="p-2 rounded-xl bg-emerald-950/30 border border-emerald-800/50 space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="text-[11px] font-semibold text-emerald-400 flex items-center gap-1">
                            <Compass className="w-3.5 h-3.5" />
                            300m Geofence Active
                          </span>
                          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                        </div>
                        <p className="font-mono text-[11px] text-slate-300 truncate">
                          {formatCoordinates(venue.latitude, venue.longitude)}
                        </p>
                      </div>
                    ) : (
                      <div className="p-2 rounded-xl bg-amber-950/30 border border-amber-800/50 space-y-0.5">
                        <span className="text-[11px] font-semibold text-amber-400 flex items-center gap-1">
                          <MapPin className="w-3.5 h-3.5" />
                          No GPS Configured
                        </span>
                        <p className="text-[10px] text-amber-300/80">
                          Configure coordinates to enable 300m student QR attendance check-in.
                        </p>
                      </div>
                    )}

                    {venue.description && (
                      <p className="text-xs text-slate-400 line-clamp-2 pt-1 border-t border-slate-800/80">
                        {venue.description}
                      </p>
                    )}
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-800 flex items-center justify-end gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    icon={Edit}
                    onClick={() => openEditModal(venue)}
                  >
                    Edit
                  </Button>

                  {venue.active && isAdmin && (
                    <Button
                      variant="danger"
                      size="sm"
                      icon={Trash2}
                      onClick={() => handleDelete(venue.id, venue.name)}
                    >
                      Deactivate
                    </Button>
                  )}
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Add / Edit Venue Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingVenue ? 'Edit Venue & GPS Geofence' : 'Add Campus Venue & GPS Geofence'}
        maxWidth="max-w-2xl"
      >
        <form onSubmit={handleSave} className="space-y-4">
          <Input
            label="Venue Name"
            placeholder="e.g. APJ Abdul Kalam Auditorium"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Building Block"
              placeholder="e.g. Academic Block A"
              value={formData.building}
              onChange={(e) => setFormData({ ...formData, building: e.target.value })}
              required
            />

            <Input
              label="Room Number / Code"
              placeholder="e.g. AUD-101"
              value={formData.roomNumber}
              onChange={(e) => setFormData({ ...formData, roomNumber: e.target.value })}
              required
            />
          </div>

          <Input
            label="Seating Capacity"
            type="number"
            min={1}
            value={formData.capacity}
            onChange={(e) => setFormData({ ...formData, capacity: parseInt(e.target.value) || 0 })}
            helperText="Hard ceiling for event registration limits"
            required
          />

          {/* Interactive Map & Geofence Picker */}
          <div className="space-y-1.5 pt-1">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
              Venue GPS Coordinates & 300m Attendance Perimeter
            </label>
            <VenueMapPicker
              latitude={formData.latitude}
              longitude={formData.longitude}
              onChange={({ latitude, longitude }) =>
                setFormData((prev) => ({ ...prev, latitude, longitude }))
              }
              height="280px"
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
              Description / Facilities
            </label>
            <textarea
              rows={2}
              placeholder="Projector, audio system, tier seating, lab workstations..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full rounded-xl bg-slate-950 border border-slate-800 p-2.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
            <Button variant="secondary" size="sm" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" loading={submitting}>
              {editingVenue ? 'Save Changes' : 'Create Venue'}
            </Button>
          </div>
        </form>
      </Modal>
    </DashboardLayout>
  );
}
