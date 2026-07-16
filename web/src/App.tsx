import { useState, useEffect } from 'react';
import './App.css';
import { LoginScreen } from './components/LoginScreen';
import { LoggingWizard } from './components/LoggingWizard';
import { TrendsTab } from './components/TrendsTab';
import { ProfileTab } from './components/ProfileTab';
import { Home, BarChart2, User as UserIcon } from 'lucide-react';

interface User {
  name: string;
  email: string;
  avatar: string;
}

interface LogEntry {
  id: string;
  timestamp: string;
  core: string;
  sub1: string;
  sub2: string;
  intensity: number;
  note: string;
  color: string;
}

const STORAGE_KEYS = {
  USER: 'serene_user',
  THEME: 'serene_theme_dark',
  LOGS: 'serene_logs'
};

const getInitialMockLogs = (): LogEntry[] => {
  const current = new Date();
  const mockEntries: LogEntry[] = [];
  
  // Array of emotions and notes
  const mockOptions = [
    { core: 'Happy', sub1: 'Content', sub2: 'Joyful', color: 'hsl(15, 60%, 70%)', note: 'Had a wonderful lunch with friends today.', intensity: 7 },
    { core: 'Sad', sub1: 'Lonely', sub2: 'Isolated', color: 'hsl(210, 40%, 65%)', note: 'Struggled to connect with coworkers today.', intensity: 4 },
    { core: 'Bad', sub1: 'Tired', sub2: 'Sleepy', color: 'hsl(140, 30%, 60%)', note: 'Did not sleep well last night, feeling slow.', intensity: 6 },
    { core: 'Surprised', sub1: 'Amazed', sub2: 'Awe', color: 'hsl(35, 55%, 65%)', note: 'Beautiful sunset on my evening walk.', intensity: 8 },
    { core: 'Happy', sub1: 'Proud', sub2: 'Confident', color: 'hsl(15, 60%, 70%)', note: 'Finished a major milestone at work!', intensity: 9 },
    { core: 'Angry', sub1: 'Frustrated', sub2: 'Annoyed', color: 'hsl(5, 45%, 65%)', note: 'Stuck in heavy traffic for an hour.', intensity: 5 }
  ];

  // Distribute over the past few days of the current week
  for (let i = 0; i < mockOptions.length; i++) {
    const logDate = new Date();
    // Offset each log to a different day of this week
    logDate.setDate(current.getDate() - (current.getDay() - i));
    logDate.setHours(10 + i, 15 * i, 0, 0);

    mockEntries.push({
      id: `mock-${i}`,
      timestamp: logDate.toISOString(),
      core: mockOptions[i].core,
      sub1: mockOptions[i].sub1,
      sub2: mockOptions[i].sub2,
      intensity: mockOptions[i].intensity,
      note: mockOptions[i].note,
      color: mockOptions[i].color
    });
  }

  return mockEntries;
};

function App() {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.USER);
    return saved ? JSON.parse(saved) : null;
  });

  const [isDarkTheme, setIsDarkTheme] = useState<boolean>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.THEME);
    return saved ? JSON.parse(saved) : false;
  });

  const [logs, setLogs] = useState<LogEntry[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.LOGS);
    if (saved) {
      return JSON.parse(saved);
    }
    // Pre-populate with mock logs so trends graph has data right away
    const initialMock = getInitialMockLogs();
    localStorage.setItem(STORAGE_KEYS.LOGS, JSON.stringify(initialMock));
    return initialMock;
  });

  const [activeTab, setActiveTab] = useState<'home' | 'trends' | 'profile'>('home');

  // Handle Theme application
  useEffect(() => {
    if (isDarkTheme) {
      document.body.classList.add('dark-theme');
    } else {
      document.body.classList.remove('dark-theme');
    }
    localStorage.setItem(STORAGE_KEYS.THEME, JSON.stringify(isDarkTheme));
  }, [isDarkTheme]);

  const handleLogin = (newUser: User) => {
    setUser(newUser);
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(newUser));
  };

  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem(STORAGE_KEYS.USER);
  };

  const handleAddLog = (newLog: LogEntry) => {
    const updated = [newLog, ...logs];
    setLogs(updated);
    localStorage.setItem(STORAGE_KEYS.LOGS, JSON.stringify(updated));
  };

  const handleResetData = () => {
    setLogs([]);
    localStorage.setItem(STORAGE_KEYS.LOGS, JSON.stringify([]));
  };

  const toggleTheme = () => {
    setIsDarkTheme(!isDarkTheme);
  };

  if (!user) {
    return (
      <div className="app-container" style={{ height: '100%' }}>
        <LoginScreen onLogin={handleLogin} />
      </div>
    );
  }

  return (
    <div className="app-container" style={{ height: '100%' }}>
      
      {/* Content Area */}
      <div style={{ flex: 1, overflow: 'hidden', position: 'relative' }}>
        {activeTab === 'home' && <LoggingWizard onAddLog={handleAddLog} />}
        {activeTab === 'trends' && <TrendsTab logs={logs} />}
        {activeTab === 'profile' && (
          <ProfileTab 
            user={user}
            isDarkTheme={isDarkTheme}
            onToggleTheme={toggleTheme}
            onLogout={handleLogout}
            onResetData={handleResetData}
          />
        )}
      </div>

      {/* Bottom Navigation Bar */}
      <div style={{
        height: '68px',
        borderTop: '1px solid var(--border-color)',
        background: 'var(--bg-card)',
        backdropFilter: 'blur(10px)',
        WebkitBackdropFilter: 'blur(10px)',
        display: 'flex',
        justifyContent: 'space-around',
        alignItems: 'center',
        paddingBottom: 'var(--safe-area-bottom)',
        zIndex: 5
      }}>
        <button
          onClick={() => setActiveTab('home')}
          style={{
            background: 'none',
            border: 'none',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '4px',
            color: activeTab === 'home' ? 'var(--accent)' : 'var(--text-sub)',
            cursor: 'pointer',
            fontSize: '12px',
            fontWeight: activeTab === 'home' ? 600 : 400,
            outline: 'none',
            width: '60px'
          }}
        >
          <Home size={20} style={{ opacity: activeTab === 'home' ? 1 : 0.7 }} />
          <span>Home</span>
        </button>

        <button
          onClick={() => setActiveTab('trends')}
          style={{
            background: 'none',
            border: 'none',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '4px',
            color: activeTab === 'trends' ? 'var(--accent)' : 'var(--text-sub)',
            cursor: 'pointer',
            fontSize: '12px',
            fontWeight: activeTab === 'trends' ? 600 : 400,
            outline: 'none',
            width: '60px'
          }}
        >
          <BarChart2 size={20} style={{ opacity: activeTab === 'trends' ? 1 : 0.7 }} />
          <span>Trends</span>
        </button>

        <button
          onClick={() => setActiveTab('profile')}
          style={{
            background: 'none',
            border: 'none',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '4px',
            color: activeTab === 'profile' ? 'var(--accent)' : 'var(--text-sub)',
            cursor: 'pointer',
            fontSize: '12px',
            fontWeight: activeTab === 'profile' ? 600 : 400,
            outline: 'none',
            width: '60px'
          }}
        >
          <UserIcon size={20} style={{ opacity: activeTab === 'profile' ? 1 : 0.7 }} />
          <span>Profile</span>
        </button>
      </div>

    </div>
  );
}

export default App;
