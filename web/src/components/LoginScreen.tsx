import React, { useState } from 'react';

interface User {
  name: string;
  email: string;
  avatar: string;
}

interface LoginScreenProps {
  onLogin: (user: User) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLogin }) => {
  const [showChooser, setShowChooser] = useState(false);

  const mockAccounts = [
    { name: 'Gary', email: 'gary@gmail.com', avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&h=150&q=80' },
    { name: 'Developer User', email: 'developer@feelings.app', avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&h=150&q=80' }
  ];

  const handleSelectAccount = (account: typeof mockAccounts[0]) => {
    setShowChooser(false);
    onLogin({
      name: account.name,
      email: account.email,
      avatar: account.avatar
    });
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      justifyContent: 'center',
      padding: '32px',
      position: 'relative'
    }} className="animate-fade-in">
      
      {/* Decorative calm background elements */}
      <div style={{
        position: 'absolute',
        top: '10%',
        left: '10%',
        width: '120px',
        height: '120px',
        borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(155, 93, 229, 0.15) 0%, rgba(255,255,255,0) 70%)',
        zIndex: 0,
        pointerEvents: 'none'
      }} />
      <div style={{
        position: 'absolute',
        bottom: '20%',
        right: '10%',
        width: '180px',
        height: '180px',
        borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(95, 39, 205, 0.1) 0%, rgba(255,255,255,0) 70%)',
        zIndex: 0,
        pointerEvents: 'none'
      }} />

      <div style={{ zIndex: 1, textAlign: 'center', marginBottom: '40px' }}>
        <div style={{
          width: '72px',
          height: '72px',
          borderRadius: '24px',
          background: 'var(--accent-light)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 24px auto',
          color: 'var(--accent)',
          fontSize: '32px',
          boxShadow: 'var(--shadow-sm)'
        }}>
          ✿
        </div>
        <h1 className="title" style={{ fontSize: '32px', marginBottom: '12px' }}>Serene</h1>
        <p className="subtitle" style={{ maxWidth: '280px', margin: '0 auto' }}>
          Welcome to your space of calm. Take a moment to check in with yourself.
        </p>
      </div>

      <div style={{ zIndex: 1 }}>
        <button 
          onClick={() => setShowChooser(true)}
          className="btn btn-primary"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '12px',
            backgroundColor: 'var(--text-main)',
            boxShadow: '0 4px 12px rgba(0,0,0,0.08)'
          }}
        >
          {/* Custom minimal Google logo */}
          <svg width="18" height="18" viewBox="0 0 24 24" style={{ marginRight: '4px' }}>
            <path fill="#ffffff" d="M12.24 10.285V14.4h6.887c-.648 2.41-2.519 4.114-5.147 4.114-3.342 0-6.058-2.716-6.058-6.057 0-3.342 2.716-6.058 6.058-6.058 1.455 0 2.784.516 3.825 1.37l3.058-3.057C18.99 2.923 15.82 1.8 12.24 1.8 6.474 1.8 1.8 6.474 1.8 12.24S6.474 22.68 12.24 22.68c5.766 0 10.44-4.674 10.44-10.44 0-.648-.058-1.286-.18-1.955H12.24z"/>
          </svg>
          Sign In with Google
        </button>
      </div>

      {/* Account Chooser Dialog (Google OAuth Simulation) */}
      {showChooser && (
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.4)',
          backdropFilter: 'blur(4px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '24px',
          zIndex: 10
        }} onClick={() => setShowChooser(false)}>
          
          <div style={{
            background: 'var(--bg-card-solid)',
            borderRadius: '24px',
            width: '100%',
            maxWidth: '360px',
            padding: '28px',
            boxShadow: 'var(--shadow-lg)',
            border: '1px solid var(--border-color)'
          }} className="animate-scale-in" onClick={(e) => e.stopPropagation()}>
            
            <div style={{ textAlign: 'center', marginBottom: '24px' }}>
              {/* Google logo text */}
              <div style={{ 
                fontFamily: 'Product Sans, Arial, sans-serif', 
                fontSize: '22px', 
                fontWeight: 'bold', 
                marginBottom: '8px',
                letterSpacing: '-0.5px' 
              }}>
                <span style={{ color: '#4285F4' }}>G</span>
                <span style={{ color: '#EA4335' }}>o</span>
                <span style={{ color: '#FBBC05' }}>o</span>
                <span style={{ color: '#4285F4' }}>g</span>
                <span style={{ color: '#34A853' }}>l</span>
                <span style={{ color: '#EA4335' }}>e</span>
              </div>
              <h2 style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text-main)' }}>Choose an account</h2>
              <p style={{ fontSize: '13px', color: 'var(--text-sub)' }}>to continue to Serene</p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {mockAccounts.map((acc, index) => (
                <button
                  key={index}
                  onClick={() => handleSelectAccount(acc)}
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
                    transition: 'background 0.2s ease'
                  }}
                  className="btn-secondary"
                >
                  <img 
                    src={acc.avatar} 
                    alt={acc.name} 
                    style={{
                      width: '40px',
                      height: '40px',
                      borderRadius: '50%',
                      objectFit: 'cover',
                      border: '1px solid rgba(0,0,0,0.05)'
                    }} 
                  />
                  <div style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                    <span style={{ fontSize: '14px', fontWeight: 500, color: 'var(--text-main)' }}>{acc.name}</span>
                    <span style={{ fontSize: '12px', color: 'var(--text-sub)', overflow: 'hidden', textOverflow: 'ellipsis' }}>{acc.email}</span>
                  </div>
                </button>
              ))}

              <button
                onClick={() => handleSelectAccount({
                  name: 'Guest User',
                  email: 'guest@feelings.app',
                  avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&h=150&q=80'
                })}
                style={{
                  padding: '12px',
                  borderRadius: '14px',
                  border: '1px dashed var(--border-color)',
                  background: 'transparent',
                  color: 'var(--text-sub)',
                  cursor: 'pointer',
                  fontSize: '13px',
                  fontWeight: 500
                }}
              >
                Use guest account
              </button>
            </div>
            
            <p style={{
              fontSize: '11px',
              color: 'var(--text-muted)',
              marginTop: '24px',
              textAlign: 'center',
              lineHeight: 1.4
            }}>
              To continue, Google will share your name, email address, profile picture, and language preference with Serene.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
