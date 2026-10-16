import React, { useState, useEffect, useRef } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { Camera, QrCode, CheckCircle2, AlertCircle, KeyRound, MapPin, Navigation, ShieldCheck, Loader2 } from 'lucide-react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import Input from '../common/Input';
import api from '../../api/client';
import { useToast } from '../../context/ToastContext';

export default function QrScannerModal({ isOpen, onClose, event, onSuccess }) {
  const [scannedResult, setScannedResult] = useState(null);
  const [manualToken, setManualToken] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [locating, setLocating] = useState(false);
  const [statusText, setStatusText] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [cameraMode, setCameraMode] = useState(true);
  const scannerRef = useRef(null);
  const toast = useToast();

  // Helper to obtain browser GPS coordinates with promise
  const getDeviceCoordinates = () => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation is not supported by your browser or device.'));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        (error) => {
          let friendlyMsg = 'Failed to obtain GPS location.';
          if (error.code === error.PERMISSION_DENIED) {
            friendlyMsg =
              'Location access is required to verify your attendance. Please enable location permissions in your browser settings and try again.';
          } else if (error.code === error.POSITION_UNAVAILABLE) {
            friendlyMsg =
              'Unable to determine your precise GPS location. Please ensure device location is turned on and try again.';
          } else if (error.code === error.TIMEOUT) {
            friendlyMsg =
              'GPS location request timed out. Please ensure you have a clear GPS signal and try again.';
          }
          reject(new Error(friendlyMsg));
        },
        {
          enableHighAccuracy: true,
          timeout: 15000,
          maximumAge: 0,
        }
      );
    });
  };

  const handleAttendanceSubmit = async (token) => {
    if (!token || !event) return;

    setSubmitting(true);
    setLocating(true);
    setErrorMsg('');
    setStatusText('Acquiring device GPS coordinates for 300m campus verification...');

    try {
      // 1. Acquire participant GPS coordinates
      const coords = await getDeviceCoordinates();

      // 2. Transmit token + coordinates to backend
      setLocating(false);
      setStatusText('Verifying geofence and recording attendance...');

      const res = await api.post(`/events/${event.id}/attendance/mark`, {
        attendanceToken: token.trim(),
        latitude: coords.latitude,
        longitude: coords.longitude,
      });

      setScannedResult(res.data);
      toast.success('Attendance recorded successfully within campus geofence!');
      if (onSuccess) onSuccess(res.data);
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Failed to mark attendance. Please verify your location and try again.';
      setErrorMsg(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
      setLocating(false);
      setStatusText('');
    }
  };

  useEffect(() => {
    let scanner = null;

    if (isOpen && cameraMode && !scannedResult) {
      // Timeout to allow Modal DOM element to mount
      const timer = setTimeout(() => {
        const qrContainer = document.getElementById('qr-reader-container');
        if (qrContainer) {
          try {
            scanner = new Html5QrcodeScanner(
              'qr-reader-container',
              {
                fps: 10,
                qrbox: { width: 250, height: 250 },
                rememberLastUsedCamera: true,
                aspectRatio: 1.0,
              },
              false
            );

            scanner.render(
              (decodedText) => {
                scanner.clear();
                handleAttendanceSubmit(decodedText);
              },
              (err) => {
                // Ignore silent scan frame errors
              }
            );
            scannerRef.current = scanner;
          } catch (e) {
            console.warn('HTML5 Scanner initialization failed:', e);
          }
        }
      }, 300);

      return () => {
        clearTimeout(timer);
        if (scannerRef.current) {
          try {
            scannerRef.current.clear();
          } catch (e) {}
        }
      };
    }
  }, [isOpen, cameraMode, scannedResult]);

  const handleClose = () => {
    if (scannerRef.current) {
      try {
        scannerRef.current.clear();
      } catch (e) {}
    }
    setScannedResult(null);
    setManualToken('');
    setErrorMsg('');
    setStatusText('');
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Mark Event Attendance" maxWidth="max-w-md">
      <div className="space-y-4 text-center">
        {/* Event Banner */}
        <div className="p-3 bg-slate-950/70 border border-slate-800 rounded-xl text-left space-y-1">
          <p className="text-[11px] text-slate-400 font-medium">Scanning for:</p>
          <p className="text-sm font-bold text-white truncate">{event?.title}</p>
          <div className="flex items-center gap-1.5 text-xs text-indigo-400">
            <MapPin className="w-3.5 h-3.5 shrink-0" />
            <span className="truncate">{event?.venue?.name || 'Campus Venue'}</span>
          </div>
        </div>

        {/* 300m Geofence Notice Banner */}
        <div className="p-2.5 rounded-xl bg-indigo-950/40 border border-indigo-800/50 text-[11px] text-indigo-200 text-left flex items-start gap-2">
          <Navigation className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
          <div className="space-y-0.5">
            <span className="font-semibold text-white">300m GPS Geofence Required:</span>
            <p className="text-slate-300">
              You must be physically present within 300 meters of the venue. Browser location permission will be requested upon scanning.
            </p>
          </div>
        </div>

        {/* Success State */}
        {scannedResult ? (
          <div className="py-6 flex flex-col items-center gap-3">
            <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center">
              <CheckCircle2 className="w-9 h-9" />
            </div>
            <h4 className="text-lg font-bold text-white">Attendance Verified!</h4>
            <p className="text-xs text-slate-400 max-w-xs">
              Your location and check-in have been confirmed within the 300m geofence and marked{' '}
              <span className="text-emerald-400 font-semibold">PRESENT</span> on the college ledger.
            </p>
            <div className="mt-2 p-2.5 bg-slate-950 rounded-lg text-xs font-mono text-slate-300">
              Check-in Time: {new Date(scannedResult.checkInTime).toLocaleTimeString()}
            </div>
            <Button variant="secondary" size="sm" onClick={handleClose} className="mt-4">
              Close
            </Button>
          </div>
        ) : (
          <>
            {/* Mode Switcher */}
            <div className="flex rounded-xl bg-slate-950 p-1 border border-slate-800">
              <button
                type="button"
                onClick={() => setCameraMode(true)}
                disabled={submitting}
                className={`flex-1 py-1.5 text-xs font-semibold rounded-lg flex items-center justify-center gap-1.5 transition-colors ${
                  cameraMode ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                <Camera className="w-3.5 h-3.5" />
                Live Camera
              </button>
              <button
                type="button"
                onClick={() => setCameraMode(false)}
                disabled={submitting}
                className={`flex-1 py-1.5 text-xs font-semibold rounded-lg flex items-center justify-center gap-1.5 transition-colors ${
                  !cameraMode ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                <KeyRound className="w-3.5 h-3.5" />
                Enter Code
              </button>
            </div>

            {/* In-Flight Status Progress */}
            {statusText && (
              <div className="p-3 rounded-xl bg-indigo-950/60 border border-indigo-700/60 text-indigo-200 text-xs flex items-center gap-2.5 text-left animate-pulse">
                <Loader2 className="w-4 h-4 animate-spin text-indigo-400 shrink-0" />
                <span>{statusText}</span>
              </div>
            )}

            {/* Error Banner */}
            {errorMsg && (
              <div className="p-3 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-300 text-xs flex items-start gap-2 text-left">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
                <span className="leading-relaxed">{errorMsg}</span>
              </div>
            )}

            {/* Camera Scanner Container */}
            {cameraMode ? (
              <div className="relative">
                <div
                  id="qr-reader-container"
                  className="overflow-hidden rounded-xl bg-slate-950 border border-slate-800 min-h-[260px] text-slate-400"
                />
                <p className="text-[11px] text-slate-500 mt-2">
                  Point your camera directly at the organizer's active QR code
                </p>
              </div>
            ) : (
              <div className="space-y-4 py-2 text-left">
                <Input
                  label="Attendance Code / Token"
                  placeholder="Paste or enter organizer's token"
                  value={manualToken}
                  onChange={(e) => setManualToken(e.target.value)}
                  icon={QrCode}
                  disabled={submitting}
                />
                <Button
                  variant="primary"
                  className="w-full"
                  loading={submitting}
                  disabled={!manualToken.trim() || submitting}
                  onClick={() => handleAttendanceSubmit(manualToken)}
                >
                  Verify Attendance with GPS
                </Button>
              </div>
            )}

            {/* Privacy Assurance Footer */}
            <div className="pt-2 text-[10px] text-slate-500 flex items-center justify-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
              <span>Location coordinates are never stored, logged, or tracked.</span>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
}
