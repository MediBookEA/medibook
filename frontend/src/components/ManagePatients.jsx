import { useEffect, useState } from 'react';
import { listPatients, registerPatient, updatePatient } from '../api.js';
import { card, label, input, primaryButton, secondaryButton, sectionTitle } from '../ui.js';
import { ErrorBanner, SuccessBanner, LoadingState, EmptyState } from './feedback.jsx';
import { UsersIcon, EditIcon } from './icons.jsx';
import { formatDate } from '../format.js';

function initials(name) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

export default function ManagePatients() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [phone, setPhone] = useState('');
  const [registerError, setRegisterError] = useState('');
  const [confirmation, setConfirmation] = useState(null);

  const [editTargetId, setEditTargetId] = useState(null);
  const [editName, setEditName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editPhone, setEditPhone] = useState('');
  const [editError, setEditError] = useState('');

  function refetchPatients() {
    return listPatients()
      .then(setPatients)
      .catch((err) => setLoadError(err.message));
  }

  useEffect(() => {
    refetchPatients().finally(() => setLoading(false));
  }, []);

  function handleRegister(e) {
    e.preventDefault();
    setRegisterError('');
    setConfirmation(null);

    registerPatient({ name, email, dateOfBirth, phone })
      .then((patient) => {
        setConfirmation(patient);
        setName('');
        setEmail('');
        setDateOfBirth('');
        setPhone('');
        refetchPatients();
      })
      .catch((err) => setRegisterError(err.message));
  }

  function startEdit(patient) {
    setEditTargetId(patient.id);
    setEditName(patient.name);
    setEditEmail(patient.email);
    setEditPhone(patient.phone);
    setEditError('');
  }

  function cancelEdit() {
    setEditTargetId(null);
    setEditError('');
  }

  function submitEdit(e, id) {
    e.preventDefault();
    updatePatient(id, { name: editName, email: editEmail, phone: editPhone })
      .then(() => {
        setEditTargetId(null);
        refetchPatients();
      })
      .catch((err) => setEditError(err.message));
  }

  if (loading) {
    return <LoadingState />;
  }

  if (loadError) {
    return <ErrorBanner>{loadError}</ErrorBanner>;
  }

  return (
    <div className="grid gap-6 lg:grid-cols-5">
      {/* Register form */}
      <div className="lg:col-span-2">
        <div className={card}>
          <h2 className="mb-4 text-sm font-semibold text-slate-900">Register a patient</h2>
          <form onSubmit={handleRegister} className="flex flex-col gap-4">
            <div>
              <label htmlFor="register-name" className={label}>
                Name
              </label>
              <input
                id="register-name"
                className={input}
                type="text"
                placeholder="Full name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div>
              <label htmlFor="register-email" className={label}>
                Email
              </label>
              <input
                id="register-email"
                className={input}
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div>
              <label htmlFor="register-dob" className={label}>
                Date of Birth
              </label>
              <input
                id="register-dob"
                className={input}
                type="date"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                required
              />
            </div>

            <div>
              <label htmlFor="register-phone" className={label}>
                Phone
              </label>
              <input
                id="register-phone"
                className={input}
                type="tel"
                placeholder="555-0100"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                required
              />
            </div>

            <button type="submit" className={`${primaryButton} mt-1 w-full`}>
              Register
            </button>
          </form>

          {registerError && <ErrorBanner className="mt-4">{registerError}</ErrorBanner>}
          {confirmation && (
            <SuccessBanner className="mt-4">
              Registered: {confirmation.name} ({confirmation.email})
            </SuccessBanner>
          )}
        </div>
      </div>

      {/* Patient list */}
      <div className="lg:col-span-3">
        <div className="mb-3 flex items-center justify-between">
          <h3 className={sectionTitle}>Patients</h3>
          <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-500">
            {patients.length} total
          </span>
        </div>

        {patients.length === 0 ? (
          <EmptyState title="No patients yet">
            Register your first patient using the form on the left.
          </EmptyState>
        ) : (
          <ul className="flex flex-col gap-3">
            {patients.map((p) => (
              <li
                key={p.id}
                className="rounded-xl border border-slate-200/80 bg-white p-4 shadow-sm transition hover:border-slate-300 hover:shadow-md"
              >
                <div className="flex flex-wrap items-center gap-4">
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-teal-100 text-sm font-semibold text-teal-700">
                    {initials(p.name) || <UsersIcon className="h-5 w-5" />}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-slate-900">{p.name}</p>
                    <p className="truncate text-sm text-slate-500">{p.email}</p>
                    <p className="mt-0.5 text-xs text-slate-400">
                      Born {formatDate(p.dateOfBirth)} · {p.phone}
                    </p>
                  </div>
                  <button className={secondaryButton} onClick={() => startEdit(p)}>
                    <EditIcon className="h-4 w-4" />
                    Edit
                  </button>
                </div>

                {editTargetId === p.id && (
                  <form
                    className="mt-4 flex flex-wrap items-end gap-3 border-t border-slate-100 pt-4"
                    onSubmit={(e) => submitEdit(e, p.id)}
                  >
                    <div className="min-w-[9rem] flex-1">
                      <label htmlFor={`edit-name-${p.id}`} className={label}>
                        Name
                      </label>
                      <input
                        id={`edit-name-${p.id}`}
                        className={input}
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        required
                      />
                    </div>
                    <div className="min-w-[9rem] flex-1">
                      <label htmlFor={`edit-email-${p.id}`} className={label}>
                        Email
                      </label>
                      <input
                        id={`edit-email-${p.id}`}
                        className={input}
                        type="email"
                        value={editEmail}
                        onChange={(e) => setEditEmail(e.target.value)}
                        required
                      />
                    </div>
                    <div className="min-w-[9rem] flex-1">
                      <label htmlFor={`edit-phone-${p.id}`} className={label}>
                        Phone
                      </label>
                      <input
                        id={`edit-phone-${p.id}`}
                        className={input}
                        type="tel"
                        value={editPhone}
                        onChange={(e) => setEditPhone(e.target.value)}
                        required
                      />
                    </div>
                    <div className="flex gap-2">
                      <button type="submit" className={primaryButton}>
                        Save
                      </button>
                      <button type="button" className={secondaryButton} onClick={cancelEdit}>
                        Cancel edit
                      </button>
                    </div>
                    {editError && <ErrorBanner className="w-full">{editError}</ErrorBanner>}
                  </form>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
