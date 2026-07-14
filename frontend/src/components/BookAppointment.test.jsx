import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BookAppointment from './BookAppointment.jsx';
import * as api from '../api.js';

vi.mock('../api.js');

const PATIENTS = [{ id: 1, name: 'Jane Doe', email: 'jane@x.com', dateOfBirth: '1990-01-01', phone: '555' }];

const DOCTORS = [
  {
    id: 2,
    name: 'Dr. Smith',
    specialty: 'Cardiology',
    workingHours: [
      { dayOfWeek: 'TUESDAY', startTime: '09:00:00', endTime: '10:00:00' },
    ],
  },
];

describe('BookAppointment', () => {
  beforeEach(() => {
    api.listPatients.mockResolvedValue(PATIENTS);
    api.listDoctors.mockResolvedValue(DOCTORS);
  });

  it('renders patient and doctor options after fetch resolves', async () => {
    render(<BookAppointment />);

    expect(await screen.findByRole('option', { name: 'Jane Doe' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Dr. Smith (Cardiology)' })).toBeInTheDocument();
  });

  it('computes time slots from the selected doctor working hours', async () => {
    const user = userEvent.setup();
    render(<BookAppointment />);

    await screen.findByRole('option', { name: 'Dr. Smith (Cardiology)' });
    await user.selectOptions(screen.getByLabelText('Doctor'), '2');
    // 2026-07-14 is a Tuesday
    await user.type(screen.getByLabelText('Date'), '2026-07-14');

    expect(screen.getByRole('option', { name: '09:00' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: '09:30' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '10:00' })).not.toBeInTheDocument();
  });

  it('submits booking with correct payload and shows confirmation', async () => {
    const user = userEvent.setup();
    api.bookAppointment.mockResolvedValue({
      id: 99,
      patientId: 1,
      patientName: 'Jane Doe',
      doctorId: 2,
      doctorName: 'Dr. Smith',
      startTime: '2026-07-14T09:00:00',
      endTime: '2026-07-14T09:30:00',
      status: 'BOOKED',
    });

    render(<BookAppointment />);

    await screen.findByRole('option', { name: 'Jane Doe' });
    await user.selectOptions(screen.getByLabelText('Patient'), '1');
    await user.selectOptions(screen.getByLabelText('Doctor'), '2');
    await user.type(screen.getByLabelText('Date'), '2026-07-14');
    await user.selectOptions(screen.getByLabelText('Time'), '09:00');
    await user.click(screen.getByRole('button', { name: 'Book' }));

    await waitFor(() => {
      expect(api.bookAppointment).toHaveBeenCalledWith({
        patientId: 1,
        doctorId: 2,
        startTime: '2026-07-14T09:00:00',
      });
    });

    expect(await screen.findByText(/Booked: Dr. Smith with Jane Doe/)).toBeInTheDocument();
  });

  it('shows server error message when booking fails', async () => {
    const user = userEvent.setup();
    api.bookAppointment.mockRejectedValue(
      new Error('Dr. Smith already has an appointment at 2026-07-14T09:00')
    );

    render(<BookAppointment />);

    await screen.findByRole('option', { name: 'Jane Doe' });
    await user.selectOptions(screen.getByLabelText('Patient'), '1');
    await user.selectOptions(screen.getByLabelText('Doctor'), '2');
    await user.type(screen.getByLabelText('Date'), '2026-07-14');
    await user.selectOptions(screen.getByLabelText('Time'), '09:00');
    await user.click(screen.getByRole('button', { name: 'Book' }));

    expect(
      await screen.findByText('Dr. Smith already has an appointment at 2026-07-14T09:00')
    ).toBeInTheDocument();
  });
});
