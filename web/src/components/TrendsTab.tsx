import React, { useState } from 'react';
import { Calendar, TrendingUp } from 'lucide-react';

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

interface TrendsTabProps {
  logs: LogEntry[];
}

export const TrendsTab: React.FC<TrendsTabProps> = ({ logs }) => {
  const [selectedDayOffset, setSelectedDayOffset] = useState<number>(new Date().getDay()); // 0-6 (Sun-Sat)

  // Get date for current week days
  const getWeekDates = () => {
    const current = new Date();
    const sunday = new Date(current.setDate(current.getDate() - current.getDay()));
    const days = [];
    for (let i = 0; i < 7; i++) {
      const date = new Date(sunday);
      date.setDate(sunday.getDate() + i);
      days.push(date);
    }
    return days;
  };

  const weekDates = getWeekDates();
  const dayNames = ['S', 'M', 'T', 'W', 'Th', 'F', 'S'];

  // Filter logs for a specific day
  const getLogsForDay = (date: Date) => {
    return logs.filter(log => {
      const logDate = new Date(log.timestamp);
      return logDate.getDate() === date.getDate() &&
             logDate.getMonth() === date.getMonth() &&
             logDate.getFullYear() === date.getFullYear();
    });
  };

  // Get dominant color/emotion for a day
  const getDominantEmotionForDay = (date: Date) => {
    const dayLogs = getLogsForDay(date);
    if (dayLogs.length === 0) return null;
    // Find the most frequent emotion, or take the latest
    return dayLogs[dayLogs.length - 1];
  };

  const selectedDate = weekDates[selectedDayOffset];
  const selectedDayLogs = getLogsForDay(selectedDate);

  // Prepare graph data (last 7 logs chronological)
  const sortedLogs = [...logs]
    .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
    .slice(-7); // Last 7 logs

  const renderSVGGraph = () => {
    if (sortedLogs.length < 2) {
      return (
        <div style={{
          height: '160px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--text-muted)',
          fontSize: '14px',
          border: '1.5px dashed var(--border-color)',
          borderRadius: '16px',
          padding: '20px',
          textAlign: 'center'
        }}>
          <div>
            <TrendingUp size={24} style={{ margin: '0 auto 8px auto', opacity: 0.5 }} />
            <p>Log your feelings on multiple days to view your emotional trends chart.</p>
          </div>
        </div>
      );
    }

    const width = 360;
    const height = 140;
    const padding = 25;
    
    // Scale functions
    const getX = (index: number) => {
      return padding + (index * (width - padding * 2) / (sortedLogs.length - 1));
    };

    const getY = (intensity: number) => {
      // Intensity 1 to 10 mapped to height - padding (at bottom) down to padding (at top)
      return height - padding - ((intensity - 1) * (height - padding * 2) / 9);
    };

    // Build the SVG path points
    const points = sortedLogs.map((log, index) => ({
      x: getX(index),
      y: getY(log.intensity),
      color: log.color,
      core: log.core,
      sub2: log.sub2,
      intensity: log.intensity
    }));

    // Draw line
    let pathD = `M ${points[0].x} ${points[0].y}`;
    for (let i = 1; i < points.length; i++) {
      pathD += ` L ${points[i].x} ${points[i].y}`;
    }

    // Draw fill gradient path (closing the shape at bottom)
    const fillD = `${pathD} L ${points[points.length - 1].x} ${height - padding} L ${points[0].x} ${height - padding} Z`;

    return (
      <div style={{ position: 'relative' }}>
        <svg width="100%" height={height} viewBox={`0 0 ${width} ${height}`} style={{ overflow: 'visible' }}>
          <defs>
            <linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.15" />
              <stop offset="100%" stopColor="var(--accent)" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* Grid lines */}
          <line x1={padding} y1={padding} x2={width - padding} y2={padding} stroke="var(--border-color)" strokeDasharray="3 3" />
          <line x1={padding} y1={height/2} x2={width - padding} y2={height/2} stroke="var(--border-color)" strokeDasharray="3 3" />
          <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} stroke="var(--border-color)" />

          {/* Area fill */}
          <path d={fillD} fill="url(#chartGradient)" />

          {/* Path line */}
          <path d={pathD} fill="none" stroke="var(--accent)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />

          {/* Points */}
          {points.map((pt, i) => (
            <g key={i}>
              <circle
                cx={pt.x}
                cy={pt.y}
                r="5"
                fill={pt.color}
                stroke="var(--bg-card-solid)"
                strokeWidth="1.5"
                style={{ filter: 'drop-shadow(0px 2px 4px rgba(0,0,0,0.1))', cursor: 'pointer' }}
              />
              <title>{`${pt.sub2} (${pt.intensity})`}</title>
            </g>
          ))}

          {/* X axis labels (Dates) */}
          {sortedLogs.map((log, index) => {
            const date = new Date(log.timestamp);
            const label = `${date.getMonth() + 1}/${date.getDate()}`;
            return (
              <text
                key={index}
                x={getX(index)}
                y={height - 8}
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
      </div>
    );
  };

  return (
    <div className="scroll-container animate-fade-in">
      <h1 className="title">Trends</h1>
      <p className="subtitle">Observe the patterns in your emotions over time.</p>

      {/* Section 1: Chart */}
      <div className="card" style={{ marginBottom: '28px', padding: '24px 18px 18px 18px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', padding: '0 6px' }}>
          <span style={{ fontSize: '15px', fontWeight: 600, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <TrendingUp size={16} style={{ color: 'var(--accent)' }} />
            Intensity Trend
          </span>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Last 7 checks</span>
        </div>
        {renderSVGGraph()}
      </div>

      {/* Section 2: Calendar */}
      <div className="card" style={{ marginBottom: '28px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px', fontWeight: 600, fontSize: '15px' }}>
          <Calendar size={16} style={{ color: 'var(--accent)' }} />
          <span>By Day</span>
        </div>

        {/* Week Days List */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(7, 1fr)',
          gap: '4px',
          marginBottom: '10px'
        }}>
          {weekDates.map((date, index) => {
            const isSelected = selectedDayOffset === index;
            const dominantLog = getDominantEmotionForDay(date);
            const isToday = new Date().getDate() === date.getDate() &&
                            new Date().getMonth() === date.getMonth();

            return (
              <button
                key={index}
                onClick={() => setSelectedDayOffset(index)}
                style={{
                  background: isSelected ? 'var(--accent-light)' : 'transparent',
                  border: 'none',
                  borderRadius: '12px',
                  padding: '8px 4px',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  cursor: 'pointer',
                  outline: 'none',
                  transition: 'background 0.2s ease',
                  borderWidth: '1px',
                  borderStyle: isToday ? 'solid' : 'none',
                  borderColor: isToday ? 'var(--accent)' : 'transparent'
                }}
              >
                <span style={{
                  fontSize: '11px',
                  fontWeight: 500,
                  color: isSelected ? 'var(--accent)' : 'var(--text-muted)',
                  marginBottom: '6px'
                }}>{dayNames[index]}</span>
                
                <span style={{
                  fontSize: '14px',
                  fontWeight: 600,
                  color: isSelected ? 'var(--accent)' : 'var(--text-main)',
                  marginBottom: '10px'
                }}>{date.getDate()}</span>
                
                {/* Mood Dot */}
                <div style={{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: dominantLog ? dominantLog.color : 'transparent',
                  border: dominantLog ? 'none' : '1px dashed var(--text-muted)',
                  opacity: dominantLog ? 1 : 0.25
                }} />
              </button>
            );
          })}
        </div>
      </div>

      {/* Section 3: Daily Logs details list */}
      <div>
        <h2 style={{
          fontSize: '14px',
          fontWeight: 600,
          color: 'var(--text-sub)',
          marginBottom: '12px',
          textTransform: 'uppercase',
          letterSpacing: '0.5px'
        }}>
          {selectedDate.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' })}
        </h2>

        {selectedDayLogs.length === 0 ? (
          <div className="card" style={{
            textAlign: 'center',
            padding: '30px 20px',
            color: 'var(--text-muted)',
            fontSize: '14px',
            borderStyle: 'dashed'
          }}>
            <p>No entries for this day.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {selectedDayLogs.map((log) => {
              const timeString = new Date(log.timestamp).toLocaleTimeString('en-US', {
                hour: '2-digit',
                minute: '2-digit'
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
                    gap: '8px'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>{timeString}</span>
                    <span style={{
                      fontSize: '11px',
                      padding: '2px 8px',
                      borderRadius: '10px',
                      backgroundColor: 'var(--accent-light)',
                      color: 'var(--accent)',
                      fontWeight: 600
                    }}>Intensity: {log.intensity}</span>
                  </div>

                  <p style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-main)' }}>
                    {log.sub2}
                    <span style={{ fontSize: '13px', fontWeight: 400, color: 'var(--text-sub)', marginLeft: '6px' }}>
                      ({log.sub1})
                    </span>
                  </p>

                  {log.note && (
                    <p style={{
                      fontSize: '14px',
                      color: 'var(--text-sub)',
                      lineHeight: '1.4',
                      borderLeft: '2px solid var(--border-color)',
                      paddingLeft: '10px',
                      marginTop: '4px',
                      fontStyle: 'italic'
                    }}>
                      "{log.note}"
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
