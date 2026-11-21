import { useEffect, useState } from 'react';
import { BarChart2, Home, User as UserIcon } from 'lucide-react';
import './App.css';
import { LoggingWizard } from './components/LoggingWizard';
import { LoginScreen } from './components/LoginScreen';
import { ProfileTab } from './components/ProfileTab';
import { TrendsTab } from './components/TrendsTab';
import { isFirebaseConfigured } from './lib/firebase';
import {
  signInDemoAccount,
  signInWithGoogleFirebase,
  signOutUser,
  subscribeToAuth,
} from './services/authService';
import {
  addLogEntry,
  clearAllLogs,
  loadThemePreference,
  saveThemePreference,
  subscribeToLogs,
} from './services/logsService';
import { safeJsonParse, THEME_STORAGE_KEY } from './services/storage';
import type { LogEntry, User } from './types';

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState<boolean>(true);
  const [isDarkTheme, setIsDarkTheme] = useState<boolean>(() =>
    safeJsonParse<boolean>(localStorage.getItem(THEME_STORAGE_KEY), false),
  );
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [logsError, setLogsError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'home' | 'trends' | 'profile'>('home');

  useEffect(() => {
    const unsubscribe = subscribeToAuth((nextUser) => {
      setUser(nextUser);
      setAuthLoading(false);
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    if (isDarkTheme) {
      document.body.classList.add('dark-theme');
    } else {
      document.body.classList.remove('dark-theme');
    }
    localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(isDarkTheme));
  }, [isDarkTheme]);

  useEffect(() => {
    if (!user) {
      setLogs([]);
      setLogsError(null);
      return;
    }

    setLogsError(null);
    const unsubscribe = subscribeToLogs(
      user,
      (nextLogs) => {
        setLogs(nextLogs);
      },
      (err) => {
        setLogsError(err.message);
      },
    );

    void loadThemePreference(user).then((savedDarkTheme) => {
      if (typeof savedDarkTheme === 'boolean') {
        setIsDarkTheme(savedDarkTheme);
      }
    });

    return unsubscribe;
  }, [user]);

  const handleFirebaseSignIn = async () => {
    await signInWithGoogleFirebase();
  };

  const handleDemoLogin = (demoUser: User) => {
    signInDemoAccount(demoUser);
    setUser(demoUser);
  };

  const handleLogout = async () => {
    await signOutUser();
    setUser(null);
    setLogs([]);
    setActiveTab('home');
  };

  const handleAddLog = async (newLog: LogEntry) => {
    if (!user) return;
    const updated = await addLogEntry(user, newLog, logs);
    if (user.isDemo || !isFirebaseConfigured) {
      setLogs(updated);
    }
  };

  const handleResetData = async () => {
    if (!user) return;
    await clearAllLogs(user);
    if (user.isDemo || !isFirebaseConfigured) {
      setLogs([]);
    }
  };

  const toggleTheme = () => {
    setIsDarkTheme((prev) => {
      const next = !prev;
      if (user) {
        void saveThemePreference(user, next);
      }
      return next;
    });
  };

  if (authLoading) {
    return (
      <div
        className="app-container"
        style={{
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--text-sub)',
          fontSize: '15px',
        }}
      >
        Loading myMood...
      </div>
    );
  }

  if (!user) {
    return (
      <div className="app-container" style={{ height: '100%' }}>
        <LoginScreen
          isFirebaseEnabled={isFirebaseConfigured}
          onFirebaseSignIn={handleFirebaseSignIn}
          onDemoLogin={handleDemoLogin}
        />
      </div>
    );
  }

  return (
    <div className="app-container" style={{ height: '100%' }}>
      {logsError && (
        <div
          role="alert"
          style={{
            padding: '8px 16px',
            fontSize: '12px',
            color: '#eb5e55',
            background: 'rgba(235, 94, 85, 0.1)',
            borderBottom: '1px solid rgba(235, 94, 85, 0.25)',
            textAlign: 'center',
          }}
        >
          Sync warning: {logsError}
        </div>
      )}

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
      <nav
        aria-label="Primary navigation"
        style={{
          height: '68px',
          borderTop: '1px solid var(--border-color)',
          background: 'var(--bg-card)',
          backdropFilter: 'blur(10px)',
          WebkitBackdropFilter: 'blur(10px)',
          display: 'flex',
          justifyContent: 'space-around',
          alignItems: 'center',
          paddingBottom: 'var(--safe-area-bottom)',
          zIndex: 5,
        }}
      >
        <button
          type="button"
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
            width: '60px',
          }}
        >
          <Home size={20} style={{ opacity: activeTab === 'home' ? 1 : 0.7 }} />
          <span>Home</span>
        </button>

        <button
          type="button"
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
            width: '60px',
          }}
        >
          <BarChart2 size={20} style={{ opacity: activeTab === 'trends' ? 1 : 0.7 }} />
          <span>Trends</span>
        </button>

        <button
          type="button"
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
            width: '60px',
          }}
        >
          <UserIcon size={20} style={{ opacity: activeTab === 'profile' ? 1 : 0.7 }} />
          <span>Profile</span>
        </button>
      </nav>
    </div>
  );
}

export default App;
