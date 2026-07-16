// Display formatters for the clinic's local date/time strings.
// Backend sends naive LocalDateTime (no zone); parse as local for display only.

function parse(dt) {
  if (!dt) return null;
  const d = new Date(dt);
  return Number.isNaN(d.getTime()) ? null : d;
}

const TIME_OPTS = { hour: 'numeric', minute: '2-digit' };
const DATE_OPTS = { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' };

export function formatTime(dt) {
  const d = parse(dt);
  return d ? d.toLocaleTimeString([], TIME_OPTS) : dt;
}

export function formatDate(dt) {
  const d = parse(dt);
  return d ? d.toLocaleDateString([], DATE_OPTS) : dt;
}

export function formatDateTime(dt) {
  const d = parse(dt);
  return d ? `${d.toLocaleDateString([], DATE_OPTS)} · ${d.toLocaleTimeString([], TIME_OPTS)}` : dt;
}

export function formatTimeRange(start, end) {
  const s = formatTime(start);
  const e = formatTime(end);
  return `${s} – ${e}`;
}

// Today's date as a local YYYY-MM-DD string (clinic has one time zone).
export function todayISO() {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

// Group appointments by their calendar date (YYYY-MM-DD), preserving order.
export function groupByDay(appointments) {
  const groups = new Map();
  for (const appt of appointments) {
    const day = (appt.startTime || '').slice(0, 10);
    if (!groups.has(day)) groups.set(day, []);
    groups.get(day).push(appt);
  }
  return Array.from(groups, ([day, items]) => ({ day, items }));
}
