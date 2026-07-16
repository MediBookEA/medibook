import { useState } from 'react';
import BookAppointment from './components/BookAppointment.jsx';
import DoctorDaySchedule from './components/DoctorDaySchedule.jsx';
import ManagePatients from './components/ManagePatients.jsx';
import AllPatients from './components/AllPatients.jsx';
import UpcomingSchedule from './components/UpcomingSchedule.jsx';
import {
  LogoMark,
  CalendarIcon,
  ClipboardIcon,
  UsersIcon,
  ClockIcon,
  StethoscopeIcon,
} from './components/icons.jsx';

const TABS = {
  BOOK: 'book',
  SCHEDULE: 'schedule',
  UPCOMING: 'upcoming',
  PATIENTS: 'patients',
  DIRECTORY: 'directory',
};

const NAV = [
  {
    id: TABS.BOOK,
    label: 'Book Appointment',
    short: 'Book',
    description: 'Reserve a 30-minute slot for a patient with an available doctor.',
    Icon: CalendarIcon,
  },
  {
    id: TABS.SCHEDULE,
    label: 'Doctor Day Schedule',
    short: 'Day',
    description: 'Review, reschedule, or cancel a doctor’s appointments for a given day.',
    Icon: ClipboardIcon,
  },
  {
    id: TABS.UPCOMING,
    label: 'Upcoming Schedule',
    short: 'Upcoming',
    description: 'Pick a doctor and see all their upcoming appointments, grouped by day.',
    Icon: ClockIcon,
  },
  {
    id: TABS.PATIENTS,
    label: 'Manage Patients',
    short: 'Manage',
    description: 'Register new patients and keep their contact details up to date.',
    Icon: UsersIcon,
  },
  {
    id: TABS.DIRECTORY,
    label: 'Patient Directory',
    short: 'Directory',
    description: 'Browse and search the full list of registered patients.',
    Icon: StethoscopeIcon,
  },
];

export default function App() {
  const [tab, setTab] = useState(TABS.BOOK);
  const active = NAV.find((item) => item.id === tab);

  return (
    <div className="min-h-screen lg:flex">
      {/* Sidebar (desktop) */}
      <aside className="hidden lg:flex lg:w-72 lg:flex-col lg:border-r lg:border-slate-200/80 lg:bg-white/70 lg:backdrop-blur-sm">
        <div className="flex items-center gap-3 px-6 py-6">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-600 text-white shadow-sm">
            <LogoMark />
          </span>
          <div>
            <p className="text-lg font-bold tracking-tight text-slate-900">MediBook</p>
            <p className="text-xs text-slate-500">Clinic appointments</p>
          </div>
        </div>

        <nav className="flex flex-1 flex-col gap-1 px-3">
          {NAV.map(({ id, label, Icon }) => {
            const isActive = tab === id;
            return (
              <button
                key={id}
                onClick={() => setTab(id)}
                aria-current={isActive ? 'page' : undefined}
                className={
                  'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ' +
                  (isActive
                    ? 'bg-teal-50 text-teal-700 ring-1 ring-inset ring-teal-600/10'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900')
                }
              >
                <Icon
                  className={
                    'h-5 w-5 transition ' +
                    (isActive ? 'text-teal-600' : 'text-slate-400 group-hover:text-slate-500')
                  }
                />
                {label}
              </button>
            );
          })}
        </nav>

        <div className="px-6 py-6">
          <div className="rounded-xl border border-slate-200/80 bg-slate-50/80 p-3 text-xs text-slate-500">
            All appointments are 30 minutes, booked within each doctor’s working hours.
          </div>
        </div>
      </aside>

      {/* Main column */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Top bar (mobile brand + tabs) */}
        <header className="border-b border-slate-200/80 bg-white/70 backdrop-blur-sm lg:hidden">
          <div className="flex items-center gap-3 px-4 py-4">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-teal-600 text-white">
              <LogoMark className="h-5 w-5" />
            </span>
            <p className="text-base font-bold tracking-tight text-slate-900">MediBook</p>
          </div>
          <nav className="flex gap-1 overflow-x-auto px-2 pb-2">
            {NAV.map(({ id, short, Icon }) => {
              const isActive = tab === id;
              return (
                <button
                  key={id}
                  onClick={() => setTab(id)}
                  aria-current={isActive ? 'page' : undefined}
                  className={
                    'flex shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ' +
                    (isActive
                      ? 'bg-teal-50 text-teal-700'
                      : 'text-slate-600 hover:bg-slate-100')
                  }
                >
                  <Icon className="h-4 w-4" />
                  {short}
                </button>
              );
            })}
          </nav>
        </header>

        <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8 sm:px-6 lg:px-10">
          <div key={tab} className="animate-fade-in">
            <div className="mb-6 flex items-start gap-3">
              <span className="hidden h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white text-teal-600 shadow-sm ring-1 ring-slate-200/80 sm:flex">
                <active.Icon className="h-6 w-6" />
              </span>
              <div>
                <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                  {active.label}
                </h1>
                <p className="mt-1 text-sm text-slate-500">{active.description}</p>
              </div>
            </div>

            {tab === TABS.BOOK && <BookAppointment />}
            {tab === TABS.SCHEDULE && <DoctorDaySchedule />}
            {tab === TABS.UPCOMING && <UpcomingSchedule />}
            {tab === TABS.PATIENTS && <ManagePatients />}
            {tab === TABS.DIRECTORY && <AllPatients />}
          </div>
        </main>
      </div>
    </div>
  );
}
