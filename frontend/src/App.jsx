import { useState } from 'react';
import BookAppointment from './components/BookAppointment.jsx';
import DoctorDaySchedule from './components/DoctorDaySchedule.jsx';
import ManagePatients from './components/ManagePatients.jsx';

const TABS = {
  BOOK: 'book',
  SCHEDULE: 'schedule',
  PATIENTS: 'patients',
};

const TAB_LABELS = {
  [TABS.BOOK]: 'Book Appointment',
  [TABS.SCHEDULE]: 'Doctor Day Schedule',
  [TABS.PATIENTS]: 'Manage Patients',
};

export default function App() {
  const [tab, setTab] = useState(TABS.BOOK);

  return (
    <div className="min-h-screen bg-slate-100">
      <div className="mx-auto max-w-3xl px-4 py-8">
        <header className="mb-6">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">MediBook</h1>
          <p className="text-sm text-slate-500">Clinic appointment booking</p>
        </header>

        <nav className="mb-6 inline-flex gap-1 rounded-lg bg-slate-200/70 p-1">
          {Object.values(TABS).map((value) => (
            <button
              key={value}
              className={
                tab === value
                  ? 'tab rounded-md bg-white px-3 py-1.5 text-sm font-medium text-slate-900 shadow-sm'
                  : 'tab rounded-md px-3 py-1.5 text-sm font-medium text-slate-600 hover:text-slate-900'
              }
              onClick={() => setTab(value)}
            >
              {TAB_LABELS[value]}
            </button>
          ))}
        </nav>

        <main>
          {tab === TABS.BOOK && <BookAppointment />}
          {tab === TABS.SCHEDULE && <DoctorDaySchedule />}
          {tab === TABS.PATIENTS && <ManagePatients />}
        </main>
      </div>
    </div>
  );
}
