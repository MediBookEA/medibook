import { useEffect, useMemo, useState } from 'react';
import { listPatients } from '../api.js';
import { input, sectionTitle } from '../ui.js';
import { ErrorBanner, LoadingState, EmptyState } from './feedback.jsx';
import { UsersIcon } from './icons.jsx';
import { formatDate } from '../format.js';

function initials(name) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

export default function AllPatients() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [query, setQuery] = useState('');

  useEffect(() => {
    listPatients()
      .then(setPatients)
      .catch((err) => setLoadError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return patients;
    return patients.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.email.toLowerCase().includes(q) ||
        (p.phone || '').toLowerCase().includes(q)
    );
  }, [patients, query]);

  if (loading) {
    return <LoadingState />;
  }

  if (loadError) {
    return <ErrorBanner>{loadError}</ErrorBanner>;
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className={sectionTitle}>Patient directory</h3>
        <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-500">
          {patients.length} total
        </span>
      </div>

      <label htmlFor="patient-search" className="sr-only">
        Search patients
      </label>
      <input
        id="patient-search"
        className={input}
        type="search"
        placeholder="Search by name, email, or phone…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />

      {patients.length === 0 ? (
        <EmptyState title="No patients yet">
          Patients you register will appear in this directory.
        </EmptyState>
      ) : filtered.length === 0 ? (
        <EmptyState title="No matches">
          No patients match “{query}”. Try a different search.
        </EmptyState>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2">
          {filtered.map((p) => (
            <li
              key={p.id}
              className="flex items-center gap-4 rounded-xl border border-slate-200/80 bg-white p-4 shadow-sm transition hover:border-slate-300 hover:shadow-md"
            >
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-teal-100 text-sm font-semibold text-teal-700">
                {initials(p.name) || <UsersIcon className="h-5 w-5" />}
              </span>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-slate-900">{p.name}</p>
                <p className="truncate text-sm text-slate-500">{p.email}</p>
                <p className="mt-0.5 truncate text-xs text-slate-400">
                  Born {formatDate(p.dateOfBirth)} · {p.phone}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
