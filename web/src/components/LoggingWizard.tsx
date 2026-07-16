import React, { useState, useEffect } from 'react';
import { feelingsData } from '../data/feelingsData';
import { ArrowLeft, Check } from 'lucide-react';

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

interface LoggingWizardProps {
  onAddLog: (entry: LogEntry) => void;
}

export const LoggingWizard: React.FC<LoggingWizardProps> = ({ onAddLog }) => {
  const [step, setStep] = useState(0); // 0: Home/Start, 1: Core, 2: Sub1, 3: Sub2, 4: Intensity/Note, 5: Success
  const [selectedCore, setSelectedCore] = useState<string>('');
  const [selectedSub1, setSelectedSub1] = useState<string>('');
  const [selectedSub2, setSelectedSub2] = useState<string>('');
  const [intensity, setIntensity] = useState<number>(5);
  const [note, setNote] = useState<string>('');
  
  // Breathing animation message
  const [breathingText, setBreathingText] = useState('Inhale');

  useEffect(() => {
    if (step === 0) {
      const interval = setInterval(() => {
        setBreathingText((prev) => (prev === 'Inhale' ? 'Exhale' : 'Inhale'));
      }, 4000);
      return () => clearInterval(interval);
    }
  }, [step]);

  const handleStart = () => {
    setSelectedCore('');
    setSelectedSub1('');
    setSelectedSub2('');
    setIntensity(5);
    setNote('');
    setStep(1);
  };

  const handleCoreSelect = (core: string) => {
    setSelectedCore(core);
    setStep(2);
  };

  const handleSub1Select = (sub1: string) => {
    setSelectedSub1(sub1);
    setStep(3);
  };

  const handleSub2Select = (sub2: string) => {
    setSelectedSub2(sub2);
    setStep(4);
  };

  const handleSubmit = () => {
    const newLog: LogEntry = {
      id: Math.random().toString(36).substr(2, 9),
      timestamp: new Date().toISOString(),
      core: selectedCore,
      sub1: selectedSub1,
      sub2: selectedSub2,
      intensity,
      note,
      color: feelingsData[selectedCore]?.color || 'var(--accent)'
    };
    onAddLog(newLog);
    setStep(5);
  };

  const handleBack = () => {
    if (step > 0) {
      setStep(step - 1);
    }
  };

  const renderProgress = () => {
    if (step === 0 || step === 5) return null;
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '16px 24px 8px 24px',
        borderBottom: '1px solid var(--border-color)',
        height: '56px'
      }}>
        <button 
          onClick={handleBack}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--text-sub)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            fontSize: '14px',
            gap: '6px'
          }}
        >
          <ArrowLeft size={18} />
          Back
        </button>
        <div style={{ display: 'flex', gap: '6px' }}>
          {[1, 2, 3, 4].map((s) => (
            <div 
              key={s} 
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                backgroundColor: step === s ? 'var(--accent)' : s < step ? 'var(--text-sub)' : 'var(--border-color)',
                transition: 'background-color 0.3s ease'
              }}
            />
          ))}
        </div>
        <div style={{ width: '48px' }} /> {/* Spacer */}
      </div>
    );
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {renderProgress()}

      <div className="scroll-container">
        
        {/* STEP 0: HOME / START */}
        {step === 0 && (
          <div className="animate-fade-in" style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%',
            minHeight: '400px',
            textAlign: 'center',
            paddingTop: '40px'
          }}>
            <div style={{
              background: 'var(--bg-card)',
              borderRadius: '24px',
              padding: '20px 28px',
              border: '1px solid var(--border-color)',
              marginBottom: '48px',
              boxShadow: 'var(--shadow-sm)'
            }}>
              <p style={{
                fontSize: '20px',
                fontWeight: 500,
                color: 'var(--text-main)',
                lineHeight: 1.4
              }}>How are you feeling today?</p>
            </div>

            {/* Pulsing breathing bubble */}
            <div 
              onClick={handleStart}
              style={{
                width: '180px',
                height: '180px',
                borderRadius: '50%',
                background: 'var(--accent-light)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                border: '2px solid var(--border-color)',
                position: 'relative',
                boxShadow: 'var(--shadow-md)',
                transform: breathingText === 'Inhale' ? 'scale(1.05)' : 'scale(0.95)',
                transition: 'transform 4s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.3s ease',
              }}
            >
              <span style={{
                fontSize: '18px',
                fontWeight: 500,
                color: 'var(--accent)',
                marginBottom: '4px'
              }}>{breathingText}</span>
              <span style={{
                fontSize: '12px',
                color: 'var(--text-sub)'
              }}>Tap to log</span>
              
              {/* Outer decorative rings */}
              <div style={{
                position: 'absolute',
                top: '-12px',
                left: '-12px',
                right: '-12px',
                bottom: '-12px',
                borderRadius: '50%',
                border: '1px solid var(--border-color)',
                opacity: 0.3,
                pointerEvents: 'none'
              }} />
            </div>
          </div>
        )}

        {/* STEP 1: LOG MAIN EMOTION */}
        {step === 1 && (
          <div className="animate-fade-in">
            <h1 className="title">Select your main feeling</h1>
            <p className="subtitle">Choose the core emotion that matches your current state.</p>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '12px' }}>
              {Object.keys(feelingsData).map((core) => {
                const color = feelingsData[core].color;
                return (
                  <button
                    key={core}
                    onClick={() => handleCoreSelect(core)}
                    className="card"
                    style={{
                      borderLeft: `6px solid ${color}`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '18px 24px',
                      cursor: 'pointer',
                      background: 'var(--bg-card)',
                      width: '100%',
                      textAlign: 'left',
                      fontFamily: 'inherit'
                    }}
                  >
                    <span style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text-main)' }}>{core}</span>
                    <span style={{
                      width: '12px',
                      height: '12px',
                      borderRadius: '50%',
                      backgroundColor: color
                    }} />
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* STEP 2: LOG SUBEMOTION 1 */}
        {step === 2 && (
          <div className="animate-fade-in">
            <h1 className="title">Let's get more specific</h1>
            <p className="subtitle">Under <span style={{ color: feelingsData[selectedCore]?.color, fontWeight: 600 }}>{selectedCore}</span>, which path fits best?</p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {Object.keys(feelingsData[selectedCore].sub1).map((sub1) => (
                <button
                  key={sub1}
                  onClick={() => handleSub1Select(sub1)}
                  className="card"
                  style={{
                    padding: '16px 20px',
                    fontSize: '16px',
                    fontWeight: 500,
                    textAlign: 'left',
                    cursor: 'pointer',
                    background: 'var(--bg-card)',
                    color: 'var(--text-main)',
                    fontFamily: 'inherit',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                >
                  <span>{sub1}</span>
                  <span style={{ fontSize: '18px', opacity: 0.3 }}>→</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* STEP 3: LOG SUBEMOTION 2 */}
        {step === 3 && (
          <div className="animate-fade-in">
            <h1 className="title">Do any of these options describe how you are feeling?</h1>
            <p className="subtitle">Refining <span style={{ fontWeight: 600 }}>{selectedCore} → {selectedSub1}</span></p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {feelingsData[selectedCore].sub1[selectedSub1].map((sub2) => (
                <button
                  key={sub2}
                  onClick={() => handleSub2Select(sub2)}
                  className="card"
                  style={{
                    padding: '16px 20px',
                    fontSize: '16px',
                    fontWeight: 500,
                    textAlign: 'left',
                    cursor: 'pointer',
                    background: 'var(--bg-card)',
                    color: 'var(--text-main)',
                    fontFamily: 'inherit',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                >
                  <span>{sub2}</span>
                  <Check size={16} style={{ color: feelingsData[selectedCore]?.color }} />
                </button>
              ))}
            </div>
          </div>
        )}

        {/* STEP 4: RATE INTENSITY */}
        {step === 4 && (
          <div className="animate-fade-in" style={{ paddingBottom: '20px' }}>
            <h1 className="title">How intense is this emotion?</h1>
            <p className="subtitle">Rate the strength of your feeling, and add optional notes.</p>

            <div className="card" style={{ marginBottom: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ textAlign: 'center', margin: '12px 0' }}>
                <span style={{
                  fontSize: '56px',
                  fontWeight: 600,
                  color: feelingsData[selectedCore]?.color
                }}>{intensity}</span>
                <p style={{
                  fontSize: '14px',
                  color: 'var(--text-sub)',
                  marginTop: '4px',
                  fontWeight: 500
                }}>
                  {intensity <= 3 ? 'Mild' : intensity <= 6 ? 'Moderate' : intensity <= 8 ? 'Intense' : 'Overwhelming'}
                </p>
              </div>

              <input 
                type="range" 
                min="1" 
                max="10" 
                value={intensity} 
                onChange={(e) => setIntensity(parseInt(e.target.value))}
                style={{
                  width: '100%',
                  height: '6px',
                  borderRadius: '3px',
                  outline: 'none',
                  accentColor: feelingsData[selectedCore]?.color
                }} 
              />
              
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0 4px', fontSize: '12px', color: 'var(--text-muted)' }}>
                <span>1 (Subtle)</span>
                <span>10 (Peak)</span>
              </div>
            </div>

            <div style={{ marginBottom: '32px' }}>
              <label style={{
                display: 'block',
                fontSize: '14px',
                fontWeight: 500,
                color: 'var(--text-sub)',
                marginBottom: '8px'
              }}>Journal reflection (Optional)</label>
              <textarea 
                className="input"
                placeholder="What is causing this feeling? Writing it down can bring clarity..."
                value={note}
                onChange={(e) => setNote(e.target.value)}
                style={{
                  height: '110px',
                  resize: 'none',
                  lineHeight: '1.5'
                }}
              />
            </div>

            <button 
              onClick={handleSubmit}
              className="btn btn-primary"
              style={{
                backgroundColor: feelingsData[selectedCore]?.color || 'var(--accent)',
                boxShadow: `0 4px 14px ${feelingsData[selectedCore]?.color}44`
              }}
            >
              Log Reflection
            </button>
          </div>
        )}

        {/* STEP 5: SUCCESS / COMPLETE */}
        {step === 5 && (
          <div className="animate-fade-in" style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%',
            minHeight: '400px',
            textAlign: 'center',
            paddingTop: '20px'
          }}>
            <div style={{
              width: '80px',
              height: '80px',
              borderRadius: '50%',
              backgroundColor: 'var(--accent-light)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent)',
              marginBottom: '32px',
              fontSize: '36px',
              boxShadow: 'var(--shadow-sm)'
            }}>
              ✓
            </div>

            <h2 className="title" style={{ fontSize: '24px', marginBottom: '12px' }}>Reflection Logged</h2>
            
            <p className="subtitle" style={{ maxWidth: '300px', marginBottom: '40px' }}>
              Your feelings have been recorded. Acknowledge this moment and take a deep breath.
            </p>

            <div className="card" style={{
              background: 'var(--bg-card)',
              borderLeft: `4px solid ${feelingsData[selectedCore]?.color}`,
              padding: '16px 20px',
              width: '100%',
              maxWidth: '320px',
              textAlign: 'left',
              marginBottom: '40px'
            }}>
              <span style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Logged Emotion</span>
              <p style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-main)', marginTop: '4px' }}>
                {selectedSub2} <span style={{ fontSize: '13px', fontWeight: 400, color: 'var(--text-sub)' }}>({selectedSub1})</span>
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
                <span style={{
                  fontSize: '12px',
                  padding: '2px 8px',
                  borderRadius: '10px',
                  backgroundColor: 'var(--border-color)',
                  color: 'var(--text-sub)'
                }}>Intensity: {intensity}</span>
              </div>
            </div>

            <button 
              onClick={() => setStep(0)}
              className="btn btn-secondary"
              style={{ width: 'auto', paddingLeft: '32px', paddingRight: '32px' }}
            >
              Back to Home
            </button>
          </div>
        )}

      </div>
    </div>
  );
};
