import React from 'react';
import { Moon, Sun, Shield, Link2, LogOut, Trash2 } from 'lucide-react';

interface User {
  name: string;
  email: string;
  avatar: string;
}

interface ProfileTabProps {
  user: User;
  isDarkTheme: boolean;
  onToggleTheme: () => void;
  onLogout: () => void;
  onResetData: () => void;
}

export const ProfileTab: React.FC<ProfileTabProps> = ({
  user,
  isDarkTheme,
  onToggleTheme,
  onLogout,
  onResetData
}) => {
  return (
    <div className="scroll-container animate-fade-in">
      <h1 className="title">Profile</h1>
      <p className="subtitle">Manage your personal settings and app preferences.</p>

      {/* User Card */}
      <div className="card" style={{
        display: 'flex',
        alignItems: 'center',
        gap: '20px',
        marginBottom: '28px',
        padding: '24px'
      }}>
        <img 
          src={user.avatar} 
          alt={user.name} 
          style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            objectFit: 'cover',
            border: '2px solid var(--accent)'
          }}
        />
        <div style={{ display: 'flex', flexDirection: 'column', textAlign: 'left' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>{user.name}</h2>
          <span style={{ fontSize: '14px', color: 'var(--text-sub)' }}>{user.email}</span>
        </div>
      </div>

      {/* Preferences Section */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '28px' }}>
        
        {/* Theme Setting */}
        <div className="card" style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '16px 20px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {isDarkTheme ? <Moon size={18} style={{ color: 'var(--accent)' }} /> : <Sun size={18} style={{ color: 'var(--accent)' }} />}
            <span style={{ fontSize: '15px', fontWeight: 500 }}>Theme Mode</span>
          </div>
          <button
            onClick={onToggleTheme}
            style={{
              padding: '6px 14px',
              borderRadius: '10px',
              border: 'none',
              background: 'var(--accent-light)',
              color: 'var(--accent)',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              outline: 'none'
            }}
          >
            {isDarkTheme ? 'Silent Night' : 'Calm Dawn'}
          </button>
        </div>

        {/* Connections Setting (Mock) */}
        <div className="card" style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '16px 20px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Link2 size={18} style={{ color: 'var(--text-sub)' }} />
            <span style={{ fontSize: '15px', fontWeight: 500 }}>Connections</span>
          </div>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Connected</span>
        </div>

        {/* Security Setting (Mock) */}
        <div className="card" style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '16px 20px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Shield size={18} style={{ color: 'var(--text-sub)' }} />
            <span style={{ fontSize: '15px', fontWeight: 500 }}>Password</span>
          </div>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Managed by Google</span>
        </div>
      </div>

      {/* Danger Zone / Action Zone */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        
        {/* Reset Data Button */}
        <button
          onClick={() => {
            if (confirm('Are you sure you want to clear your feelings log history? This action cannot be undone.')) {
              onResetData();
            }
          }}
          className="btn btn-secondary"
          style={{
            borderColor: 'rgba(235, 94, 85, 0.2)',
            color: '#eb5e55',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '10px'
          }}
        >
          <Trash2 size={16} />
          Reset History
        </button>

        {/* Logout Button */}
        <button
          onClick={onLogout}
          className="btn btn-primary"
          style={{
            background: 'var(--text-main)',
            boxShadow: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '10px'
          }}
        >
          <LogOut size={16} />
          Log Out
        </button>
      </div>
    </div>
  );
};
