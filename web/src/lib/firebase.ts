import { getApp, getApps, initializeApp, type FirebaseApp } from 'firebase/app';
import { connectAuthEmulator, getAuth, GoogleAuthProvider, type Auth } from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore, type Firestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY?.trim() ?? '',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN?.trim() ?? '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID?.trim() ?? '',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET?.trim() ?? '',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID?.trim() ?? '',
  appId: import.meta.env.VITE_FIREBASE_APP_ID?.trim() ?? '',
};

const isRealValue = (value: string): boolean =>
  value.length > 0 && !value.startsWith('YOUR_');

export const isFirebaseConfigured: boolean =
  isRealValue(firebaseConfig.apiKey) &&
  isRealValue(firebaseConfig.authDomain) &&
  isRealValue(firebaseConfig.projectId) &&
  isRealValue(firebaseConfig.appId);

let app: FirebaseApp | null = null;
let auth: Auth | null = null;
let db: Firestore | null = null;
let googleProvider: GoogleAuthProvider | null = null;
let emulatorsConnected = false;

if (isFirebaseConfigured) {
  app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
  auth = getAuth(app);
  db = getFirestore(app);
  googleProvider = new GoogleAuthProvider();
  googleProvider.setCustomParameters({ prompt: 'select_account' });

  if (import.meta.env.VITE_USE_FIREBASE_EMULATORS === 'true' && !emulatorsConnected) {
    connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
    connectFirestoreEmulator(db, '127.0.0.1', 8080);
    emulatorsConnected = true;
  }
}

export { app, auth, db, googleProvider };
