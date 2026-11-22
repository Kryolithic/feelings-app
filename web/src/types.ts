export interface User {
  uid: string;
  name: string;
  email: string;
  avatar: string;
  isDemo?: boolean;
  authProvider?: 'google' | 'password' | 'apple' | 'demo';
}

export interface LogEntry {
  id: string;
  timestamp: string;
  core: string;
  sub1: string;
  sub2: string;
  intensity: number;
  note: string;
  color: string;
}
