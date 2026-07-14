import { useEffect, useMemo, useState } from 'react';
import { listPatients, listDoctors, bookAppointment } from '../api.js';
import { card, label, input, primaryButton, errorBanner, confirmationBanner } from '../ui.js';

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

  const slots = useMemo(() => computeSlots(selectedDoctor, date), [selectedDoctor, date]);

  function handleSubmit(e) {
    e.preventDefault();
    setSubmitError('');
    setConfirmation(null);

    const startTime = `${date}T${time}:00`;
    bookAppointment({ patientId: Number(patientId), doctorId: Number(doctorId), startTime })
      .then((appointment) => {
        setConfirmation(appointment);
        setTime('');
      })
      .catch((err) => setSubmitError(err.message));
  }

  if (loading) {
    return <p className="text-sm text-slate-500">Loading...</p>;
  }

  if (loadError) {
    return <p className={errorBanner}>{loadError}</p>;
  }

  return (
    <div className={`${card} max-w-md`}>
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Book Appointment</h2>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
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

        <button type="submit" className={`${primaryButton} self-start`}>
          Book
        </button>
      </form>

      {submitError && <p className={`${errorBanner} mt-4`}>{submitError}</p>}
      {confirmation && (
        <p className={`${confirmationBanner} mt-4`}>
          Booked: {confirmation.doctorName} with {confirmation.patientName} at{' '}
          {confirmation.startTime}
        </p>
      )}
    </div>
  );
}
