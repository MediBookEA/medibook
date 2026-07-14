import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { listPatients, getDoctorSchedule, bookAppointment } from './api.js';

function mockFetchResolve(body, ok = true, status = 200) {
  return vi.fn().mockResolvedValue({
    ok,
    status,
    json: () => Promise.resolve(body),
  });
}

describe('api.js', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('parses JSON on success', async () => {
    global.fetch = mockFetchResolve([{ id: 1, name: 'Jane' }]);
    const result = await listPatients();
    expect(result).toEqual([{ id: 1, name: 'Jane' }]);
    expect(global.fetch).toHaveBeenCalledWith('/api/v1/patients', undefined);
  });

  it('throws an Error with the server message on error response', async () => {
    global.fetch = mockFetchResolve(
      {
        timestamp: '2026-07-14T10:00:00',
        status: 409,
        error: 'DOUBLE_BOOKING',
        message: 'Dr. Smith already has an appointment at 2026-07-14T09:00',
        path: '/api/v1/appointments',
      },
      false,
      409
    );

    await expect(
      bookAppointment({ patientId: 1, doctorId: 2, startTime: '2026-07-14T09:00:00' })
    ).rejects.toThrow('Dr. Smith already has an appointment at 2026-07-14T09:00');
  });

  it('falls back to a generic message when the error body is not JSON', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new Error('not json')),
    });

    await expect(listPatients()).rejects.toThrow('Request failed with status 500');
  });

  it('builds the correct query string for getDoctorSchedule', async () => {
    global.fetch = mockFetchResolve([]);
    await getDoctorSchedule(3, '2026-07-14');
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/appointments?doctorId=3&date=2026-07-14',
      undefined
    );
  });
});
