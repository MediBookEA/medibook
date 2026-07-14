import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DoctorDaySchedule from './DoctorDaySchedule.jsx';
import * as api from '../api.js';

vi.mock('../api.js');

const DOCTORS = [{ id: 2, name: 'Dr. Smith', specialty: 'Cardiology', workingHours: [] }];

const APPOINTMENT = {
  id: 5,
  patientId: 1,
  patientName: 'Jane Doe',
  doctorId: 2,
  doctorName: 'Dr. Smith',
  startTime: '2026-07-14T09:00:00',
  endTime: '2026-07-14T09:30:00',
  status: 'BOOKED',
};

async function selectDoctorAndDate(user) {
  await screen.findByRole('option', { name: 'Dr. Smith (Cardiology)' });
  await user.selectOptions(screen.getByLabelText('Doctor'), '2');
  fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2026-07-14' } });
}

describe('DoctorDaySchedule', () => {
  beforeEach(() => {
    api.listDoctors.mockResolvedValue(DOCTORS);
  });

  it('renders appointment rows from the fetched schedule', async () => {
    api.getDoctorSchedule.mockResolvedValue([APPOINTMENT]);
    const user = userEvent.setup();

    render(<DoctorDaySchedule />);
    await selectDoctorAndDate(user);

    expect(await screen.findByText(/Jane Doe/)).toBeInTheDocument();
    expect(screen.getByText(/BOOKED/)).toBeInTheDocument();
  });

  it('renders the empty-state message when there are no appointments', async () => {
    api.getDoctorSchedule.mockResolvedValue([]);
    const user = userEvent.setup();

    render(<DoctorDaySchedule />);
    await selectDoctorAndDate(user);

    expect(
      await screen.findByText('No appointments scheduled for this day.')
    ).toBeInTheDocument();
  });

  it('cancels an appointment and refetches the schedule', async () => {
    api.getDoctorSchedule.mockResolvedValueOnce([APPOINTMENT]).mockResolvedValueOnce([]);
    api.cancelAppointment.mockResolvedValue({ ...APPOINTMENT, status: 'CANCELLED' });
    const user = userEvent.setup();

    render(<DoctorDaySchedule />);
    await selectDoctorAndDate(user);
    await screen.findByText(/Jane Doe/);

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(api.cancelAppointment).toHaveBeenCalledWith(5);
    await waitFor(() => expect(api.getDoctorSchedule).toHaveBeenCalledTimes(2));
  });

  it('reschedules an appointment with the new start time and refetches', async () => {
    api.getDoctorSchedule.mockResolvedValue([APPOINTMENT]);
    api.rescheduleAppointment.mockResolvedValue({
      ...APPOINTMENT,
      startTime: '2026-07-15T10:00:00',
    });
    const user = userEvent.setup();

    render(<DoctorDaySchedule />);
    await selectDoctorAndDate(user);
    await screen.findByText(/Jane Doe/);

    await user.click(screen.getByRole('button', { name: 'Reschedule' }));
    fireEvent.change(screen.getByLabelText('New date'), { target: { value: '2026-07-15' } });
    fireEvent.change(screen.getByLabelText('New time'), { target: { value: '10:00' } });
    await user.click(screen.getByRole('button', { name: 'Confirm' }));

    await waitFor(() => {
      expect(api.rescheduleAppointment).toHaveBeenCalledWith(5, '2026-07-15T10:00:00');
    });
    await waitFor(() => expect(api.getDoctorSchedule).toHaveBeenCalledTimes(2));
  });
});
