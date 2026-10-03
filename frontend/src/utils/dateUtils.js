/**
 * Shared Date and Time Formatting Utilities for CEMS
 * Ensures consistent, human-readable date/time representation across student, organizer, and admin portals.
 */

const MONTH_NAMES_SHORT = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const MONTH_NAMES_FULL = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

/**
 * Format a date string (YYYY-MM-DD or ISO) into readable 'DD Mon YYYY' (e.g., '06 Oct 2026')
 */
export function formatDate(dateInput) {
  if (!dateInput) return '';
  const dateStr = String(dateInput).split('T')[0];
  const parts = dateStr.split('-');
  if (parts.length !== 3) return String(dateInput);

  const year = parts[0];
  const monthIdx = parseInt(parts[1], 10) - 1;
  const day = parts[2].padStart(2, '0');

  const monthName = MONTH_NAMES_SHORT[monthIdx] || parts[1];
  return `${day} ${monthName} ${year}`;
}

/**
 * Format a time string (HH:mm or HH:mm:ss) into 12-hour 'hh:mm AM/PM' (e.g., '09:00 AM')
 */
export function formatTime(timeInput) {
  if (!timeInput) return '';
  // Extract time portion if ISO
  let timeStr = String(timeInput);
  if (timeStr.includes('T')) {
    timeStr = timeStr.split('T')[1];
  }
  const parts = timeStr.split(':');
  if (parts.length < 2) return String(timeInput);

  let hours = parseInt(parts[0], 10);
  const minutes = parts[1].padStart(2, '0');
  const ampm = hours >= 12 ? 'PM' : 'AM';

  hours = hours % 12;
  hours = hours ? hours : 12; // 0 hour is 12 AM
  const formattedHours = String(hours).padStart(2, '0');

  return `${formattedHours}:${minutes} ${ampm}`;
}

/**
 * Format date and time together (e.g., '06 Oct 2026 • 09:00 AM')
 */
export function formatDateTime(dateInput, timeInput) {
  if (!dateInput && !timeInput) return '';

  // If single ISO date-time string passed
  if (dateInput && !timeInput && String(dateInput).includes('T')) {
    const [d, t] = String(dateInput).split('T');
    return `${formatDate(d)} • ${formatTime(t)}`;
  }

  const d = formatDate(dateInput);
  const t = formatTime(timeInput);

  if (d && t) return `${d} • ${t}`;
  return d || t || '';
}

/**
 * Format time range (e.g., '09:00 AM - 11:30 AM')
 */
export function formatTimeRange(startTime, endTime) {
  const s = formatTime(startTime);
  const e = formatTime(endTime);
  if (s && e) return `${s} - ${e}`;
  return s || e || '';
}

/**
 * Extract structured date block for prominent visual rendering:
 * { day: '06', month: 'OCT', year: '2026' }
 */
export function formatEventDateBlock(dateInput) {
  if (!dateInput) {
    return { day: '--', month: '---', year: '----' };
  }
  const dateStr = String(dateInput).split('T')[0];
  const parts = dateStr.split('-');
  if (parts.length !== 3) {
    return { day: '01', month: 'EVT', year: '2026' };
  }

  const year = parts[0];
  const monthIdx = parseInt(parts[1], 10) - 1;
  const day = parts[2].padStart(2, '0');
  const month = (MONTH_NAMES_SHORT[monthIdx] || 'EVT').toUpperCase();

  return { day, month, year };
}
