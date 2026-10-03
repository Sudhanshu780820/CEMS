import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { QrCode, Copy, Check, PowerOff, Users, RefreshCw } from 'lucide-react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import { useToast } from '../../context/ToastContext';
import api from '../../api/client';

export default function QrSessionModal({ isOpen, onClose, event, token, onSessionEnd }) {
  const [copied, setCopied] = useState(false);
  const [stopping, setStopping] = useState(false);
  const toast = useToast();

  const handleCopy = () => {
    if (!token) return;
    navigator.clipboard.writeText(token);
    setCopied(true);
    toast.info('Attendance token copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleStopSession = async () => {
    if (!event) return;
    setStopping(true);
    try {
      await api.post(`/organizer/events/${event.id}/attendance/stop`);
      toast.success('Attendance session closed');
      if (onSessionEnd) onSessionEnd();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to close attendance session');
    } finally {
      setStopping(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Live QR Attendance Session" maxWidth="max-w-md">
      <div className="flex flex-col items-center text-center space-y-4">
        {/* Status Indicator */}
        <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-950/80 border border-emerald-700/60 text-emerald-400 text-xs font-semibold animate-pulse">
          <span className="w-2 h-2 rounded-full bg-emerald-400" />
          SESSION ACTIVE - SCANNING OPEN
        </div>

        <div>
          <h4 className="text-base font-bold text-white">{event?.title}</h4>
          <p className="text-xs text-slate-400 mt-0.5">{event?.venue?.name} • {event?.eventDate}</p>
        </div>

        {/* QR Code Canvas */}
        <div className="p-4 bg-white rounded-2xl shadow-xl shadow-indigo-500/10 border-4 border-indigo-500/30">
          <QRCodeSVG
            value={token || 'NO_TOKEN'}
            size={220}
            level="H"
            includeMargin={true}
          />
        </div>

        <p className="text-xs text-slate-400 max-w-xs">
          Display this QR code to attendees on the projector or screen. Registered students can scan from their student dashboard to mark attendance.
        </p>

        {/* Token string pill */}
        <div className="w-full flex items-center justify-between p-2.5 bg-slate-950 rounded-xl border border-slate-800 text-xs font-mono text-slate-300">
          <span className="truncate max-w-[240px]">{token}</span>
          <button
            onClick={handleCopy}
            className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
            title="Copy token"
          >
            {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>

        {/* Actions */}
        <div className="w-full flex gap-2 pt-2 border-t border-slate-800">
          <Button
            variant="secondary"
            size="sm"
            onClick={onClose}
            className="flex-1"
          >
            Keep Running & Close Window
          </Button>
          <Button
            variant="danger"
            size="sm"
            icon={PowerOff}
            loading={stopping}
            onClick={handleStopSession}
            className="flex-1"
          >
            Stop Attendance
          </Button>
        </div>
      </div>
    </Modal>
  );
}
