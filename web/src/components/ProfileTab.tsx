import React, { useEffect, useState } from 'react';
import { Link2, LogOut, Moon, Shield, Sun, Trash2 } from 'lucide-react';
import type { User } from '../types';

interface ProfileTabProps {
  user: User;
  isDarkTheme: boolean;
  onToggleTheme: () => void;
  onLogout: () => Promise<void> | void;
  onResetData: () => Promise<void> | void;
  onSendPasswordReset?: (email: string) => Promise<void>;
}

export const ProfileTab: React.FC<ProfileTabProps> = ({
  user,
  isDarkTheme,
  onToggleTheme,
  onLogout,
  onResetData,
  onSendPasswordReset,
}) => {
  const [imgError, setImgError] = useState(false);
  const [showResetModal, setShowResetModal] = useState(false);
  const [isResetting, setIsResetting] = useState(false);
  const [resetSent, setResetSent] = useState(false);
  const [resetError, setResetError] = useState<string | null>(null);

  useEffect(() => {
    setImgError(false);
  }, [user.avatar]);

  useEffect(() => {
    if (!showResetModal) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isResetting) {
        setShowResetModal(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [showResetModal, isResetting]);

  const handleConfirmReset = async () => {
    setIsResetting(true);
    try {
      await onResetData();
      setShowResetModal(false);
    } finally {
      setIsResetting(false);
    }
  };

  const showAvatarImage = Boolean(user.avatar) && !imgError;

  return (
    <div className="scroll-container animate-fade-in">
      <h1 className="title">Profile</h1>
      <p className="subtitle">Manage your personal settings and app preferences.</p>

      {/* User Card */}
      <div
        className="card"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '20px',
          marginBottom: '28px',
          padding: '24px',
        }}
      >
        {showAvatarImage ? (
          <img
            src={user.avatar}
            alt={user.name}
            referrerPolicy="no-referrer"
            onError={() => setImgError(true)}
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              objectFit: 'cover',
              border: '2px solid var(--accent)',
              flexShrink: 0,
            }}
          />
        ) : (
          <div
            aria-hidden="true"
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              background: 'var(--accent-light)',
              color: 'var(--accent)',
              border: '2px solid var(--accent)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '24px',
              fontWeight: 600,
              flexShrink: 0,
            }}
          >
            {user.name.charAt(0).toUpperCase() || 'M'}
          </div>
        )}
        <div style={{ display: 'flex', flexDirection: 'column', textAlign: 'left', overflow: 'hidden' }}>
          <h2
            style={{
              fontSize: '18px',
              fontWeight: 600,
              color: 'var(--text-main)',
              marginBottom: '4px',
            }}
          >
            {user.name}
          </h2>
          <span
            style={{
              fontSize: '14px',
              color: 'var(--text-sub)',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {user.email}
          </span>
        </div>
      </div>

      {/* Preferences Section */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '28px' }}>
        <div
          className="card"
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '16px 20px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {isDarkTheme ? (
              <Moon size={18} style={{ color: 'var(--accent)' }} />
            ) : (
              <Sun size={18} style={{ color: 'var(--accent)' }} />
            )}
            <span style={{ fontSize: '15px', fontWeight: 500 }}>Theme Mode</span>
          </div>
          <button
            type="button"
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
            }}
          >
            {isDarkTheme ? 'Silent Night' : 'Calm Dawn'}
          </button>
        </div>

        <div
          className="card"
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '16px 20px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Link2 size={18} style={{ color: 'var(--text-sub)' }} />
            <span style={{ fontSize: '15px', fontWeight: 500 }}>Connections</span>
          </div>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
            {user.isDemo ? 'Local Demo Mode' : 'Firebase Cloud Sync'}
          </span>
        </div>

        <div
          className="card"
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '16px 20px',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Shield size={18} style={{ color: 'var(--text-sub)' }} />
            <span style={{ fontSize: '15px', fontWeight: 500 }}>Password</span>
          </div>
          {user.isDemo ? (
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Local Demo Account</span>
          ) : user.authProvider === 'apple' ? (
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Managed by Apple</span>
          ) : user.authProvider === 'password' ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                {resetSent ? 'Reset email sent' : 'Email & Password'}
              </span>
              {onSendPasswordReset && user.email && (
                <button
                  type="button"
                  onClick={() => {
                    setResetError(null);
                    setResetSent(false);
                    void onSendPasswordReset(user.email)
                      .then(() => setResetSent(true))
                      .catch((err: unknown) => {
                        setResetError(
                          err instanceof Error ? err.message : 'Could not send reset email.',
                        );
                      });
                  }}
                  style={{
                    padding: '4px 10px',
                    borderRadius: '8px',
                    border: 'none',
                    background: 'var(--accent-light)',
                    color: 'var(--accent)',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  Reset
                </button>
              )}
            </div>
          ) : (
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Managed by Google</span>
          )}
        </div>

        {resetError && (
          <p style={{ fontSize: '12px', color: '#eb5e55', textAlign: 'right', paddingRight: '4px' }}>
            {resetError}
          </p>
        )}
      </div>

      {/* Action Zone */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <button
          type="button"
          onClick={() => setShowResetModal(true)}
          className="btn btn-secondary"
          style={{
            borderColor: 'rgba(235, 94, 85, 0.35)',
            color: '#eb5e55',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '10px',
          }}
        >
          <Trash2 size={16} />
          Reset History
        </button>

        <button
          type="button"
          onClick={() => void onLogout()}
          className="btn btn-dark"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '10px',
          }}
        >
          <LogOut size={16} />
          Log Out
        </button>
      </div>

      {/* Calming Reset Confirmation Dialog */}
      {showResetModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="reset-dialog-title"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.45)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '24px',
            zIndex: 20,
          }}
          onClick={() => {
            if (!isResetting) setShowResetModal(false);
          }}
        >
          <div
            className="animate-scale-in"
            style={{
              background: 'var(--bg-card-solid)',
              borderRadius: '24px',
              width: '100%',
              maxWidth: '340px',
              padding: '24px',
              boxShadow: 'var(--shadow-lg)',
              border: '1px solid var(--border-color)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h2
              id="reset-dialog-title"
              style={{ fontSize: '18px', fontWeight: 600, color: 'var(--text-main)', marginBottom: '8px' }}
            >
              Reset History?
            </h2>
            <p style={{ fontSize: '14px', color: 'var(--text-sub)', lineHeight: 1.5, marginBottom: '24px' }}>
              Are you sure you want to clear your feelings log history? This action cannot be undone.
            </p>
            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                type="button"
                disabled={isResetting}
                onClick={() => setShowResetModal(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={isResetting}
                onClick={() => void handleConfirmReset()}
                className="btn"
                style={{
                  backgroundColor: '#eb5e55',
                  color: '#ffffff',
                }}
              >
                {isResetting ? 'Clearing...' : 'Clear History'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
