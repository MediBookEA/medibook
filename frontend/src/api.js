const BASE = '/api/v1';

async function request(path, options) {
  const res = await fetch(`${BASE}${path}`, options);
  if (!res.ok) {
    let message = `Request failed with status ${res.status}`;
    try {
      const body = await res.json();
      if (body && body.message) {
        message = body.message;
      }
    } catch {
      // response body wasn't JSON, keep generic message
    }
    throw new Error(message);
  }
  if (res.status === 204) {
    return null;
  }
  return res.json();
}

export function listPatients() {
  return request('/patients');
}

export function registerPatient({ name, email, dateOfBirth, phone }) {
  return request('/patients', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, dateOfBirth, phone }),
  });
}

export function updatePatient(id, { name, email, phone }) {
  return request(`/patients/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, phone }),
  });
}

export function listDoctors() {
  return request('/doctors');
}

export function getDoctorSchedule(doctorId, dateStr) {
  return request(`/appointments?doctorId=${encodeURIComponent(doctorId)}&date=${encodeURIComponent(dateStr)}`);
}

export function bookAppointment({ patientId, doctorId, startTime }) {
  return request('/appointments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ patientId, doctorId, startTime }),
  });
}

export function rescheduleAppointment(id, startTime) {
  return request(`/appointments/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ startTime }),
  });
}

export function cancelAppointment(id) {
  return request(`/appointments/${id}`, {
    method: 'DELETE',
  });
}
