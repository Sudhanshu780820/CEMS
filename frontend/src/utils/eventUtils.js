import { formatDate } from './dateUtils';

/**
 * Calculates authoritative, standardized registration status, badges, and button styles
 * for student and public event cards across CEMS.
 *
 * Rules:
 * 1. Already Registered: "REGISTERED" (visually distinct, no registration action)
 * 2. Registration Not Started: "REGISTRATION OPENS ON [DATE]" (disabled/info style)
 * 3. Registration Closed: "REGISTRATION CLOSED" (distinct disabled style, NEVER "VIEW EVENT")
 * 4. Completed Event: "COMPLETED"
 * 5. Not Eligible: "NOT ELIGIBLE"
 * 6. Capacity Full: "EVENT FULL"
 * 7. Registration Open: "REGISTER NOW" (primary action style)
 */
export function getEventRegistrationStatus(event) {
  if (!event) {
    return {
      label: 'VIEW DETAILS',
      state: 'DEFAULT',
      variant: 'default',
      badgeClass: 'bg-slate-900 text-slate-400 border border-slate-800',
      btnClass: 'bg-slate-800 text-slate-300 hover:bg-slate-700',
      canRegister: false,
      isClosed: false,
    };
  }

  // 1. Already Registered
  if (event.registered || event.isRegistered || event.actionStatus === 'REGISTERED') {
    return {
      label: 'REGISTERED',
      state: 'REGISTERED',
      variant: 'purple',
      badgeClass: 'bg-purple-950/90 text-purple-300 border border-purple-700/60',
      btnClass: 'bg-purple-950/80 text-purple-300 border border-purple-700/60',
      canRegister: false,
      isClosed: false,
    };
  }

  // 2. Completed / Past Event
  const isPast = event.status === 'COMPLETED' || event.actionStatus === 'COMPLETED';
  if (isPast) {
    return {
      label: 'COMPLETED',
      state: 'COMPLETED',
      variant: 'default',
      badgeClass: 'bg-slate-900 text-slate-400 border border-slate-800',
      btnClass: 'bg-slate-950 text-slate-500 border border-slate-800',
      canRegister: false,
      isClosed: true,
    };
  }

  // 3. Registration Not Started
  if (
    event.registrationState === 'NOT_STARTED' ||
    event.actionStatus === 'REGISTRATION NOT STARTED'
  ) {
    const formattedDate = event.registrationStartDate ? formatDate(event.registrationStartDate) : '';
    const label = formattedDate
      ? `REGISTRATION OPENS ON ${formattedDate.toUpperCase()}`
      : 'REGISTRATION NOT STARTED';
    return {
      label,
      state: 'NOT_STARTED',
      variant: 'warning',
      badgeClass: 'bg-amber-950/90 text-amber-300 border border-amber-700/60',
      btnClass: 'bg-amber-950/60 text-amber-300 border border-amber-800/60',
      canRegister: false,
      isClosed: false,
    };
  }

  // 4. Registration Closed (Deadline passed)
  if (
    event.registrationState === 'CLOSED' ||
    event.actionStatus === 'REGISTRATION CLOSED' ||
    (event.registrationOpen === false && event.isRegistrationOpen === false)
  ) {
    return {
      label: 'REGISTRATION CLOSED',
      state: 'CLOSED',
      variant: 'danger',
      badgeClass: 'bg-rose-950/90 text-rose-300 border border-rose-700/60',
      btnClass: 'bg-slate-950 text-rose-300/80 border border-rose-900/40 opacity-80',
      canRegister: false,
      isClosed: true,
    };
  }

  // 5. Ineligible for Student
  if (
    event.eligible === false ||
    event.isEligible === false ||
    event.actionStatus === 'NOT ELIGIBLE'
  ) {
    return {
      label: 'NOT ELIGIBLE',
      state: 'NOT_ELIGIBLE',
      variant: 'danger',
      badgeClass: 'bg-rose-950/90 text-rose-300 border border-rose-700/60',
      btnClass: 'bg-slate-950 text-slate-500 border border-slate-800',
      canRegister: false,
      isClosed: false,
    };
  }

  // 6. Event Capacity Full
  if (
    event.full ||
    event.isFull ||
    (event.remainingSeats != null && event.remainingSeats <= 0) ||
    event.actionStatus === 'FULL'
  ) {
    return {
      label: 'EVENT FULL',
      state: 'FULL',
      variant: 'danger',
      badgeClass: 'bg-rose-950/90 text-rose-300 border border-rose-700/60',
      btnClass: 'bg-slate-950 text-slate-500 border border-slate-800',
      canRegister: false,
      isClosed: true,
    };
  }

  // 7. Registration Open & Eligible
  return {
    label: 'REGISTER NOW',
    state: 'OPEN',
    variant: 'success',
    badgeClass: 'bg-emerald-950/90 text-emerald-300 border border-emerald-700/60',
    btnClass: 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-md shadow-indigo-600/20',
    canRegister: true,
    isClosed: false,
  };
}
