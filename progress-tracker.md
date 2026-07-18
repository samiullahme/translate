# Progress Tracker

## Current Phase

- **Phase 0 — Foundations** (In Progress -> Complete)
- **Phase 1 — Core Document Loop** (Next Up)

## Current Goal

- **Step 02 — Capture + OCR**: Setup Camera and Gallery, integrate client image compression, create documents table operations, and connect server-side Google Cloud Vision OCR.

## Completed

- **Step 01 — App Shell & Authentication** (Completed)
  - Created a robust native Android App Shell using Jetpack Compose.
  - Implemented secure local session persistence with `SessionManager` via `SharedPreferences`.
  - Built a lightweight, fully standard Supabase GoTrue Auth client with Retrofit, Moshi, and KSP.
  - Configured edge-to-edge rendering and a stunning **AI-Forward Swiss Minimal** UI theme utilizing custom styled screens, display typography, and smooth transitions.
  - Provided a canonical SQL migration (`supabase/migration.sql`) including triggers for automatic profile creation and strict RLS policies.
  - Configured `.env.example` with placeholders for backend URLs and Supabase anon keys.

## Next Up

- **Step 02 — Capture + OCR**:
  1. Add camera and photo gallery scanning controls to HomeScreen.
  2. Implement client-side image compression (downscaling + JPEG compression).
  3. Wire server-side OCR via Google Cloud Vision API endpoint.
  4. Create `documents` table CRUD operations on Supabase with status flow (`processing` -> `ready` / `failed`).

## Architecture Decisions

- **Supabase native Auth integration**: Integrated standard GoTrue Auth API via Retrofit on Android to bypass heavy JS-targeted libraries, guaranteeing ultra-fast compilation and runtime stability.
- **Dynamic Session Hydration**: Implemented automatic state checks during app launch to resolve active user sessions and direct flow gracefully without flickering.
- **Strict Separation of Concerns**: Auth state is isolated in `AuthRepository` and managed seamlessly using stateful `AuthViewModel` binding.
