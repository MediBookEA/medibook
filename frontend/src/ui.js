export const card = 'rounded-xl border border-slate-200 bg-white p-6 shadow-sm';

export const label = 'text-sm font-medium text-slate-700';

export const input =
  'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 shadow-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 disabled:bg-slate-50 disabled:text-slate-400';

export const primaryButton =
  'inline-flex items-center justify-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50';

export const secondaryButton =
  'inline-flex items-center justify-center rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50';

export const dangerButton =
  'inline-flex items-center justify-center rounded-md border border-red-200 bg-red-50 px-3 py-1.5 text-sm font-medium text-red-700 transition hover:bg-red-100';

export const errorBanner = 'rounded-md border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700';

export const confirmationBanner =
  'rounded-md border border-green-200 bg-green-50 px-4 py-2 text-sm text-green-700';

export const row =
  'flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm';

export function statusBadge(status) {
  const base = 'inline-block rounded-full px-2 py-0.5 text-xs font-medium';
  return status === 'BOOKED'
    ? `${base} bg-green-100 text-green-700`
    : `${base} bg-slate-200 text-slate-600`;
}
