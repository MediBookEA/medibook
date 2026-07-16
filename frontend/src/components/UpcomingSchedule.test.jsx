import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UpcomingSchedule from './UpcomingSchedule.jsx';
import * as api from '../api.js';

vi.mock('../api.js');

const DOCTORS = [{ id: 2, name: 'Dr. Smith', specialty: 'Cardiology', workingHours: [] }];

const APPOINTMENTS = [
  {
    id: 5,
    patientId: 1,
    patientName: 'Jane Doe',
    doctorId: 2,
    doctorName: 'Dr. Smith',
    startTime: '2026-07-16T09:00:00',
    endTime: '2026-07-16T09:30:00',
    status: 'BOOKED',
  },
  {
    id: 6,
    patientId: 3,
    patientName: 'John Smith',
    doctorId: 2,
    doctorName: 'Dr. Smith',
    startTime: '2026-07-17T10:00:00',
    endTime: '2026-07-17T10:30:00',
    status: 'BOOKED',
  },
];

describe('UpcomingSchedule', () => {
  beforeEach(() => {
    api.listDoctors.mockResolvedValue(DOCTORS);
  });

  it('fetches and groups a doctor upcoming appointments by day', async () => {
    api.getDoctorUpcoming.mockResolvedValue(APPOINTMENTS);
    const user = userEvent.setup();

    render(<UpcomingSchedule />);
    await screen.findByRole('option', { name: 'Dr. Smith (Cardiology)' });
    await user.selectOptions(screen.getByLabelText('Doctor'), '2');

    await waitFor(() => {
      expect(api.getDoctorUpcoming).toHaveBeenCalledWith('2', expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/));
    });

    expect(await screen.findByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('John Smith')).toBeInTheDocument();
    // Two distinct calendar days → two day sections.
    expect(screen.getByText(/across/)).toHaveTextContent('2 days');
  });

  it('shows an empty state when the doctor has no upcoming appointments', async () => {
    api.getDoctorUpcoming.mockResolvedValue([]);
    const user = userEvent.setup();

    render(<UpcomingSchedule />);
    await screen.findByRole('option', { name: 'Dr. Smith (Cardiology)' });
    await user.selectOptions(screen.getByLabelText('Doctor'), '2');

    expect(await screen.findByText('No upcoming appointments')).toBeInTheDocument();
  });

  it('surfaces the server error message when the fetch fails', async () => {
    api.getDoctorUpcoming.mockRejectedValue(new Error('Doctor 2 not found'));
    const user = userEvent.setup();

    render(<UpcomingSchedule />);
    await screen.findByRole('option', { name: 'Dr. Smith (Cardiology)' });
    await user.selectOptions(screen.getByLabelText('Doctor'), '2');

    expect(await screen.findByText('Doctor 2 not found')).toBeInTheDocument();
  });
});
