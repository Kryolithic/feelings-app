import {
  getRedirectResult,
  onAuthStateChanged,
  signInWithPopup,
  signInWithRedirect,
  signOut,
  type User as FirebaseUser,
} from 'firebase/auth';
import { doc, serverTimestamp, setDoc } from 'firebase/firestore';
import { auth, db, googleProvider, isFirebaseConfigured } from '../lib/firebase';
import type { User } from '../types';
import { DEMO_USER_KEY, safeJsonParse } from './storage';

function mapFirebaseUser(fbUser: FirebaseUser): User {
  return {
    uid: fbUser.uid,
    name: fbUser.displayName?.trim() || 'moody User',
    email: fbUser.email?.trim() || '',
    avatar: fbUser.photoURL?.trim() || '',
    isDemo: false,
  };
}

async function upsertUserProfile(fbUser: FirebaseUser): Promise<void> {
  if (!db) return;
  try {
    const mapped = mapFirebaseUser(fbUser);
    await setDoc(
      doc(db, 'users', fbUser.uid),
      {
        uid: mapped.uid,
        name: mapped.name,
        email: mapped.email,
        avatar: mapped.avatar,
        updatedAt: serverTimestamp(),
      },
      { merge: true },
    );
  } catch (err) {
    console.warn('Could not sync user profile to Firestore:', err);
  }
}

export function subscribeToAuth(callback: (user: User | null) => void): () => void {
  if (isFirebaseConfigured && auth) {
    getRedirectResult(auth)
      .then((result) => {
        if (result?.user) {
          void upsertUserProfile(result.user);
        }
      })
      .catch((err) => {
        console.warn('Redirect sign-in check failed:', err);
      });

    return onAuthStateChanged(auth, (fbUser) => {
      callback(fbUser ? mapFirebaseUser(fbUser) : null);
    });
  }

  const demoUser = safeJsonParse<User | null>(
    localStorage.getItem(DEMO_USER_KEY),
    null,
  );
  callback(demoUser);
  return () => {};
}

export async function signInWithGoogleFirebase(): Promise<User | null> {
  if (!isFirebaseConfigured || !auth || !googleProvider) {
    throw new Error('Firebase authentication is not configured.');
  }

  try {
    const result = await signInWithPopup(auth, googleProvider);
    await upsertUserProfile(result.user);
    return mapFirebaseUser(result.user);
  } catch (err: unknown) {
    const code =
      typeof err === 'object' && err !== null && 'code' in err
        ? String((err as { code?: unknown }).code)
        : '';

    if (code === 'auth/popup-blocked') {
      await signInWithRedirect(auth, googleProvider);
      return null;
    }

    if (code === 'auth/popup-closed-by-user' || code === 'auth/cancelled-popup-request') {
      return null;
    }

    throw err;
  }
}

export function signInDemoAccount(user: User): void {
  const demoUser: User = { ...user, isDemo: true };
  localStorage.setItem(DEMO_USER_KEY, JSON.stringify(demoUser));
}

export async function signOutUser(): Promise<void> {
  localStorage.removeItem(DEMO_USER_KEY);
  if (isFirebaseConfigured && auth && auth.currentUser) {
    await signOut(auth);
  }
}
