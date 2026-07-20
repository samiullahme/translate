# Expose local FastAPI backend over HTTPS (no Android cleartext issues).
#
# Option A — Quick test URL (no domain setup, URL changes each run):
#   .\tunnel.ps1
#
# Option B — Your domain (requires Cloudflare DNS for the domain):
#   .\tunnel.ps1 -Hostname api.yourdomain.com
#
# Backend must already be running on port 8000.

param(
    [string]$Hostname = "",
    [int]$Port = 8000
)

$ErrorActionPreference = "Stop"
$cloudflared = Get-Command cloudflared -ErrorAction SilentlyContinue
if (-not $cloudflared) {
    $fallback = "C:\Program Files (x86)\cloudflared\cloudflared.exe"
    if (Test-Path $fallback) { $cloudflared = Get-Command $fallback }
}
if (-not $cloudflared) {
    Write-Error "cloudflared not found. Install: winget install Cloudflare.cloudflared"
}

$listening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if (-not $listening) {
    Write-Host "Backend not listening on port $Port. Start it first:"
    Write-Host "  cd `"$PSScriptRoot`""
    Write-Host "  python -m uvicorn main:app --host 127.0.0.1 --port $Port"
    exit 1
}

if ($Hostname) {
    Write-Host "Named tunnel mode: https://$Hostname -> http://127.0.0.1:$Port"
    Write-Host "Ensure you have run: cloudflared tunnel login && cloudflared tunnel create doctranslate"
    cloudflared tunnel run --url "http://127.0.0.1:$Port" --hostname $Hostname doctranslate
} else {
    Write-Host "Quick tunnel (temporary HTTPS URL printed below)."
    Write-Host "Copy the https://....trycloudflare.com URL into .env as BACKEND_URL, then rebuild the app."
    & $cloudflared.Source tunnel --url "http://127.0.0.1:$Port"
}
