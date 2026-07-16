import { errorBanner, confirmationBanner } from '../ui.js';
import { AlertIcon, CheckIcon, EmptyCalendarIcon } from './icons.jsx';

export function ErrorBanner({ children, className = '' }) {
  return (
    <div role="alert" className={`${errorBanner} ${className}`.trim()}>
      <AlertIcon className="mt-0.5 h-4 w-4 shrink-0 text-red-500" />
      <span>{children}</span>
    </div>
  );
}

export function SuccessBanner({ children, className = '' }) {
  return (
    <div className={`${confirmationBanner} ${className}`.trim()}>
      <CheckIcon className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
      <span>{children}</span>
    </div>
  );
}

export function Spinner({ className = 'h-5 w-5' }) {
  return (
    <svg
      className={`animate-spin text-teal-600 ${className}`}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle className="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path
        className="opacity-90"
        fill="currentColor"
        d="M4 12a8 8 0 0 1 8-8V0C5.4 0 0 5.4 0 12h4Z"
      />
    </svg>
  );
}

export function LoadingState({ label = 'Loading…' }) {
  return (
    <div className="flex items-center gap-3 py-10 text-sm text-slate-500">
      <Spinner />
      {label}
    </div>
  );
}

export function EmptyState({ title, children }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-slate-300 bg-white/60 px-6 py-12 text-center">
      <span className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
        <EmptyCalendarIcon className="h-6 w-6" />
      </span>
      {title && <p className="text-sm font-semibold text-slate-700">{title}</p>}
      {children && <p className="max-w-sm text-sm text-slate-500">{children}</p>}
    </div>
  );
}
