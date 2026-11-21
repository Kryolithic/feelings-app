import { feelingsData } from '../data/feelingsData';
import type { LogEntry } from '../types';

export const THEME_STORAGE_KEY = 'mymood_theme_dark';
export const DEMO_USER_KEY = 'mymood_demo_user';

export const getLogsStorageKey = (uid: string): string => `mymood_logs_${uid}`;

export function safeJsonParse<T>(raw: string | null, fallback: T): T {
  if (!raw) return fallback;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export function getInitialMockLogs(): LogEntry[] {
  const now = new Date();
  const sampleTemplates = [
    {
      core: 'Happy',
      sub1: 'Content',
      sub2: 'Joyful',
      note: 'Had a wonderful lunch with friends today.',
      intensity: 7,
      hour: 10,
      minute: 15,
    },
    {
      core: 'Surprised',
      sub1: 'Amazed',
      sub2: 'Awe',
      note: 'Beautiful sunset on my evening walk.',
      intensity: 8,
      hour: 14,
      minute: 30,
    },
    {
      core: 'Bad',
      sub1: 'Tired',
      sub2: 'Sleepy',
      note: 'Did not sleep well last night, feeling slow.',
      intensity: 6,
      hour: 11,
      minute: 0,
    },
    {
      core: 'Happy',
      sub1: 'Proud',
      sub2: 'Confident',
      note: 'Finished a major milestone at work!',
      intensity: 9,
      hour: 16,
      minute: 45,
    },
    {
      core: 'Sad',
      sub1: 'Lonely',
      sub2: 'Isolated',
      note: 'Quiet evening indoors reflecting on the week.',
      intensity: 4,
      hour: 9,
      minute: 20,
    },
  ];

  const entries: LogEntry[] = sampleTemplates.map((sample, offset) => {
    const logDate = new Date(now);
    logDate.setDate(now.getDate() - offset);
    logDate.setHours(sample.hour, sample.minute, 0, 0);
    if (logDate.getTime() > now.getTime()) {
      logDate.setTime(now.getTime() - offset * 60 * 60 * 1000);
    }

    return {
      id: `mock-${offset}`,
      timestamp: logDate.toISOString(),
      core: sample.core,
      sub1: sample.sub1,
      sub2: sample.sub2,
      intensity: sample.intensity,
      note: sample.note,
      color: feelingsData[sample.core]?.color ?? '#5f27cd',
    };
  });

  return entries.sort(
    (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
  );
}
