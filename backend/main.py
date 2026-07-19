import os
from fastapi import FastAPI, Depends, HTTPException, status, UploadFile, File
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import httpx
from pydantic import BaseModel
from typing import List, Optional
import dotenv

dotenv.load_dotenv()

app = FastAPI(title="DocTranslate AI Proxy")
security = HTTPBearer()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_ANON_KEY = os.getenv("SUPABASE_ANON_KEY")

async def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{SUPABASE_URL}/auth/v1/user",
            headers={"Authorization": f"Bearer {token}", "apikey": SUPABASE_ANON_KEY}
        )
        if response.status_code != 200:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired token")
        return response.json()

@app.get("/api/health")
async def health():
    return {"status": "ok", "ocr_provider": os.getenv("OCR_PROVIDER", "google")}

class Block(BaseModel):
    text: str

class OCRResponse(BaseModel):
    text: str
    blocks: List[Block]

@app.post("/api/ocr", response_model=OCRResponse)
async def ocr(file: UploadFile = File(...), user: dict = Depends(verify_token)):
    provider = os.getenv("OCR_PROVIDER", "google")
    demo_ai = os.getenv("DEMO_AI", "false").lower() == "true"
    
    if provider == "google":
        cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        if not cred_path and not demo_ai:
            raise HTTPException(status_code=500, detail="Google Vision credentials missing and DEMO_AI is false")
            
    # Mocking successful response for demo
    return OCRResponse(
        text="This is the extracted text from the document.\n\nIt preserves paragraphs and layout.",
        blocks=[Block(text="This is the extracted text from the document."), Block(text="It preserves paragraphs and layout.")]
    )
