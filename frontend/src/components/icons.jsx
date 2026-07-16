// Lightweight inline SVG icons (stroke-based, inherit currentColor).
// Keeping them local avoids adding an icon-library dependency.

function Svg({ children, className = 'h-5 w-5', ...props }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
      {...props}
    >
      {children}
    </svg>
  );
}

export function LogoMark({ className = 'h-6 w-6' }) {
  return (
    <Svg className={className} strokeWidth="2">
      <path d="M11 2 4 5v6c0 5 3 8.5 7 10 4-1.5 7-5 7-10V5l-7-3Z" />
      <path d="M11 8v6M8 11h6" />
    </Svg>
  );
}

export function CalendarIcon(props) {
  return (
    <Svg {...props}>
      <rect x="3" y="4.5" width="18" height="16" rx="2.5" />
      <path d="M3 9h18M8 2.5v4M16 2.5v4" />
    </Svg>
  );
}

export function ClipboardIcon(props) {
  return (
    <Svg {...props}>
      <path d="M9 4h6a1 1 0 0 1 1 1v1H8V5a1 1 0 0 1 1-1Z" />
      <path d="M8 5H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
      <path d="M9 12h6M9 16h4" />
    </Svg>
  );
}

export function UsersIcon(props) {
  return (
    <Svg {...props}>
      <path d="M16 20v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="3.5" />
      <path d="M22 20v-2a4 4 0 0 0-3-3.87M16 3.13A4 4 0 0 1 16 11" />
    </Svg>
  );
}

export function CheckIcon(props) {
  return (
    <Svg {...props}>
      <path d="M20 6 9 17l-5-5" />
    </Svg>
  );
}

export function AlertIcon(props) {
  return (
    <Svg {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 8v4.5M12 16h.01" />
    </Svg>
  );
}

export function TrashIcon(props) {
  return (
    <Svg {...props}>
      <path d="M4 7h16M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13" />
    </Svg>
  );
}

export function EditIcon(props) {
  return (
    <Svg {...props}>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5Z" />
    </Svg>
  );
}

export function ClockIcon(props) {
  return (
    <Svg {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </Svg>
  );
}

export function StethoscopeIcon(props) {
  return (
    <Svg {...props}>
      <path d="M5 3v5a4 4 0 0 0 8 0V3" />
      <path d="M5 3H3.5M13 3h1.5M9 15v1a5 5 0 0 0 10 0v-1" />
      <circle cx="19" cy="12" r="2.5" />
    </Svg>
  );
}

export function EmptyCalendarIcon({ className = 'h-10 w-10' }) {
  return (
    <Svg className={className} strokeWidth="1.5">
      <rect x="3" y="4.5" width="18" height="16" rx="2.5" />
      <path d="M3 9h18M8 2.5v4M16 2.5v4M8.5 14.5h7" />
    </Svg>
  );
}
