import { useEffect, useMemo, useState } from 'react';
import { listPatients, listDoctors, bookAppointment } from '../api.js';
import { card, label, input, primaryButton } from '../ui.js';
import { ErrorBanner, SuccessBanner, LoadingState } from './feedback.jsx';
import { CalendarIcon, ClockIcon, StethoscopeIcon, UsersIcon } from './icons.jsx';
import { formatDateTime } from '../format.js';

const DAY_NAMES = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
];

function dayOfWeekForDateString(dateStr) {
  if (!dateStr) return null;
  const parsed = new Date(dateStr);
  return DAY_NAMES[parsed.getUTCDay()];
}

function timeStringToMinutes(timeStr) {
  const [hours, minutes] = timeStr.split(':').map(Number);
  return hours * 60 + minutes;
}

function minutesToTimeString(totalMinutes) {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
}

function computeSlots(doctor, dateStr) {
  if (!doctor || !dateStr) return [];
  const dayName = dayOfWeekForDateString(dateStr);
  const workingHours = doctor.workingHours.find((wh) => wh.dayOfWeek === dayName);
  if (!workingHours) return [];

  const startMinutes = timeStringToMinutes(workingHours.startTime);
  const endMinutes = timeStringToMinutes(workingHours.endTime);
  const slots = [];
  for (let m = startMinutes; m + 30 <= endMinutes; m += 30) {
    slots.push(minutesToTimeString(m));
  }
  return slots;
}

export default function BookAppointment() {
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const [patientId, setPatientId] = useState('');
  const [doctorId, setDoctorId] = useState('');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [confirmation, setConfirmation] = useState(null);

  useEffect(() => {
    Promise.all([listPatients(), listDoctors()])
      .then(([patientsData, doctorsData]) => {
        setPatients(patientsData);
        setDoctors(doctorsData);
      })
      .catch((err) => setLoadError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const selectedDoctor = useMemo(
    () => doctors.find((d) => String(d.id) === String(doctorId)) || null,
    [doctors, doctorId]
  );
  const selectedPatient = useMemo(
    () => patients.find((p) => String(p.id) === String(patientId)) || null,
    [patients, patientId]
  );

  const slots = useMemo(() => computeSlots(selectedDoctor, date), [selectedDoctor, date]);

  function handleSubmit(e) {
    e.preventDefault();
    setSubmitError('');
    setConfirmation(null);
    setSubmitting(true);

    const startTime = `${date}T${time}:00`;
    bookAppointment({ patientId: Number(patientId), doctorId: Number(doctorId), startTime })
      .then((appointment) => {
        setConfirmation(appointment);
        setTime('');
      })
      .catch((err) => setSubmitError(err.message))
      .finally(() => setSubmitting(false));
  }

  if (loading) {
    return <LoadingState />;
  }

  if (loadError) {
    return <ErrorBanner>{loadError}</ErrorBanner>;
  }

  const ready = patientId && doctorId && date && time;

  return (
    <div className="grid gap-6 lg:grid-cols-5">
      <div className={`${card} lg:col-span-3`}>
        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <div>
            <label htmlFor="patient-select" className={label}>
              Patient
            </label>
            <select
              id="patient-select"
              className={input}
              value={patientId}
              onChange={(e) => setPatientId(e.target.value)}
              required
            >
              <option value="">Select a patient</option>
              {patients.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="doctor-select" className={label}>
              Doctor
            </label>
            <select
              id="doctor-select"
              className={input}
              value={doctorId}
              onChange={(e) => {
                setDoctorId(e.target.value);
                setTime('');
              }}
              required
            >
              <option value="">Select a doctor</option>
              {doctors.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} ({d.specialty})
                </option>
              ))}
            </select>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <div>
              <label htmlFor="date-input" className={label}>
                Date
              </label>
              <input
                id="date-input"
                className={input}
                type="date"
                value={date}
                onChange={(e) => {
                  setDate(e.target.value);
                  setTime('');
                }}
                required
              />
            </div>

            <div>
              <label htmlFor="time-select" className={label}>
                Time
              </label>
              <select
                id="time-select"
                className={input}
                value={time}
                onChange={(e) => setTime(e.target.value)}
                required
                disabled={slots.length === 0}
              >
                <option value="">
                  {slots.length === 0 ? 'No available slots' : 'Select a time'}
                </option>
                {slots.map((slot) => (
                  <option key={slot} value={slot}>
                    {slot}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {date && slots.length === 0 && selectedDoctor && (
            <p className="-mt-1 text-xs text-amber-600">
              {selectedDoctor.name} has no working hours on the selected date.
            </p>
          )}

          <button type="submit" className={`${primaryButton} self-start`} disabled={submitting}>
            <CalendarIcon className="h-4 w-4" />
            {submitting ? 'Booking…' : 'Book'}
          </button>
        </form>

        {submitError && <ErrorBanner className="mt-5">{submitError}</ErrorBanner>}
        {confirmation && (
          <SuccessBanner className="mt-5">
            Booked: {confirmation.doctorName} with {confirmation.patientName} at{' '}
            {formatDateTime(confirmation.startTime)}
          </SuccessBanner>
        )}
      </div>

      {/* Live summary */}
      <aside className="lg:col-span-2">
        <div className={card}>
          <h2 className="mb-4 text-sm font-semibold text-slate-900">Summary</h2>
          <dl className="space-y-4">
            <SummaryItem
              Icon={UsersIcon}
              term="Patient"
              value={selectedPatient?.name}
              placeholder="No patient selected"
            />
            <SummaryItem
              Icon={StethoscopeIcon}
              term="Doctor"
              value={
                selectedDoctor ? `${selectedDoctor.name} · ${selectedDoctor.specialty}` : null
              }
              placeholder="No doctor selected"
            />
            <SummaryItem
              Icon={ClockIcon}
              term="When"
              value={ready ? formatDateTime(`${date}T${time}:00`) : null}
              placeholder="No time selected"
            />
          </dl>
          <p className="mt-5 border-t border-slate-100 pt-4 text-xs text-slate-500">
            Appointments last 30 minutes and must fall within the doctor’s working hours.
          </p>
        </div>
      </aside>
    </div>
  );
}

function SummaryItem({ Icon, term, value, placeholder }) {
  return (
    <div className="flex items-start gap-3">
      <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-500">
        <Icon className="h-4 w-4" />
      </span>
      <div className="min-w-0">
        <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">{term}</dt>
        <dd
          className={
            value ? 'text-sm font-medium text-slate-800' : 'text-sm italic text-slate-400'
          }
        >
          {value || placeholder}
        </dd>
      </div>
    </div>
  );
}
