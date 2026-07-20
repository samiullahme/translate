# Progress Tracker

Update this file after every meaningful implementation change.
**Be honest.** Never mark a step complete unless it ships with real APIs.

## Current Phase

- Phase 0 — Foundations (**complete**)
- Phase 1 — Core Document Loop (**in progress**)

## Current Goal

- **Step 03 — Translate, Summarize, and Save**: FastAPI translate/summarize, Document Viewer, AppUIDesign Home/History/Favorites/Profile languages.  
  Context prompts: `doctranslate-ai-context/prompts/step-03-translate-summarize.md`

## Completed

- **Step 02 — Capture + OCR** (Completed)
  - Camera and gallery on Home (`ActivityResultContracts`).
  - Client-side compression (`ImageCompressor`).
  - Pipeline stages in `ScannerViewModel` (Upload → Scan → Paper reveal).
  - FastAPI `POST /api/ocr` → API Ninjas `imagetotext` (Bearer auth; fail closed if key missing).
  - Paper reveal with exact OCR text.
  - HTTPS tunnel helper: `backend/tunnel.ps1`.
- **Step 01 — App Shell & Authentication** (Completed)
  - Jetpack Compose app shell + session (`SessionManager`).
  - Supabase GoTrue via Retrofit/Moshi.
  - Theme + auth screens.
  - Canonical `supabase/migration.sql` + RLS.
  - Root `.env` / `.env.example` for `SUPABASE_*` + `BACKEND_URL`.

## NOT done

- Step 03 — translate / summarize / Document Viewer / full AppUIDesign tabs & lists
- Step 04 — Cartesia SSE TTS karaoke + Profile voice picker
- Step 05 — Document chat + STT
- Step 06 — PDF OCR wiring + Storage polish + hardening

## Next Up

1. **Step 03** — Translate + Summarize + Save + Viewer + AppUIDesign library screens
2. Step 04 — TTS karaoke + voice picker
3. Step 05 — Chat + STT
4. Step 06 — PDF + polish

## Honest snapshot (Jul 21, 2026)

| Area | Reality |
| --- | --- |
| Stack | Native Android Kotlin Compose + FastAPI + Supabase (**not Expo**) |
| FastAPI | `/api/health`, `/api/ocr` only |
| UI design SoT | [`samiullahme/AppUIDesign`](https://github.com/samiullahme/AppUIDesign) — match as each step ships |
| Files bento | Show for design parity; real PDF OCR in Step 06 |

## Locked decisions

- OCR = API Ninjas only (no Google Vision, no demo OCR)
- LLM = OpenAI-compatible (`OPENAI_BASE_URL` / Vercel AI Gateway OK)
- Cartesia pins: `CARTESIA_VERSION=2024-06-10`, `CARTESIA_MODEL=sonic-english`
- `BACKEND_URL` = public HTTPS FastAPI base (Emergent host or Cloudflare tunnel); rebuild app after change
- AI secrets stay in `backend/.env` only
