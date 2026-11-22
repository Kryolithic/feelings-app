import React, { useEffect, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import moodyLogoDark from '../assets/moody-logo-dark.png';
import moodyLogoLight from '../assets/moody-logo-light.png';
import type { User } from '../types';

interface LoginScreenProps {
  isDarkTheme: boolean;
  isFirebaseEnabled: boolean;
  onFirebaseSignIn: () => Promise<void>;
  onEmailSignIn: (email: string, password: string) => Promise<void>;
  onEmailSignUp: (email: string, password: string, displayName: string) => Promise<void>;
  onSendPasswordReset: (email: string) => Promise<void>;
  onDemoLogin: (user: User) => void;
}

const DEMO_ACCOUNTS: User[] = [
  { uid: 'demo-gary', name: 'Gary', email: 'gary@gmail.com', avatar: '', isDemo: true, authProvider: 'demo' },
  { uid: 'demo-dev', name: 'Developer User', email: 'developer@feelings.app', avatar: '', isDemo: true, authProvider: 'demo' },
];

export const LoginScreen: React.FC<LoginScreenProps> = ({
  isDarkTheme,
  isFirebaseEnabled,
  onFirebaseSignIn,
  onEmailSignIn,
  onEmailSignUp,
  onSendPasswordReset,
  onDemoLogin,
}) => {
  const [showChooser, setShowChooser] = useState(false);
  const [isSigningIn, setIsSigningIn] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);
  const [authNotice, setAuthNotice] = useState<string | null>(null);

  const [mode, setMode] = useState<'signin' | 'signup'>('signin');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (!showChooser) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowChooser(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [showChooser]);

  const handleGoogleSignInClick = async () => {
    setAuthError(null);
    setAuthNotice(null);
    if (isFirebaseEnabled) {
      setIsSigningIn(true);
      try {
        await onFirebaseSignIn();
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : 'Unable to sign in with Google right now.';
        setAuthError(message);
      } finally {
        setIsSigningIn(false);
      }
      return;
    }

    setShowChooser(true);
  };

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError(null);
    setAuthNotice(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail || !trimmedEmail.includes('@')) {
      setAuthError('Please enter a valid email address.');
      return;
    }
    if (password.length < 6) {
      setAuthError('Password must be at least 6 characters.');
      return;
    }

    if (!isFirebaseEnabled) {
      const derivedName = name.trim() || trimmedEmail.split('@')[0] || 'moody User';
      onDemoLogin({
        uid: `demo-${trimmedEmail.toLowerCase()}`,
        name: derivedName,
        email: trimmedEmail,
        avatar: '',
        isDemo: true,
        authProvider: 'demo',
      });
      return;
    }

    setIsSigningIn(true);
    try {
      if (mode === 'signup') {
        await onEmailSignUp(trimmedEmail, password, name.trim());
      } else {
        await onEmailSignIn(trimmedEmail, password);
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Unable to sign in right now.';
      setAuthError(message);
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleForgotPassword = async () => {
    setAuthError(null);
    setAuthNotice(null);
    const trimmedEmail = email.trim();
    if (!trimmedEmail || !trimmedEmail.includes('@')) {
      setAuthError('Enter your email address first to reset your password.');
      return;
    }

    setIsSigningIn(true);
    try {
      await onSendPasswordReset(trimmedEmail);
      setAuthNotice(`Password reset email sent to ${trimmedEmail}.`);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Could not send reset email.';
      setAuthError(message);
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleSelectDemoAccount = (account: User) => {
    setShowChooser(false);
    onDemoLogin(account);
  };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        flex: 1,
        minHeight: 0,
        height: '100%',
        overflowY: 'auto',
        WebkitOverflowScrolling: 'touch',
        padding: '28px 28px calc(28px + var(--safe-area-bottom)) 28px',
        position: 'relative',
      }}
      className="animate-fade-in"
    >
      {/* Decorative calm background elements */}
      <div
        style={{
          position: 'absolute',
          top: '10%',
          left: '10%',
          width: '120px',
          height: '120px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(155, 93, 229, 0.15) 0%, rgba(255,255,255,0) 70%)',
          zIndex: 0,
          pointerEvents: 'none',
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: '20%',
          right: '10%',
          width: '180px',
          height: '180px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(95, 39, 205, 0.1) 0%, rgba(255,255,255,0) 70%)',
          zIndex: 0,
          pointerEvents: 'none',
        }}
      />

      <div style={{ margin: 'auto 0', width: '100%', zIndex: 1 }}>
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <h1
            style={{
              margin: '0 auto 14px auto',
              display: 'flex',
              justifyContent: 'center',
            }}
          >
            <img
              src={isDarkTheme ? moodyLogoDark : moodyLogoLight}
              alt="moody"
              style={{
                width: '180px',
                maxWidth: '64%',
                height: 'auto',
                display: 'block',
              }}
            />
          </h1>
          <p className="subtitle" style={{ maxWidth: '280px', margin: '0 auto' }}>
            Welcome to your space of calm. Take a moment to check in with yourself.
          </p>
        </div>

        {/* Mode toggle */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '6px',
            padding: '4px',
            borderRadius: '14px',
            background: 'var(--bg-card)',
            border: '1px solid var(--border-color)',
            marginBottom: '16px',
          }}
        >
          <button
            type="button"
            disabled={isSigningIn}
            onClick={() => {
              setMode('signin');
              setAuthError(null);
              setAuthNotice(null);
            }}
            style={{
              padding: '9px 12px',
              borderRadius: '10px',
              border: 'none',
              cursor: 'pointer',
              fontFamily: 'inherit',
              fontSize: '13px',
              fontWeight: mode === 'signin' ? 600 : 500,
              background: mode === 'signin' ? 'var(--accent-light)' : 'transparent',
              color: mode === 'signin' ? 'var(--accent)' : 'var(--text-sub)',
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            disabled={isSigningIn}
            onClick={() => {
              setMode('signup');
              setAuthError(null);
              setAuthNotice(null);
            }}
            style={{
              padding: '9px 12px',
              borderRadius: '10px',
              border: 'none',
              cursor: 'pointer',
              fontFamily: 'inherit',
              fontSize: '13px',
              fontWeight: mode === 'signup' ? 600 : 500,
              background: mode === 'signup' ? 'var(--accent-light)' : 'transparent',
              color: mode === 'signup' ? 'var(--accent)' : 'var(--text-sub)',
            }}
          >
            Create Account
          </button>
        </div>

        {/* Email & Password Form */}
        <form
          onSubmit={(e) => void handleEmailSubmit(e)}
          style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}
        >
          {mode === 'signup' && (
            <input
              type="text"
              placeholder="Name (optional)"
              autoComplete="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={isSigningIn}
              className="input"
            />
          )}

          <input
            type="email"
            placeholder="Email address"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={isSigningIn}
            className="input"
            required
          />

          <div style={{ position: 'relative' }}>
            <input
              type={showPassword ? 'text' : 'password'}
              placeholder="Password (6+ characters)"
              autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isSigningIn}
              className="input"
              style={{ paddingRight: '44px' }}
              required
            />
            <button
              type="button"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              onClick={() => setShowPassword((prev) => !prev)}
              style={{
                position: 'absolute',
                right: '12px',
                top: '50%',
                transform: 'translateY(-50%)',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                color: 'var(--text-muted)',
                display: 'flex',
                alignItems: 'center',
                padding: '4px',
              }}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>

          {mode === 'signin' && isFirebaseEnabled && (
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '-4px' }}>
              <button
                type="button"
                disabled={isSigningIn}
                onClick={() => void handleForgotPassword()}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--accent)',
                  fontSize: '12px',
                  fontWeight: 500,
                  cursor: 'pointer',
                  fontFamily: 'inherit',
                  padding: '2px 0',
                }}
              >
                Forgot password?
              </button>
            </div>
          )}

          <button
            type="submit"
            disabled={isSigningIn}
            className="btn btn-dark"
            style={{ marginTop: '4px' }}
          >
            {isSigningIn
              ? 'Please wait...'
              : mode === 'signup'
                ? 'Create Account'
                : 'Sign In'}
          </button>
        </form>

        {/* Divider */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            margin: '18px 0',
            color: 'var(--text-muted)',
            fontSize: '12px',
          }}
        >
          <div style={{ flex: 1, height: '1px', background: 'var(--border-color)' }} />
          <span>or</span>
          <div style={{ flex: 1, height: '1px', background: 'var(--border-color)' }} />
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <button
            type="button"
            onClick={() => void handleGoogleSignInClick()}
            disabled={isSigningIn}
            className="btn btn-secondary"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '12px',
            }}
          >
            {/* Official 4-color Google G icon */}
            <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="#4285F4"
                d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17Z"
              />
              <path
                fill="#34A853"
                d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.14C3.26 21.3 7.31 24 12 24Z"
              />
              <path
                fill="#FBBC05"
                d="M5.28 14.24c-.24-.72-.38-1.49-.38-2.24s.14-1.52.38-2.24V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.99-3.14Z"
              />
              <path
                fill="#EA4335"
                d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.99 3.14c.95-2.85 3.6-4.96 6.72-4.96Z"
              />
            </svg>
            <span>Continue with Google</span>
          </button>

          {authNotice && (
            <p
              style={{
                fontSize: '13px',
                color: 'var(--accent)',
                textAlign: 'center',
                lineHeight: 1.4,
              }}
            >
              {authNotice}
            </p>
          )}

          {authError && (
            <p
              role="alert"
              style={{
                fontSize: '13px',
                color: '#eb5e55',
                textAlign: 'center',
                lineHeight: 1.4,
              }}
            >
              {authError}
            </p>
          )}

          {!isFirebaseEnabled && (
            <div
              style={{
                fontSize: '12px',
                color: 'var(--text-sub)',
                textAlign: 'center',
                padding: '8px 12px',
                borderRadius: '12px',
                background: 'var(--bg-card)',
                border: '1px solid var(--border-color)',
              }}
            >
              Demo Mode (add Firebase config to enable cloud auth &amp; sync)
            </div>
          )}
        </div>
      </div>

      {/* Demo Account Chooser Dialog when Firebase env vars are not configured */}
      {showChooser && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="demo-chooser-title"
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.45)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '24px',
            zIndex: 10,
          }}
          onClick={() => setShowChooser(false)}
        >
          <div
            style={{
              background: 'var(--bg-card-solid)',
              borderRadius: '24px',
              width: '100%',
              maxWidth: '360px',
              padding: '28px',
              boxShadow: 'var(--shadow-lg)',
              border: '1px solid var(--border-color)',
            }}
            className="animate-scale-in"
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ textAlign: 'center', marginBottom: '24px' }}>
              <div
                style={{
                  fontSize: '22px',
                  fontWeight: 'bold',
                  marginBottom: '8px',
                  letterSpacing: '-0.5px',
                }}
              >
                <span style={{ color: '#4285F4' }}>G</span>
                <span style={{ color: '#EA4335' }}>o</span>
                <span style={{ color: '#FBBC05' }}>o</span>
                <span style={{ color: '#4285F4' }}>g</span>
                <span style={{ color: '#34A853' }}>l</span>
                <span style={{ color: '#EA4335' }}>e</span>
              </div>
              <h2
                id="demo-chooser-title"
                style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text-main)' }}
              >
                Choose a demo account
              </h2>
              <p style={{ fontSize: '13px', color: 'var(--text-sub)' }}>
                to continue to moody
              </p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {DEMO_ACCOUNTS.map((acc) => (
                <button
                  key={acc.uid}
                  type="button"
                  onClick={() => handleSelectDemoAccount(acc)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '14px',
                    padding: '12px 16px',
                    borderRadius: '16px',
                    border: '1px solid var(--border-color)',
                    background: 'var(--bg-card)',
                    cursor: 'pointer',
                    width: '100%',
                    textAlign: 'left',
                    fontFamily: 'inherit',
                    transition: 'background 0.2s ease',
                  }}
                  className="btn-secondary"
                >
                  <div
                    style={{
                      width: '40px',
                      height: '40px',
                      borderRadius: '50%',
                      background: 'var(--accent-light)',
                      color: 'var(--accent)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 600,
                      fontSize: '16px',
                      flexShrink: 0,
                    }}
                  >
                    {acc.name.charAt(0).toUpperCase()}
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                    <span style={{ fontSize: '14px', fontWeight: 500, color: 'var(--text-main)' }}>
                      {acc.name}
                    </span>
                    <span
                      style={{
                        fontSize: '12px',
                        color: 'var(--text-sub)',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                    >
                      {acc.email}
                    </span>
                  </div>
                </button>
              ))}

              <button
                type="button"
                onClick={() =>
                  handleSelectDemoAccount({
                    uid: 'demo-guest',
                    name: 'Guest User',
                    email: 'guest@feelings.app',
                    avatar: '',
                    isDemo: true,
                  })
                }
                style={{
                  padding: '12px',
                  borderRadius: '14px',
                  border: '1px dashed var(--border-color)',
                  background: 'transparent',
                  color: 'var(--text-sub)',
                  cursor: 'pointer',
                  fontSize: '13px',
                  fontWeight: 500,
                }}
              >
                Use guest account
              </button>
            </div>

            <p
              style={{
                fontSize: '11px',
                color: 'var(--text-muted)',
                marginTop: '24px',
                textAlign: 'center',
                lineHeight: 1.4,
              }}
            >
              Local demo mode stores entries in your browser. Configure Firebase in{' '}
              <code>web/.env.local</code> for Google Auth and Cloud Firestore.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
