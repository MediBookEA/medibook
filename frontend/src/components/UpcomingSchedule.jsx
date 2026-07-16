import { useEffect, useState } from 'react';
import { listDoctors, getDoctorUpcoming } from '../api.js';
import { card, label, input, statusBadge } from '../ui.js';
import { ErrorBanner, LoadingState, EmptyState } from './feedback.jsx';
import { ClockIcon, UsersIcon } from './icons.jsx';
import { formatDate, formatTimeRange, groupByDay, todayISO } from '../format.js';

export default function UpcomingSchedule() {
  const [doctors, setDoctors] = useState([]);
  const [loadError, setLoadError] = useState('');

  const [doctorId, setDoctorId] = useState('');
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [scheduleError, setScheduleError] = useState('');

  useEffect(() => {
    listDoctors()
      .then(setDoctors)
      .catch((err) => setLoadError(err.message));
  }, []);

  useEffect(() => {
    if (!doctorId) {
      setAppointments([]);
      return;
    }
    setLoading(true);
    setScheduleError('');
    getDoctorUpcoming(doctorId, todayISO())
      .then(setAppointments)
      .catch((err) => setScheduleError(err.message))
      .finally(() => setLoading(false));
  }, [doctorId]);

  if (loadError) {
    return <ErrorBanner>{loadError}</ErrorBanner>;
  }

  const days = groupByDay(appointments);

  return (
    <div className="space-y-6">
      <div className={`${card} max-w-md`}>
        <label htmlFor="upcoming-doctor-select" className={label}>
          Doctor
        </label>
        <select
          id="upcoming-doctor-select"
          className={input}
          value={doctorId}
          onChange={(e) => setDoctorId(e.target.value)}
        >
          <option value="">Select a doctor</option>
          {doctors.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name} ({d.specialty})
            </option>
          ))}
        </select>
        <p className="mt-3 text-xs text-slate-500">
          Shows every upcoming booked appointment for the selected doctor, from today onward.
        </p>
      </div>

      {scheduleError && <ErrorBanner>{scheduleError}</ErrorBanner>}

      {loading && <LoadingState label="Loading schedule…" />}

      {!loading && !scheduleError && !doctorId && (
        <EmptyState title="Pick a doctor">
          Choose a doctor above to see their upcoming day-by-day schedule.
        </EmptyState>
      )}

      {!loading && !scheduleError && doctorId && appointments.length === 0 && (
        <EmptyState title="No upcoming appointments">
          This doctor has no booked appointments from today onward.
        </EmptyState>
      )}

      {!loading && days.length > 0 && (
        <div className="space-y-6">
          <p className="text-sm text-slate-500">
            <span className="font-semibold text-slate-700">{appointments.length}</span> upcoming
            appointment{appointments.length === 1 ? '' : 's'} across{' '}
            <span className="font-semibold text-slate-700">{days.length}</span> day
            {days.length === 1 ? '' : 's'}
          </p>

          {days.map(({ day, items }) => (
            <section key={day}>
              <div className="mb-2 flex items-center gap-2">
                <h3 className="text-sm font-semibold text-slate-900">{formatDate(day)}</h3>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500">
                  {items.length}
                </span>
              </div>
              <ul className="flex flex-col gap-2">
                {items.map((appt) => (
                  <li
                    key={appt.id}
                    className="flex items-center gap-4 rounded-xl border border-slate-200/80 bg-white p-3.5 shadow-sm transition hover:border-slate-300 hover:shadow-md"
                  >
                    <div className="flex h-11 w-24 shrink-0 flex-col items-center justify-center rounded-lg bg-teal-50 text-teal-700">
                      <ClockIcon className="h-4 w-4" />
                      <span className="mt-0.5 text-xs font-semibold">
                        {formatTimeRange(appt.startTime, appt.endTime)}
                      </span>
                    </div>
                    <div className="flex min-w-0 flex-1 items-center gap-2">
                      <UsersIcon className="h-4 w-4 shrink-0 text-slate-400" />
                      <span className="truncate text-sm font-semibold text-slate-900">
                        {appt.patientName}
                      </span>
                    </div>
                    <span className={statusBadge(appt.status)}>{appt.status}</span>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
