import {
  createUserWithEmailAndPassword,
  getRedirectResult,
  onAuthStateChanged,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signInWithRedirect,
  signOut,
  updateProfile,
  type User as FirebaseUser,
} from 'firebase/auth';
import { doc, serverTimestamp, setDoc } from 'firebase/firestore';
import { auth, db, googleProvider, isFirebaseConfigured } from '../lib/firebase';
import type { User } from '../types';
import { DEMO_USER_KEY, safeJsonParse } from './storage';

function resolveAuthProvider(fbUser: FirebaseUser): 'google' | 'password' | 'apple' {
  const providerIds = fbUser.providerData.map((p) => p.providerId);
  if (providerIds.includes('apple.com')) return 'apple';
  if (providerIds.includes('google.com')) return 'google';
  if (providerIds.includes('password')) return 'password';
  return 'password';
}

function deriveDisplayName(
  displayName: string | null | undefined,
  email: string | null | undefined,
): string {
  const trimmedName = displayName?.trim();
  if (trimmedName) return trimmedName.slice(0, 200);
  const emailPrefix = email?.split('@')[0]?.trim();
  if (emailPrefix) return emailPrefix.slice(0, 200);
  return 'moody User';
}

function formatFirebaseAuthError(err: unknown): Error {
  const code =
    typeof err === 'object' && err !== null && 'code' in err
      ? String((err as { code?: unknown }).code)
      : '';

  switch (code) {
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
    case 'auth/user-not-found':
      return new Error('Incorrect email or password.');
    case 'auth/email-already-in-use':
      return new Error('An account with this email already exists.');
    case 'auth/invalid-email':
      return new Error('Please enter a valid email address.');
    case 'auth/weak-password':
      return new Error('Password must be at least 6 characters.');
    case 'auth/operation-not-allowed':
      return new Error(
        'Email/Password sign-in is not enabled yet in Firebase Authentication Console.',
      );
    case 'auth/too-many-requests':
      return new Error('Too many attempts. Please wait a moment and try again.');
    default:
      return err instanceof Error ? err : new Error('Authentication failed. Please try again.');
  }
}

function mapFirebaseUser(fbUser: FirebaseUser, overrideName?: string): User {
  return {
    uid: fbUser.uid,
    name: deriveDisplayName(overrideName || fbUser.displayName, fbUser.email),
    email: fbUser.email?.trim() || '',
    avatar: fbUser.photoURL?.trim() || '',
    isDemo: false,
    authProvider: resolveAuthProvider(fbUser),
  };
}

async function upsertUserProfile(fbUser: FirebaseUser, overrideName?: string): Promise<void> {
  if (!db) return;
  try {
    const mapped = mapFirebaseUser(fbUser, overrideName);
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

export async function signInWithEmailFirebase(email: string, password: string): Promise<User> {
  if (!isFirebaseConfigured || !auth) {
    throw new Error('Firebase authentication is not configured.');
  }
  try {
    const result = await signInWithEmailAndPassword(auth, email.trim(), password);
    await upsertUserProfile(result.user);
    return mapFirebaseUser(result.user);
  } catch (err: unknown) {
    throw formatFirebaseAuthError(err);
  }
}

export async function signUpWithEmailFirebase(
  email: string,
  password: string,
  displayName?: string,
): Promise<User> {
  if (!isFirebaseConfigured || !auth) {
    throw new Error('Firebase authentication is not configured.');
  }
  try {
    const result = await createUserWithEmailAndPassword(auth, email.trim(), password);
    const trimmedName = displayName?.trim() || '';
    if (trimmedName) {
      await updateProfile(result.user, { displayName: trimmedName });
    }
    await upsertUserProfile(result.user, trimmedName || undefined);
    return mapFirebaseUser(result.user, trimmedName || undefined);
  } catch (err: unknown) {
    throw formatFirebaseAuthError(err);
  }
}

export async function sendPasswordResetFirebase(email: string): Promise<void> {
  if (!isFirebaseConfigured || !auth) {
    throw new Error('Firebase authentication is not configured.');
  }
  try {
    await sendPasswordResetEmail(auth, email.trim());
  } catch (err: unknown) {
    throw formatFirebaseAuthError(err);
  }
}

export function signInDemoAccount(user: User): void {
  const demoUser: User = { ...user, isDemo: true, authProvider: user.authProvider ?? 'demo' };
  localStorage.setItem(DEMO_USER_KEY, JSON.stringify(demoUser));
}

export async function signOutUser(): Promise<void> {
  localStorage.removeItem(DEMO_USER_KEY);
  if (isFirebaseConfigured && auth && auth.currentUser) {
    await signOut(auth);
  }
}
