import { useEffect, useState } from 'react';
import { listPatients, registerPatient, updatePatient } from '../api.js';
import {
  card,
  label,
  input,
  primaryButton,
  secondaryButton,
  errorBanner,
  confirmationBanner,
  row,
} from '../ui.js';

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
    return <p className="text-sm text-slate-500">Loading...</p>;
  }

  if (loadError) {
    return <p className={errorBanner}>{loadError}</p>;
  }

  return (
    <div>
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Manage Patients</h2>

      <div className={`${card} mb-6 max-w-md`}>
        <form onSubmit={handleRegister} className="flex flex-col gap-4">
          <div>
            <label htmlFor="register-name" className={label}>
              Name
            </label>
            <input
              id="register-name"
              className={input}
              type="text"
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
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              required
            />
          </div>

          <button type="submit" className={`${primaryButton} self-start`}>
            Register
          </button>
        </form>

        {registerError && <p className={`${errorBanner} mt-4`}>{registerError}</p>}
        {confirmation && (
          <p className={`${confirmationBanner} mt-4`}>
            Registered: {confirmation.name} ({confirmation.email})
          </p>
        )}
      </div>

      <h3 className="mb-3 text-base font-semibold text-slate-900">Patients</h3>
      <ul className="flex flex-col gap-3">
        {patients.map((p) => (
          <li key={p.id} className={row}>
            <span className="text-sm text-slate-700">
              {p.name} | {p.email} | DOB {p.dateOfBirth} | {p.phone}
            </span>
            <button className={secondaryButton} onClick={() => startEdit(p)}>
              Edit
            </button>

            {editTargetId === p.id && (
              <form
                className="mt-2 flex w-full flex-wrap items-end gap-3 border-t border-slate-200 pt-3"
                onSubmit={(e) => submitEdit(e, p.id)}
              >
                <div>
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
                <div>
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
                <div>
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
                <button type="submit" className={primaryButton}>
                  Save
                </button>
                <button type="button" className={secondaryButton} onClick={cancelEdit}>
                  Cancel edit
                </button>
                {editError && <p className={`${errorBanner} w-full`}>{editError}</p>}
              </form>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
