import { useEffect, useState } from 'react';
import {
  listDoctors,
  getDoctorSchedule,
  cancelAppointment,
  rescheduleAppointment,
} from '../api.js';
import { card, label, input, primaryButton, secondaryButton, dangerButton, statusBadge } from '../ui.js';
import { ErrorBanner, EmptyState } from './feedback.jsx';
import { TrashIcon, EditIcon, ClockIcon, UsersIcon } from './icons.jsx';
import { formatTimeRange } from '../format.js';

export default function DoctorDaySchedule() {
  const [doctors, setDoctors] = useState([]);
  const [loadError, setLoadError] = useState('');

  const [doctorId, setDoctorId] = useState('');
  const [date, setDate] = useState('');

  const [appointments, setAppointments] = useState([]);
  const [scheduleError, setScheduleError] = useState('');

  const [rescheduleTargetId, setRescheduleTargetId] = useState(null);
  const [rescheduleDate, setRescheduleDate] = useState('');
  const [rescheduleTime, setRescheduleTime] = useState('');
  const [rescheduleError, setRescheduleError] = useState('');

  useEffect(() => {
    listDoctors()
      .then(setDoctors)
      .catch((err) => setLoadError(err.message));
  }, []);

  function refetchSchedule() {
    if (!doctorId || !date) return;
    setScheduleError('');
    getDoctorSchedule(doctorId, date)
      .then(setAppointments)
      .catch((err) => setScheduleError(err.message));
  }

  useEffect(() => {
    refetchSchedule();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doctorId, date]);

  function handleCancel(id) {
    cancelAppointment(id)
      .then(() => refetchSchedule())
      .catch((err) => setScheduleError(err.message));
  }

  function startReschedule(id) {
    setRescheduleTargetId(id);
    setRescheduleDate('');
    setRescheduleTime('');
    setRescheduleError('');
  }

  function cancelReschedule() {
    setRescheduleTargetId(null);
    setRescheduleError('');
  }

  function submitReschedule(e, id) {
    e.preventDefault();
    const startTime = `${rescheduleDate}T${rescheduleTime}:00`;
    rescheduleAppointment(id, startTime)
      .then(() => {
        setRescheduleTargetId(null);
        refetchSchedule();
      })
      .catch((err) => setRescheduleError(err.message));
  }

  if (loadError) {
    return <ErrorBanner>{loadError}</ErrorBanner>;
  }

  const hasSelection = doctorId && date;
  const bookedCount = appointments.filter((a) => a.status === 'BOOKED').length;

  return (
    <div className="space-y-6">
      <div className={`${card} grid gap-5 sm:grid-cols-2`}>
        <div>
          <label htmlFor="schedule-doctor-select" className={label}>
            Doctor
          </label>
          <select
            id="schedule-doctor-select"
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
        </div>

        <div>
          <label htmlFor="schedule-date-input" className={label}>
            Date
          </label>
          <input
            id="schedule-date-input"
            className={input}
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
        </div>
      </div>

      {scheduleError && <ErrorBanner>{scheduleError}</ErrorBanner>}

      {!hasSelection && !scheduleError && (
        <EmptyState title="Pick a doctor and date">
          Choose a doctor and a day above to see their appointments.
        </EmptyState>
      )}

      {hasSelection && appointments.length === 0 && !scheduleError && (
        <EmptyState title="No appointments scheduled for this day.">
          This day is completely open — new bookings will appear here.
        </EmptyState>
      )}

      {appointments.length > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-slate-500">
            <span className="font-semibold text-slate-700">{appointments.length}</span>{' '}
            appointment{appointments.length === 1 ? '' : 's'}
            {bookedCount !== appointments.length && (
              <span className="text-slate-400"> · {bookedCount} booked</span>
            )}
          </p>
        </div>
      )}

      <ul className="flex flex-col gap-3">
        {appointments.map((appt) => {
          const isCancelled = appt.status === 'CANCELLED';
          return (
            <li
              key={appt.id}
              className="flex flex-wrap items-center gap-4 rounded-xl border border-slate-200/80 bg-white p-4 shadow-sm transition hover:border-slate-300 hover:shadow-md"
            >
              <div
                className={
                  'flex h-14 w-24 shrink-0 flex-col items-center justify-center rounded-lg text-center ' +
                  (isCancelled
                    ? 'bg-slate-50 text-slate-400'
                    : 'bg-teal-50 text-teal-700')
                }
              >
                <ClockIcon className="h-4 w-4" />
                <span className="mt-0.5 text-xs font-semibold">
                  {formatTimeRange(appt.startTime, appt.endTime)}
                </span>
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <UsersIcon className="h-4 w-4 text-slate-400" />
                  <span
                    className={
                      'truncate text-sm font-semibold ' +
                      (isCancelled ? 'text-slate-400 line-through' : 'text-slate-900')
                    }
                  >
                    {appt.patientName}
                  </span>
                </div>
                <div className="mt-1">
                  <span className={statusBadge(appt.status)}>{appt.status}</span>
                </div>
              </div>

              {!isCancelled && (
                <div className="flex gap-2">
                  <button className={dangerButton} onClick={() => handleCancel(appt.id)}>
                    <TrashIcon className="h-4 w-4" />
                    Cancel
                  </button>
                  <button className={secondaryButton} onClick={() => startReschedule(appt.id)}>
                    <EditIcon className="h-4 w-4" />
                    Reschedule
                  </button>
                </div>
              )}

              {rescheduleTargetId === appt.id && (
                <form
                  className="mt-1 flex w-full flex-wrap items-end gap-3 border-t border-slate-100 pt-4"
                  onSubmit={(e) => submitReschedule(e, appt.id)}
                >
                  <div className="min-w-[10rem] flex-1">
                    <label htmlFor={`reschedule-date-${appt.id}`} className={label}>
                      New date
                    </label>
                    <input
                      id={`reschedule-date-${appt.id}`}
                      className={input}
                      type="date"
                      value={rescheduleDate}
                      onChange={(e) => setRescheduleDate(e.target.value)}
                      required
                    />
                  </div>
                  <div className="min-w-[10rem] flex-1">
                    <label htmlFor={`reschedule-time-${appt.id}`} className={label}>
                      New time
                    </label>
                    <input
                      id={`reschedule-time-${appt.id}`}
                      className={input}
                      type="time"
                      value={rescheduleTime}
                      onChange={(e) => setRescheduleTime(e.target.value)}
                      required
                    />
                  </div>
                  <div className="flex gap-2">
                    <button type="submit" className={primaryButton}>
                      Confirm
                    </button>
                    <button type="button" className={secondaryButton} onClick={cancelReschedule}>
                      Cancel edit
                    </button>
                  </div>
                  {rescheduleError && <ErrorBanner className="w-full">{rescheduleError}</ErrorBanner>}
                </form>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
