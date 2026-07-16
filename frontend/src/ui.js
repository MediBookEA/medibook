// Shared UI class tokens — a small, consistent design system so every view
// reads as one product. Brand colour is teal; slate is the neutral scale.

export const card =
  'rounded-2xl border border-slate-200/80 bg-white/90 p-6 shadow-sm ring-1 ring-slate-900/[0.02] backdrop-blur-sm';

export const cardHover =
  'rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm transition hover:border-slate-300 hover:shadow-md';

export const label = 'mb-1.5 block text-sm font-medium text-slate-700';

export const input =
  'w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/10 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-400';

export const primaryButton =
  'inline-flex items-center justify-center gap-2 rounded-lg bg-teal-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-teal-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-teal-500/20 active:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50';

export const secondaryButton =
  'inline-flex items-center justify-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-slate-500/10';

export const dangerButton =
  'inline-flex items-center justify-center gap-1.5 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-700 transition hover:border-red-300 hover:bg-red-100 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-red-500/10';

export const errorBanner =
  'flex items-start gap-2.5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-800';

export const confirmationBanner =
  'flex items-start gap-2.5 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800';

export const row =
  'flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200/80 bg-white p-4 shadow-sm transition hover:border-slate-300 hover:shadow-md';

export const sectionTitle = 'text-base font-semibold text-slate-900';

export function statusBadge(status) {
  const base =
    'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset';
  return status === 'BOOKED'
    ? `${base} bg-emerald-50 text-emerald-700 ring-emerald-600/20`
    : `${base} bg-slate-100 text-slate-500 ring-slate-500/20`;
}
