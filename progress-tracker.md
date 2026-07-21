# Progress Tracker

Update this file after every meaningful implementation change.
**Be honest.** Never mark a step complete unless it ships with real APIs.

## Current Phase

- Phase 1 — Core Document Loop (**in progress**)
- Phase 2 — Voice & Chat (**backend done on Emergent; Android pending**)

## Current Goal

- **Android Step 05** — Wire chat sheet + mic STT against Emergent `BACKEND_URL`; persist messages in Supabase `document_chats`.  
  Backend for Step 05 is **done on Emergent** (see below). Local Kotlin repo still needs UI + Retrofit wiring.

## Completed

### Android app (`samiullahme/translate` — local repo)

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

### Emergent backend (hosted — verified Jul 21, 2026)

`BACKEND_URL=https://bbb0a2fd-623f-44f6-8a72-43b80ae89de9.preview.emergentagent.com`

- **Step 03 — Translate + Summarize** (Completed on Emergent)
  - `POST /api/translate`, `POST /api/summarize`, `llm_configured` on health.
- **Step 04 — TTS + Karaoke** (Completed on Emergent)
  - `POST /api/tts`, `GET /api/tts/voices`, Cartesia SSE, `tts_configured` on health.
  - Note: health reports `tts_model: sonic-2` (verify against locked pins if Android karaoke drifts).
- **Step 05 — Chat + STT** (Backend complete; STT upstream blocked)
  - `POST /api/chat` — grounded replies, out-of-scope guard verified (35 tests passed).
  - `POST /api/stt` — wiring correct; **fails closed** because Vercel AI Gateway does not proxy `/v1/audio/transcriptions`. Needs real `WHISPER_API_KEY` or `STT_BASE_URL` override (deferred).
  - `GET /api/health` includes `stt_configured`, `stt_model`.

## NOT done

### Android (local repo — still behind Emergent backend)

- Step 03 — translate / summarize / Document Viewer / full AppUIDesign tabs & lists
- Step 04 — Cartesia TTS karaoke + Profile voice picker UI
- Step 05 — Chat sheet + mic → STT → chat UI; Supabase `document_chats` persistence
- Step 06 — PDF OCR wiring + Storage polish + hardening

### Backend / platform backlog (Emergent)

- P1: Real STT provider (`WHISPER_API_KEY` or `STT_BASE_URL`) — **deferred; owner decision pending**
- P1: Remap provider failures from HTTP 502 → structured JSON the phone can read (ingress swallows 502 bodies)
- P2: Voices pagination on `/api/tts/voices`
- P2: Split `server.py` into routers
- P3: UUID validator on `TTSRequest.voice_id`
- Optional: Server-side chat persistence (`chat_sessions` / `chat_messages`) — **defer to Step 06 batch**

## Next Up

1. **Android Step 03–05** — Catch local Compose app up to Emergent backend (03 → 04 → 05 in order, or 05 only if 03–04 already wired elsewhere)
2. **STT decision** — Accept text-only chat for now OR add real Whisper key + `STT_BASE_URL`
3. **Step 06** — PDF OCR + Storage polish (backend + Android) after Android Step 05 smoke test

## Honest snapshot (Jul 21, 2026)

| Area | Reality |
| --- | --- |
| Stack | Native Android Kotlin Compose + FastAPI + Supabase (**not Expo**) |
| Emergent FastAPI | health, ocr, translate, summarize, tts, voices, chat, stt — **all routes live** |
| Local FastAPI (`backend/main.py`) | `/api/health`, `/api/ocr` only (stale vs Emergent) |
| Local Android | Auth + OCR only; no Viewer / TTS / Chat UI yet |
| UI design SoT | [`samiullahme/AppUIDesign`](https://github.com/samiullahme/AppUIDesign) |
| STT on device | Blocked until real Whisper backend or gateway compat |
| Chat | Works on Emergent backend; Android UI not wired |

## Locked decisions

- OCR = API Ninjas only (no Google Vision, no demo OCR)
- LLM = OpenAI-compatible (`OPENAI_BASE_URL` / Vercel AI Gateway OK)
- Cartesia pins: `CARTESIA_VERSION=2024-06-10`, `CARTESIA_MODEL=sonic-english` (Emergent health may show `sonic-2` — confirm before Android TTS)
- `BACKEND_URL` = Emergent preview URL above; rebuild app after change
- AI secrets stay in backend env only
- STT: defer real key for now; text chat is acceptable for Step 05 sign-off
- Chat persistence: Android + Supabase `document_chats` per original architecture (defer server-side `chat_sessions` to Step 06)
- Provider 502 → remap to HTTP 200 `{status:"failed", error_message}` so Android gets structured errors through ingress

## Session notes

- Jul 21, 2026: Emergent Step 05 backend verified (35 passed / 0 failed / 2 skipped). Progress tracker updated to reflect split: Emergent backend ahead of local Android repo.
