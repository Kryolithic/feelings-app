import React, { useMemo, useState } from 'react';
import { Calendar, ChevronLeft, ChevronRight, TrendingUp } from 'lucide-react';
import type { LogEntry } from '../types';

interface TrendsTabProps {
  logs: LogEntry[];
}

function getWeekDates(weekOffset: number): Date[] {
  const now = new Date();
  const sunday = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate() - now.getDay() + weekOffset * 7,
  );
  const days: Date[] = [];
  for (let i = 0; i < 7; i++) {
    days.push(new Date(sunday.getFullYear(), sunday.getMonth(), sunday.getDate() + i));
  }
  return days;
}

function getLogsForDay(logs: LogEntry[], date: Date): LogEntry[] {
  return logs
    .filter((log) => {
      const logDate = new Date(log.timestamp);
      return (
        logDate.getDate() === date.getDate() &&
        logDate.getMonth() === date.getMonth() &&
        logDate.getFullYear() === date.getFullYear()
      );
    })
    .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
}

function getDominantEmotionForDay(logs: LogEntry[], date: Date): LogEntry | null {
  const dayLogs = getLogsForDay(logs, date); // sorted newest-first
  if (dayLogs.length === 0) return null;

  const counts = new Map<string, number>();
  for (const log of dayLogs) {
    counts.set(log.core, (counts.get(log.core) ?? 0) + 1);
  }

  let bestCore = dayLogs[0].core;
  let bestCount = 0;
  for (const log of dayLogs) {
    const count = counts.get(log.core) ?? 0;
    if (count > bestCount) {
      bestCount = count;
      bestCore = log.core;
    }
  }

  return dayLogs.find((log) => log.core === bestCore) ?? dayLogs[0];
}

export const TrendsTab: React.FC<TrendsTabProps> = ({ logs }) => {
  const [selectedDayOffset, setSelectedDayOffset] = useState<number>(() => new Date().getDay());
  const [weekOffset, setWeekOffset] = useState<number>(0);
  const [selectedPointIndex, setSelectedPointIndex] = useState<number | null>(null);

  const now = new Date();
  const weekDates = useMemo(() => getWeekDates(weekOffset), [weekOffset]);
  const dayNames = ['S', 'M', 'T', 'W', 'Th', 'F', 'S'];

  const selectedDate = weekDates[selectedDayOffset] ?? weekDates[0];
  const selectedDayLogs = useMemo(
    () => getLogsForDay(logs, selectedDate),
    [logs, selectedDate],
  );

  // Last 7 logs in chronological order
  const sortedLogs = useMemo(
    () =>
      [...logs]
        .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
        .slice(-7),
    [logs],
  );

  const handleJumpToday = () => {
    setWeekOffset(0);
    setSelectedDayOffset(new Date().getDay());
  };

  const renderSVGGraph = () => {
    if (sortedLogs.length < 2) {
      return (
        <div
          style={{
            height: '160px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--text-muted)',
            fontSize: '14px',
            border: '1.5px dashed var(--border-color)',
            borderRadius: '16px',
            padding: '20px',
            textAlign: 'center',
          }}
        >
          <div>
            <TrendingUp size={24} style={{ margin: '0 auto 8px auto', opacity: 0.5 }} />
            <p>Log your feelings multiple times to view your emotional trends chart.</p>
          </div>
        </div>
      );
    }

    const width = 360;
    const height = 150;
    const leftPadding = 28;
    const rightPadding = 20;
    const topPadding = 18;
    const bottomPadding = 28;

    const getX = (index: number) =>
      leftPadding + (index * (width - leftPadding - rightPadding)) / (sortedLogs.length - 1);

    const getY = (intensity: number) => {
      const clamped = Math.max(1, Math.min(10, intensity));
      return (
        height -
        bottomPadding -
        ((clamped - 1) * (height - topPadding - bottomPadding)) / 9
      );
    };

    const points = sortedLogs.map((log, index) => ({
      x: getX(index),
      y: getY(log.intensity),
      color: log.color,
      core: log.core,
      sub2: log.sub2,
      intensity: log.intensity,
      timestamp: log.timestamp,
    }));

    let pathD = `M ${points[0].x} ${points[0].y}`;
    for (let i = 1; i < points.length; i++) {
      pathD += ` L ${points[i].x} ${points[i].y}`;
    }

    const fillD = `${pathD} L ${points[points.length - 1].x} ${height - bottomPadding} L ${points[0].x} ${height - bottomPadding} Z`;

    const activePoint =
      selectedPointIndex !== null && selectedPointIndex < points.length
        ? points[selectedPointIndex]
        : null;

    return (
      <div style={{ position: 'relative' }}>
        <svg
          width="100%"
          height={height}
          viewBox={`0 0 ${width} ${height}`}
          style={{ overflow: 'visible' }}
          role="img"
          aria-label="Emotion intensity trend chart"
        >
          <defs>
            <linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.18" />
              <stop offset="100%" stopColor="var(--accent)" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* Y-axis labels */}
          <text
            x={leftPadding - 8}
            y={getY(10) + 3}
            textAnchor="end"
            fontSize="10"
            fill="var(--text-muted)"
          >
            10
          </text>
          <text
            x={leftPadding - 8}
            y={getY(5.5) + 3}
            textAnchor="end"
            fontSize="10"
            fill="var(--text-muted)"
          >
            5
          </text>
          <text
            x={leftPadding - 8}
            y={getY(1) + 3}
            textAnchor="end"
            fontSize="10"
            fill="var(--text-muted)"
          >
            1
          </text>

          {/* Grid lines */}
          <line
            x1={leftPadding}
            y1={getY(10)}
            x2={width - rightPadding}
            y2={getY(10)}
            stroke="var(--border-color)"
            strokeDasharray="3 3"
          />
          <line
            x1={leftPadding}
            y1={getY(5.5)}
            x2={width - rightPadding}
            y2={getY(5.5)}
            stroke="var(--border-color)"
            strokeDasharray="3 3"
          />
          <line
            x1={leftPadding}
            y1={getY(1)}
            x2={width - rightPadding}
            y2={getY(1)}
            stroke="var(--border-color)"
          />

          {/* Area fill */}
          <path d={fillD} fill="url(#chartGradient)" />

          {/* Path line */}
          <path
            d={pathD}
            fill="none"
            stroke="var(--accent)"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* Data Points */}
          {points.map((pt, i) => {
            const isSelected = selectedPointIndex === i;
            return (
              <g
                key={sortedLogs[i].id}
                onClick={() =>
                  setSelectedPointIndex((prev) => (prev === i ? null : i))
                }
                style={{ cursor: 'pointer' }}
              >
                {isSelected && (
                  <circle
                    cx={pt.x}
                    cy={pt.y}
                    r="10"
                    fill={pt.color}
                    fillOpacity="0.25"
                  />
                )}
                <circle
                  cx={pt.x}
                  cy={pt.y}
                  r={isSelected ? 6.5 : 5}
                  fill={pt.color}
                  stroke="var(--bg-card-solid)"
                  strokeWidth="1.5"
                  style={{ filter: 'drop-shadow(0px 2px 4px rgba(0,0,0,0.1))' }}
                />
                <title>{`${pt.sub2} (${pt.intensity}/10)`}</title>
              </g>
            );
          })}

          {/* X-axis labels */}
          {sortedLogs.map((log, index) => {
            const date = new Date(log.timestamp);
            const label = `${date.getMonth() + 1}/${date.getDate()}`;
            return (
              <text
                key={log.id}
                x={getX(index)}
                y={height - 6}
                textAnchor="middle"
                fontSize="10"
                fill="var(--text-muted)"
                fontWeight="500"
              >
                {label}
              </text>
            );
          })}
        </svg>

        {activePoint && (
          <div
            style={{
              marginTop: '10px',
              padding: '8px 12px',
              borderRadius: '12px',
              background: 'var(--bg-card-solid)',
              border: '1px solid var(--border-color)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              fontSize: '12px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span
                style={{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: activePoint.color,
                  display: 'inline-block',
                }}
              />
              <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                {activePoint.sub2}
              </span>
              <span style={{ color: 'var(--text-sub)' }}>({activePoint.core})</span>
            </div>
            <span style={{ color: 'var(--accent)', fontWeight: 600 }}>
              {activePoint.intensity}/10 ·{' '}
              {new Date(activePoint.timestamp).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
              })}
            </span>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="scroll-container animate-fade-in">
      <h1 className="title">Trends</h1>
      <p className="subtitle">Observe the patterns in your emotions over time.</p>

      {/* Section 1: Chart */}
      <div className="card" style={{ marginBottom: '28px', padding: '24px 18px 18px 18px' }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '16px',
            padding: '0 6px',
          }}
        >
          <span
            style={{
              fontSize: '15px',
              fontWeight: 600,
              color: 'var(--text-main)',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <TrendingUp size={16} style={{ color: 'var(--accent)' }} />
            Intensity Trend
          </span>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Last 7 checks</span>
        </div>
        {renderSVGGraph()}
      </div>

      {/* Section 2: Calendar */}
      <div className="card" style={{ marginBottom: '28px' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '18px',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontWeight: 600,
              fontSize: '15px',
            }}
          >
            <Calendar size={16} style={{ color: 'var(--accent)' }} />
            <span>By Day</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            {weekOffset !== 0 && (
              <button
                type="button"
                onClick={handleJumpToday}
                style={{
                  background: 'var(--accent-light)',
                  color: 'var(--accent)',
                  border: 'none',
                  borderRadius: '8px',
                  padding: '4px 8px',
                  fontSize: '11px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Today
              </button>
            )}
            <button
              type="button"
              aria-label="Previous week"
              onClick={() => setWeekOffset((prev) => prev - 1)}
              style={{
                background: 'none',
                border: '1px solid var(--border-color)',
                borderRadius: '8px',
                color: 'var(--text-sub)',
                width: '28px',
                height: '28px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
            >
              <ChevronLeft size={16} />
            </button>
            <button
              type="button"
              aria-label="Next week"
              disabled={weekOffset >= 0}
              onClick={() => setWeekOffset((prev) => Math.min(0, prev + 1))}
              style={{
                background: 'none',
                border: '1px solid var(--border-color)',
                borderRadius: '8px',
                color: weekOffset >= 0 ? 'var(--text-muted)' : 'var(--text-sub)',
                opacity: weekOffset >= 0 ? 0.4 : 1,
                width: '28px',
                height: '28px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: weekOffset >= 0 ? 'not-allowed' : 'pointer',
              }}
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>

        {/* Week Days List */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 1fr)',
            gap: '4px',
            marginBottom: '6px',
          }}
        >
          {weekDates.map((date, index) => {
            const isSelected = selectedDayOffset === index;
            const dominantLog = getDominantEmotionForDay(logs, date);
            const isToday =
              now.getFullYear() === date.getFullYear() &&
              now.getMonth() === date.getMonth() &&
              now.getDate() === date.getDate();

            return (
              <button
                key={date.toISOString()}
                type="button"
                onClick={() => setSelectedDayOffset(index)}
                style={{
                  background: isSelected ? 'var(--accent-light)' : 'transparent',
                  border: isToday ? '1px solid var(--accent)' : '1px solid transparent',
                  borderRadius: '12px',
                  padding: '8px 4px',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  cursor: 'pointer',
                  transition: 'background 0.2s ease',
                }}
              >
                <span
                  style={{
                    fontSize: '11px',
                    fontWeight: 500,
                    color: isSelected ? 'var(--accent)' : 'var(--text-muted)',
                    marginBottom: '6px',
                  }}
                >
                  {dayNames[index]}
                </span>

                <span
                  style={{
                    fontSize: '14px',
                    fontWeight: 600,
                    color: isSelected ? 'var(--accent)' : 'var(--text-main)',
                    marginBottom: '10px',
                  }}
                >
                  {date.getDate()}
                </span>

                <div
                  style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor: dominantLog ? dominantLog.color : 'transparent',
                    border: dominantLog ? 'none' : '1px dashed var(--text-muted)',
                    opacity: dominantLog ? 1 : 0.25,
                  }}
                />
              </button>
            );
          })}
        </div>
      </div>

      {/* Section 3: Daily Logs details list */}
      <div>
        <h2
          style={{
            fontSize: '14px',
            fontWeight: 600,
            color: 'var(--text-sub)',
            marginBottom: '12px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          {selectedDate.toLocaleDateString('en-US', {
            weekday: 'long',
            month: 'short',
            day: 'numeric',
          })}
        </h2>

        {selectedDayLogs.length === 0 ? (
          <div
            className="card"
            style={{
              textAlign: 'center',
              padding: '30px 20px',
              color: 'var(--text-muted)',
              fontSize: '14px',
              borderStyle: 'dashed',
            }}
          >
            <p>No entries for this day.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {selectedDayLogs.map((log) => {
              const timeString = new Date(log.timestamp).toLocaleTimeString('en-US', {
                hour: '2-digit',
                minute: '2-digit',
              });

              return (
                <div
                  key={log.id}
                  className="card"
                  style={{
                    borderLeft: `4px solid ${log.color}`,
                    padding: '16px 20px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '8px',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                      {timeString}
                    </span>
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '2px 8px',
                        borderRadius: '10px',
                        backgroundColor: 'var(--accent-light)',
                        color: 'var(--accent)',
                        fontWeight: 600,
                      }}
                    >
                      Intensity: {log.intensity}
                    </span>
                  </div>

                  <p style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-main)' }}>
                    {log.sub2}
                    <span
                      style={{
                        fontSize: '13px',
                        fontWeight: 400,
                        color: 'var(--text-sub)',
                        marginLeft: '6px',
                      }}
                    >
                      ({log.sub1})
                    </span>
                  </p>

                  {log.note && (
                    <p
                      style={{
                        fontSize: '14px',
                        color: 'var(--text-sub)',
                        lineHeight: '1.4',
                        borderLeft: '2px solid var(--border-color)',
                        paddingLeft: '10px',
                        marginTop: '4px',
                        fontStyle: 'italic',
                      }}
                    >
                      &ldquo;{log.note}&rdquo;
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
