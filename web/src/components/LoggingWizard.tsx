import React, { useEffect, useState } from 'react';
import { ArrowLeft, Check } from 'lucide-react';
import { feelingsData } from '../data/feelingsData';
import type { LogEntry } from '../types';

interface LoggingWizardProps {
  onAddLog: (entry: LogEntry) => Promise<void> | void;
}

function generateLogId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

export const LoggingWizard: React.FC<LoggingWizardProps> = ({ onAddLog }) => {
  const [step, setStep] = useState(0); // 0: Start, 1: Core, 2: Sub1, 3: Sub2, 4: Intensity/Note, 5: Success
  const [selectedCore, setSelectedCore] = useState<string>('');
  const [selectedSub1, setSelectedSub1] = useState<string>('');
  const [selectedSub2, setSelectedSub2] = useState<string>('');
  const [intensity, setIntensity] = useState<number>(5);
  const [note, setNote] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Synced with 8s CSS @keyframes breathe (0-4s Inhale, 4-8s Exhale)
  const [breathingText, setBreathingText] = useState<'Inhale' | 'Exhale'>('Inhale');

  useEffect(() => {
    if (step !== 0) return;
    setBreathingText('Inhale');
    const interval = setInterval(() => {
      setBreathingText((prev) => (prev === 'Inhale' ? 'Exhale' : 'Inhale'));
    }, 4000);
    return () => clearInterval(interval);
  }, [step]);

  const handleStart = () => {
    setSelectedCore('');
    setSelectedSub1('');
    setSelectedSub2('');
    setIntensity(5);
    setNote('');
    setSubmitError(null);
    setStep(1);
  };

  const handleCoreSelect = (core: string) => {
    setSelectedCore(core);
    setSelectedSub1('');
    setSelectedSub2('');
    setStep(2);
  };

  const handleSub1Select = (sub1: string) => {
    setSelectedSub1(sub1);
    setSelectedSub2('');
    setStep(3);
  };

  const handleSub2Select = (sub2: string) => {
    setSelectedSub2(sub2);
    setSubmitError(null);
    setStep(4);
  };

  const handleSubmit = async () => {
    if (isSubmitting) return;
    const coreColor = feelingsData[selectedCore]?.color || '#5f27cd';
    const newLog: LogEntry = {
      id: generateLogId(),
      timestamp: new Date().toISOString(),
      core: selectedCore,
      sub1: selectedSub1,
      sub2: selectedSub2,
      intensity,
      note: note.trim(),
      color: coreColor,
    };

    setIsSubmitting(true);
    setSubmitError(null);
    try {
      await onAddLog(newLog);
      setStep(5);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Could not save your reflection. Please try again.';
      setSubmitError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBack = () => {
    setSubmitError(null);
    if (step > 0) {
      setStep((prev) => prev - 1);
    }
  };

  const coreDetail = selectedCore ? feelingsData[selectedCore] : undefined;
  const sub1Options = coreDetail ? Object.keys(coreDetail.sub1) : [];
  const sub2Options =
    coreDetail && selectedSub1 ? (coreDetail.sub1[selectedSub1] ?? []) : [];
  const coreColor = coreDetail?.color || '#5f27cd';

  const renderProgress = () => {
    if (step === 0 || step === 5) return null;
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '16px 24px 8px 24px',
          borderBottom: '1px solid var(--border-color)',
          height: '56px',
        }}
      >
        <button
          type="button"
          onClick={handleBack}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--text-sub)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            fontSize: '14px',
            gap: '6px',
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
                backgroundColor:
                  step === s ? 'var(--accent)' : s < step ? 'var(--text-sub)' : 'var(--border-color)',
                transition: 'background-color 0.3s ease',
              }}
            />
          ))}
        </div>
        <div style={{ width: '48px' }} />
      </div>
    );
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {renderProgress()}

      <div className="scroll-container">
        {/* STEP 0: HOME / START */}
        {step === 0 && (
          <div
            className="animate-fade-in"
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              minHeight: '400px',
              textAlign: 'center',
              paddingTop: '40px',
            }}
          >
            <div
              style={{
                background: 'var(--bg-card)',
                borderRadius: '24px',
                padding: '20px 28px',
                border: '1px solid var(--border-color)',
                marginBottom: '48px',
                boxShadow: 'var(--shadow-sm)',
              }}
            >
              <p
                style={{
                  fontSize: '20px',
                  fontWeight: 500,
                  color: 'var(--text-main)',
                  lineHeight: 1.4,
                }}
              >
                How are you feeling today?
              </p>
            </div>

            <button
              type="button"
              onClick={handleStart}
              aria-label="Start logging how you feel"
              className="breathing-bubble"
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
                fontFamily: 'inherit',
              }}
            >
              <span
                style={{
                  fontSize: '18px',
                  fontWeight: 500,
                  color: 'var(--accent)',
                  marginBottom: '4px',
                }}
              >
                {breathingText}
              </span>
              <span
                style={{
                  fontSize: '12px',
                  color: 'var(--text-sub)',
                }}
              >
                Tap to log
              </span>

              <div
                style={{
                  position: 'absolute',
                  top: '-12px',
                  left: '-12px',
                  right: '-12px',
                  bottom: '-12px',
                  borderRadius: '50%',
                  border: '1px solid var(--border-color)',
                  opacity: 0.3,
                  pointerEvents: 'none',
                }}
              />
            </button>
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
                    type="button"
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
                      fontFamily: 'inherit',
                    }}
                  >
                    <span style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text-main)' }}>
                      {core}
                    </span>
                    <span
                      style={{
                        width: '12px',
                        height: '12px',
                        borderRadius: '50%',
                        backgroundColor: color,
                      }}
                    />
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* STEP 2: LOG SUBEMOTION 1 */}
        {step === 2 && coreDetail && (
          <div className="animate-fade-in">
            <h1 className="title">Let&apos;s get more specific</h1>
            <p className="subtitle">
              Under{' '}
              <span style={{ color: coreColor, fontWeight: 600 }}>{selectedCore}</span>, which
              path fits best?
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {sub1Options.map((sub1) => (
                <button
                  key={sub1}
                  type="button"
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
                    alignItems: 'center',
                  }}
                >
                  <span>{sub1}</span>
                  <span style={{ fontSize: '18px', opacity: 0.4 }}>→</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* STEP 3: LOG SUBEMOTION 2 */}
        {step === 3 && coreDetail && (
          <div className="animate-fade-in">
            <h1 className="title">Do any of these options describe how you are feeling?</h1>
            <p className="subtitle">
              Refining{' '}
              <span style={{ fontWeight: 600 }}>
                {selectedCore} → {selectedSub1}
              </span>
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {sub2Options.map((sub2) => (
                <button
                  key={sub2}
                  type="button"
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
                    alignItems: 'center',
                  }}
                >
                  <span>{sub2}</span>
                  <Check size={16} style={{ color: coreColor }} />
                </button>
              ))}
            </div>
          </div>
        )}

        {/* STEP 4: RATE INTENSITY */}
        {step === 4 && (
          <div className="animate-fade-in" style={{ paddingBottom: '20px' }}>
            <h1 className="title">How intense is this emotion?</h1>
            <p className="subtitle">
              {selectedCore} → {selectedSub1} →{' '}
              <span style={{ color: coreColor, fontWeight: 600 }}>{selectedSub2}</span>
            </p>

            <div
              className="card"
              style={{ marginBottom: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}
            >
              <div style={{ textAlign: 'center', margin: '12px 0' }}>
                <span
                  style={{
                    fontSize: '56px',
                    fontWeight: 600,
                    color: coreColor,
                  }}
                >
                  {intensity}
                </span>
                <p
                  style={{
                    fontSize: '14px',
                    color: 'var(--text-sub)',
                    marginTop: '4px',
                    fontWeight: 500,
                  }}
                >
                  {intensity <= 3
                    ? 'Mild'
                    : intensity <= 6
                      ? 'Moderate'
                      : intensity <= 8
                        ? 'Intense'
                        : 'Overwhelming'}
                </p>
              </div>

              <input
                type="range"
                min="1"
                max="10"
                aria-label="Emotion intensity from 1 to 10"
                value={intensity}
                onChange={(e) => setIntensity(parseInt(e.target.value, 10))}
                style={{
                  width: '100%',
                  height: '6px',
                  borderRadius: '3px',
                  accentColor: coreColor,
                }}
              />

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  padding: '0 4px',
                  fontSize: '12px',
                  color: 'var(--text-muted)',
                }}
              >
                <span>1 (Subtle)</span>
                <span>10 (Peak)</span>
              </div>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label
                htmlFor="journal-reflection"
                style={{
                  display: 'block',
                  fontSize: '14px',
                  fontWeight: 500,
                  color: 'var(--text-sub)',
                  marginBottom: '8px',
                }}
              >
                Journal reflection (Optional)
              </label>
              <textarea
                id="journal-reflection"
                className="input"
                maxLength={2000}
                placeholder="What is causing this feeling? Writing it down can bring clarity..."
                value={note}
                onChange={(e) => setNote(e.target.value)}
                style={{
                  height: '110px',
                  resize: 'none',
                  lineHeight: '1.5',
                }}
              />
            </div>

            {submitError && (
              <p
                role="alert"
                style={{
                  fontSize: '13px',
                  color: '#eb5e55',
                  marginBottom: '12px',
                  textAlign: 'center',
                }}
              >
                {submitError}
              </p>
            )}

            <button
              type="button"
              onClick={() => void handleSubmit()}
              disabled={isSubmitting}
              className="btn btn-primary"
              style={{
                backgroundColor: coreColor,
                boxShadow: `0 4px 14px ${coreColor}44`,
              }}
            >
              {isSubmitting ? 'Saving...' : 'Log Reflection'}
            </button>
          </div>
        )}

        {/* STEP 5: SUCCESS / COMPLETE */}
        {step === 5 && (
          <div
            className="animate-fade-in"
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              minHeight: '400px',
              textAlign: 'center',
              paddingTop: '20px',
            }}
          >
            <div
              style={{
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
                boxShadow: 'var(--shadow-sm)',
              }}
            >
              ✓
            </div>

            <h2 className="title" style={{ fontSize: '24px', marginBottom: '12px' }}>
              Reflection Logged
            </h2>

            <p className="subtitle" style={{ maxWidth: '300px', marginBottom: '40px' }}>
              Your feelings have been recorded. Acknowledge this moment and take a deep breath.
            </p>

            <div
              className="card"
              style={{
                background: 'var(--bg-card)',
                borderLeft: `4px solid ${coreColor}`,
                padding: '16px 20px',
                width: '100%',
                maxWidth: '320px',
                textAlign: 'left',
                marginBottom: '40px',
              }}
            >
              <span
                style={{
                  fontSize: '11px',
                  color: 'var(--text-muted)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                }}
              >
                Logged Emotion
              </span>
              <p
                style={{
                  fontSize: '16px',
                  fontWeight: 600,
                  color: 'var(--text-main)',
                  marginTop: '4px',
                }}
              >
                {selectedSub2}{' '}
                <span style={{ fontSize: '13px', fontWeight: 400, color: 'var(--text-sub)' }}>
                  ({selectedSub1})
                </span>
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
                <span
                  style={{
                    fontSize: '12px',
                    padding: '2px 8px',
                    borderRadius: '10px',
                    backgroundColor: 'var(--border-color)',
                    color: 'var(--text-sub)',
                  }}
                >
                  Intensity: {intensity}
                </span>
              </div>
            </div>

            <button
              type="button"
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
