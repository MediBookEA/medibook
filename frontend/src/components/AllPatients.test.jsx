import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AllPatients from './AllPatients.jsx';
import * as api from '../api.js';

vi.mock('../api.js');

const PATIENTS = [
  { id: 1, name: 'Jane Doe', email: 'jane@x.com', dateOfBirth: '1990-01-01', phone: '555-0100' },
  { id: 2, name: 'John Smith', email: 'john@x.com', dateOfBirth: '1985-05-05', phone: '555-0200' },
];

describe('AllPatients', () => {
  beforeEach(() => {
    api.listPatients.mockResolvedValue(PATIENTS);
  });

  it('renders every patient after fetch resolves', async () => {
    render(<AllPatients />);

    expect(await screen.findByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('John Smith')).toBeInTheDocument();
    expect(screen.getByText('jane@x.com')).toBeInTheDocument();
  });

  it('filters the directory by the search query', async () => {
    const user = userEvent.setup();
    render(<AllPatients />);
    await screen.findByText('Jane Doe');

    await user.type(screen.getByLabelText('Search patients'), 'john');

    expect(screen.getByText('John Smith')).toBeInTheDocument();
    expect(screen.queryByText('Jane Doe')).not.toBeInTheDocument();
  });

  it('shows the server error message when the fetch fails', async () => {
    api.listPatients.mockRejectedValue(new Error('boom'));
    render(<AllPatients />);

    expect(await screen.findByText('boom')).toBeInTheDocument();
  });
});
