import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from '../components/common/LoadingSpinner';

// Public Pages
import LoginPage from '../pages/public/LoginPage';
import RegisterPage from '../pages/public/RegisterPage';
import PublicEventsPage from '../pages/public/PublicEventsPage';
import EventDetailPage from '../pages/public/EventDetailPage';

// Student Pages
import StudentDashboard from '../pages/student/StudentDashboard';
import StudentEventsPage from '../pages/student/StudentEventsPage';
import MyRegistrationsPage from '../pages/student/MyRegistrationsPage';
import AttendanceHistoryPage from '../pages/student/AttendanceHistoryPage';
import StudentProfilePage from '../pages/student/StudentProfilePage';

// Organizer Pages
import OrganizerDashboard from '../pages/organizer/OrganizerDashboard';
import MyEventsPage from '../pages/organizer/MyEventsPage';
import CreateEventPage from '../pages/organizer/CreateEventPage';
import EditEventPage from '../pages/organizer/EditEventPage';
import ParticipantsPage from '../pages/organizer/ParticipantsPage';
import AttendanceSessionPage from '../pages/organizer/AttendanceSessionPage';

// Admin Pages
import AdminDashboard from '../pages/admin/AdminDashboard';
import PendingApprovalsPage from '../pages/admin/PendingApprovalsPage';
import StudentManagementPage from '../pages/admin/StudentManagementPage';
import OrganizerManagementPage from '../pages/admin/OrganizerManagementPage';
import VenueManagementPage from '../pages/admin/VenueManagementPage';
import AdminEventManagementPage from '../pages/admin/AdminEventManagementPage';
import AnalyticsReportsPage from '../pages/admin/AnalyticsReportsPage';

// Role-protected route guard
function ProtectedRoute({ children, allowedRoles }) {
  const { user, isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center">
        <LoadingSpinner message="Verifying session credentials..." size="lg" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    // Redirect to respective authorized dashboard
    if (user?.role === 'ROLE_ADMIN') return <Navigate to="/admin/dashboard" replace />;
    if (user?.role === 'ROLE_ORGANIZER') return <Navigate to="/organizer/dashboard" replace />;
    return <Navigate to="/student/dashboard" replace />;
  }

  return children;
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public Pages */}
      <Route path="/" element={<Navigate to="/events" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/events" element={<PublicEventsPage />} />
      <Route path="/events/:id" element={<EventDetailPage />} />

      {/* Student Routes */}
      <Route
        path="/student/dashboard"
        element={
          <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
            <StudentDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/events"
        element={
          <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
            <StudentEventsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/events/:id"
        element={
          <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
            <EventDetailPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/registrations"
        element={
          <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
            <MyRegistrationsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/attendance"
        element={
          <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
            <AttendanceHistoryPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/profile"
        element={
          <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
            <StudentProfilePage />
          </ProtectedRoute>
        }
      />

      {/* Organizer Routes */}
      <Route
        path="/organizer/dashboard"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ORGANIZER', 'ROLE_ADMIN']}>
            <OrganizerDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/organizer/events"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ORGANIZER', 'ROLE_ADMIN']}>
            <MyEventsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/organizer/events/create"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ORGANIZER', 'ROLE_ADMIN']}>
            <CreateEventPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/organizer/events/:id/edit"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ORGANIZER', 'ROLE_ADMIN']}>
            <EditEventPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/organizer/events/:id/participants"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ORGANIZER', 'ROLE_ADMIN']}>
            <ParticipantsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/organizer/events/:id/attendance"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ORGANIZER', 'ROLE_ADMIN']}>
            <AttendanceSessionPage />
          </ProtectedRoute>
        }
      />

      {/* Admin Routes */}
      <Route
        path="/admin/dashboard"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/students/pending"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <PendingApprovalsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/students"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <StudentManagementPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/organizers"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <OrganizerManagementPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/venues"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <VenueManagementPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/events"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <AdminEventManagementPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/reports"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <AnalyticsReportsPage />
          </ProtectedRoute>
        }
      />

      {/* Catch-all */}
      <Route path="*" element={<Navigate to="/events" replace />} />
    </Routes>
  );
}
