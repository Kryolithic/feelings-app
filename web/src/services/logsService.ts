import {
  collection,
  doc,
  getDoc,
  getDocs,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  writeBatch,
} from 'firebase/firestore';
import { db, isFirebaseConfigured } from '../lib/firebase';
import type { LogEntry, User } from '../types';
import { getInitialMockLogs, getLogsStorageKey, safeJsonParse } from './storage';

export function subscribeToLogs(
  user: User,
  onData: (logs: LogEntry[]) => void,
  onError?: (err: Error) => void,
): () => void {
  if (isFirebaseConfigured && db && !user.isDemo) {
    const logsQuery = query(
      collection(db, 'users', user.uid, 'logs'),
      orderBy('timestamp', 'desc'),
    );

    return onSnapshot(
      logsQuery,
      (snapshot) => {
        const entries: LogEntry[] = snapshot.docs.map((docSnap) => {
          const data = docSnap.data();
          return {
            id: typeof data.id === 'string' ? data.id : docSnap.id,
            timestamp: typeof data.timestamp === 'string' ? data.timestamp : new Date().toISOString(),
            core: typeof data.core === 'string' ? data.core : 'Happy',
            sub1: typeof data.sub1 === 'string' ? data.sub1 : '',
            sub2: typeof data.sub2 === 'string' ? data.sub2 : '',
            intensity: typeof data.intensity === 'number' ? data.intensity : 5,
            note: typeof data.note === 'string' ? data.note : '',
            color: typeof data.color === 'string' ? data.color : '#5f27cd',
          };
        });
        onData(entries);
      },
      (err) => {
        onError?.(err);
      },
    );
  }

  const key = getLogsStorageKey(user.uid);
  const raw = localStorage.getItem(key);
  if (raw === null) {
    const initial = getInitialMockLogs();
    localStorage.setItem(key, JSON.stringify(initial));
    onData(initial);
  } else {
    const parsed = safeJsonParse<LogEntry[]>(raw, []).sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
    );
    onData(parsed);
  }

  return () => {};
}

export async function addLogEntry(
  user: User,
  entry: LogEntry,
  currentLogs: LogEntry[],
): Promise<LogEntry[]> {
  if (isFirebaseConfigured && db && !user.isDemo) {
    await setDoc(doc(db, 'users', user.uid, 'logs', entry.id), {
      ...entry,
      createdAt: serverTimestamp(),
    });
    return [entry, ...currentLogs];
  }

  const updated = [entry, ...currentLogs].sort(
    (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
  );
  localStorage.setItem(getLogsStorageKey(user.uid), JSON.stringify(updated));
  return updated;
}

export async function clearAllLogs(user: User): Promise<void> {
  if (isFirebaseConfigured && db && !user.isDemo) {
    const snapshot = await getDocs(collection(db, 'users', user.uid, 'logs'));
    const docs = snapshot.docs;
    const chunkSize = 450;
    for (let i = 0; i < docs.length; i += chunkSize) {
      const batch = writeBatch(db);
      for (const docSnap of docs.slice(i, i + chunkSize)) {
        batch.delete(docSnap.ref);
      }
      await batch.commit();
    }
    return;
  }

  localStorage.setItem(getLogsStorageKey(user.uid), JSON.stringify([]));
}

export async function loadThemePreference(user: User): Promise<boolean | null> {
  if (!isFirebaseConfigured || !db || user.isDemo) return null;
  try {
    const snap = await getDoc(doc(db, 'users', user.uid));
    if (!snap.exists()) return null;
    const data = snap.data();
    return typeof data.darkTheme === 'boolean' ? data.darkTheme : null;
  } catch {
    return null;
  }
}

export async function saveThemePreference(user: User, isDark: boolean): Promise<void> {
  if (!isFirebaseConfigured || !db || user.isDemo) return;
  try {
    await setDoc(
      doc(db, 'users', user.uid),
      { darkTheme: isDark, updatedAt: serverTimestamp() },
      { merge: true },
    );
  } catch {
    // Best-effort preference sync
  }
}
