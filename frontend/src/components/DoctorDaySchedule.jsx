import { useEffect, useState } from 'react';
import {
  listDoctors,
  getDoctorSchedule,
  cancelAppointment,
  rescheduleAppointment,
} from '../api.js';
import {
  card,
  label,
  input,
  primaryButton,
  secondaryButton,
  dangerButton,
  errorBanner,
  row,
  statusBadge,
} from '../ui.js';

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
    return <p className={errorBanner}>{loadError}</p>;
  }

  return (
    <div>
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Doctor Day Schedule</h2>

      <div className={`${card} mb-6 flex max-w-md flex-col gap-4`}>
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

      {scheduleError && <p className={`${errorBanner} mb-4`}>{scheduleError}</p>}

      {doctorId && date && appointments.length === 0 && !scheduleError && (
        <p className="text-sm text-slate-500">No appointments scheduled for this day.</p>
      )}

      <ul className="flex flex-col gap-3">
        {appointments.map((appt) => (
          <li key={appt.id} className={row}>
            <span className="text-sm text-slate-700">
              {appt.startTime} - {appt.endTime} | {appt.patientName}{' '}
              <span className={statusBadge(appt.status)}>{appt.status}</span>
            </span>
            <div className="flex gap-2">
              <button className={dangerButton} onClick={() => handleCancel(appt.id)}>
                Cancel
              </button>
              <button className={secondaryButton} onClick={() => startReschedule(appt.id)}>
                Reschedule
              </button>
            </div>

            {rescheduleTargetId === appt.id && (
              <form
                className="mt-2 flex w-full flex-wrap items-end gap-3 border-t border-slate-200 pt-3"
                onSubmit={(e) => submitReschedule(e, appt.id)}
              >
                <div>
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
                <div>
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
                <button type="submit" className={primaryButton}>
                  Confirm
                </button>
                <button type="button" className={secondaryButton} onClick={cancelReschedule}>
                  Cancel edit
                </button>
                {rescheduleError && <p className={`${errorBanner} w-full`}>{rescheduleError}</p>}
              </form>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
