import os
from fastapi import FastAPI, Depends, HTTPException, status, UploadFile, File
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import httpx
from pydantic import BaseModel
from typing import Any, List
import dotenv

dotenv.load_dotenv()

app = FastAPI(title="DocTranslate AI Proxy")
security = HTTPBearer()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_ANON_KEY = os.getenv("SUPABASE_ANON_KEY")
API_NINJAS_KEY = os.getenv("API_NINJAS_KEY", "").strip()
NINJAS_OCR_URL = "https://api.api-ninjas.com/v1/imagetotext"


def _ocr_error(message: str, code: int = status.HTTP_502_BAD_GATEWAY) -> HTTPException:
    return HTTPException(
        status_code=code,
        detail={"status": "failed", "error_message": message},
    )


async def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{SUPABASE_URL}/auth/v1/user",
            headers={"Authorization": f"Bearer {token}", "apikey": SUPABASE_ANON_KEY},
        )
        if response.status_code != 200:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired token",
            )
        return response.json()


@app.get("/api/health")
async def health():
    return {
        "status": "ok",
        "ocr_configured": bool(API_NINJAS_KEY),
    }


class Block(BaseModel):
    text: str


class OCRResponse(BaseModel):
    text: str
    blocks: List[Block]


def _normalize_ninjas_payload(data: Any) -> OCRResponse:
    if not isinstance(data, list):
        raise _ocr_error("Unexpected OCR response from API Ninjas")

    blocks: List[Block] = []
    for item in data:
        if not isinstance(item, dict):
            continue
        piece = (item.get("text") or "").strip()
        if piece:
            blocks.append(Block(text=piece))

    full_text = " ".join(b.text for b in blocks).strip()
    return OCRResponse(text=full_text, blocks=blocks)


@app.post("/api/ocr", response_model=OCRResponse)
async def ocr(file: UploadFile = File(...), user: dict = Depends(verify_token)):
    if not API_NINJAS_KEY:
        raise _ocr_error(
            "API_NINJAS_KEY is not configured on the server",
            code=status.HTTP_503_SERVICE_UNAVAILABLE,
        )

    image_bytes = await file.read()
    if not image_bytes:
        raise _ocr_error("Empty image upload", code=status.HTTP_400_BAD_REQUEST)

    filename = file.filename or "image.jpg"
    content_type = file.content_type or "image/jpeg"

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                NINJAS_OCR_URL,
                headers={"X-Api-Key": API_NINJAS_KEY},
                files={"image": (filename, image_bytes, content_type)},
            )
    except httpx.HTTPError as exc:
        raise _ocr_error(f"Failed to reach API Ninjas: {exc}") from exc

    if response.status_code != 200:
        try:
            err_body = response.json()
            message = (
                err_body.get("error")
                or err_body.get("message")
                or err_body.get("error_message")
                or response.text
            )
        except Exception:
            message = response.text or f"HTTP {response.status_code}"
        raise _ocr_error(f"OCR failed: {message}")

    try:
        payload = response.json()
    except Exception as exc:
        raise _ocr_error("Invalid JSON from API Ninjas") from exc

    if isinstance(payload, dict) and (payload.get("error") or payload.get("message")):
        raise _ocr_error(
            f"OCR failed: {payload.get('error') or payload.get('message')}"
        )

    return _normalize_ninjas_payload(payload)
