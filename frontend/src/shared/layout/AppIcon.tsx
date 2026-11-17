export default function AppIcon({ name, size = 20 }: {
  name: string;
  size?: number;
}) {
  let content;

  switch (name) {
    case 'dashboard':
      content = <>
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </>;
      break;

    case 'donor':
      content = <path d="M12 3c-3 4-7 8-7 12a7 7 0 0 0 14 0c0-4-4-8-7-12Z" />;
      break;

    case 'verified-ambulances':
    case 'ambulance':
      content = <>
        <path d="M3 6h12v12H3zM15 10h4l3 4v4h-7" />
        <path d="M6 11h6M9 8v6" />
        <circle cx="7" cy="19" r="2" />
        <circle cx="18" cy="19" r="2" />
      </>;
      break;

    case 'pharmacy':
      content = <>
        <rect x="3" y="7" width="18" height="14" rx="3" />
        <path d="M8 7V5h8v2M9 14h6M12 11v6" />
      </>;
      break;

    case 'admin':
      content = <>
        <path d="M12 3 3 7v5c0 5 4 8 9 10 5-2 9-5 9-10V7l-9-4Z" />
        <path d="m8 12 3 3 5-6" />
      </>;
      break;

    case 'profile':
      content = <>
        <circle cx="12" cy="8" r="4" />
        <path d="M4 21v-2a8 8 0 0 1 16 0v2" />
      </>;
      break;

    case 'logout':
      content = <>
        <path d="M10 17l5-5-5-5M15 12H3M14 3h7v18h-7" />
      </>;
      break;

    case 'menu':
      content = <path d="M4 6h16M4 12h16M4 18h16" />;
      break;

    case 'close':
      content = <path d="m6 6 12 12M18 6 6 18" />;
      break;

    case 'chevron':
      content = <path d="m6 9 6 6 6-6" />;
      break;

    case 'arrow':
      content = <path d="M4 12h16m-6-6 6 6-6 6" />;
      break;

    case 'language':
      content = <>
        <circle cx="12" cy="12" r="9" />
        <path d="M3 12h18M12 3c5 5 5 13 0 18M12 3c-5 5-5 13 0 18" />
      </>;
      break;

    default:
      content = <path d="M12 5v14M5 12h14" />;
  }

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {content}
    </svg>
  );
}