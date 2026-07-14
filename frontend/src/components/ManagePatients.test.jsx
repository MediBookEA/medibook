import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ManagePatients from './ManagePatients.jsx';
import * as api from '../api.js';

vi.mock('../api.js');

const PATIENTS = [
  { id: 1, name: 'Jane Doe', email: 'jane@x.com', dateOfBirth: '1990-01-01', phone: '555-0100' },
];

describe('ManagePatients', () => {
  beforeEach(() => {
    api.listPatients.mockResolvedValue(PATIENTS);
  });

  it('renders the patient list after fetch resolves', async () => {
    render(<ManagePatients />);

    expect(await screen.findByText(/Jane Doe/)).toBeInTheDocument();
    expect(screen.getByText(/jane@x.com/)).toBeInTheDocument();
  });

  it('registers a new patient with correct payload and shows confirmation', async () => {
    const user = userEvent.setup();
    api.registerPatient.mockResolvedValue({
      id: 2,
      name: 'John Smith',
      email: 'john@x.com',
      dateOfBirth: '1985-05-05',
      phone: '555-0200',
    });

    render(<ManagePatients />);
    await screen.findByText(/Jane Doe/);

    await user.type(screen.getByLabelText('Name'), 'John Smith');
    await user.type(screen.getByLabelText('Email'), 'john@x.com');
    await user.type(screen.getByLabelText('Date of Birth'), '1985-05-05');
    await user.type(screen.getByLabelText('Phone'), '555-0200');
    await user.click(screen.getByRole('button', { name: 'Register' }));

    await waitFor(() => {
      expect(api.registerPatient).toHaveBeenCalledWith({
        name: 'John Smith',
        email: 'john@x.com',
        dateOfBirth: '1985-05-05',
        phone: '555-0200',
      });
    });

    expect(await screen.findByText(/Registered: John Smith/)).toBeInTheDocument();
  });

  it('shows server error message when registration fails', async () => {
    const user = userEvent.setup();
    api.registerPatient.mockRejectedValue(
      new Error('Patient with email jane@x.com already exists')
    );

    render(<ManagePatients />);
    await screen.findByText(/Jane Doe/);

    await user.type(screen.getByLabelText('Name'), 'Jane Doe');
    await user.type(screen.getByLabelText('Email'), 'jane@x.com');
    await user.type(screen.getByLabelText('Date of Birth'), '1990-01-01');
    await user.type(screen.getByLabelText('Phone'), '555-0100');
    await user.click(screen.getByRole('button', { name: 'Register' }));

    expect(
      await screen.findByText('Patient with email jane@x.com already exists')
    ).toBeInTheDocument();
  });

  it('edits a patient and refetches the list', async () => {
    const user = userEvent.setup();
    api.updatePatient.mockResolvedValue({
      id: 1,
      name: 'Jane Updated',
      email: 'jane@x.com',
      dateOfBirth: '1990-01-01',
      phone: '555-9999',
    });

    render(<ManagePatients />);
    await screen.findByText(/Jane Doe/);

    await user.click(screen.getByRole('button', { name: 'Edit' }));
    const nameInput = screen.getByLabelText('Name', { selector: 'input#edit-name-1' });
    await user.clear(nameInput);
    await user.type(nameInput, 'Jane Updated');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => {
      expect(api.updatePatient).toHaveBeenCalledWith(1, {
        name: 'Jane Updated',
        email: 'jane@x.com',
        phone: '555-0100',
      });
    });
    await waitFor(() => expect(api.listPatients).toHaveBeenCalledTimes(2));
  });
});
