# myMood Web App

A minimal, calming emotion tracking web application built with React, TypeScript, Vite, and Firebase (Google Authentication, Cloud Firestore, and Firebase Hosting), with automatic local demo fallback when Firebase environment variables are not set.

## 1. Local Development (Demo Mode)

Without any Firebase configuration, the app runs out of the box in **Local Demo Mode** (account chooser + browser `localStorage` persistence):

```bash
cd web
npm install
npm run dev
```

## 2. Importing into Firebase Studio

This repository includes `.idx/dev.nix` at the repo root so Firebase Studio automatically installs `web/` dependencies and starts the Vite preview server on port `$PORT`.

1. Open [Firebase Studio](https://studio.firebase.google.com/) and choose **Import repo**.
2. Paste your repository URL (`https://github.com/Kryolithic/feelings-app`).
3. Once the workspace boots, copy `web/.env.example` to `web/.env.local` and fill in your Firebase Web App credentials.

## 3. Connecting a Firebase Project

1. Create or open a project in the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Authentication → Sign-in method → Google** and set a project support email.
3. Under **Authentication → Settings → Authorized domains**, ensure your Hosting domain (and any Firebase Studio preview domain) is listed.
4. Create a **Cloud Firestore** database.
5. Register a Web App in **Project Settings**, copy the config values into `web/.env.local` (based on `web/.env.example`), and set your project ID in the root `.firebaserc`.

## 4. Running with Firebase Emulators

To test Google Auth and Firestore rules locally without touching production data:

1. Set `VITE_USE_FIREBASE_EMULATORS=true` in `web/.env.local` (with your `.env.local` Firebase keys filled in, or any placeholder project ID).
2. From the repo root, start the emulators:
   ```bash
   npx -y firebase-tools@latest emulators:start
   ```
3. In another terminal, start the web app:
   ```bash
   cd web
   npm run dev
   ```

## 5. Deploying to Firebase Hosting & Firestore

From the repository root:

```bash
npx -y firebase-tools@latest login
npx -y firebase-tools@latest deploy
```

`firebase.json` automatically runs `npm --prefix web run build` (`predeploy`), deploys `web/dist` with SPA rewrites, and publishes `firestore.rules` and `firestore.indexes.json`.
