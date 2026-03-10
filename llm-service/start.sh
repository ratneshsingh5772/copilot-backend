#!/usr/bin/env bash
# ============================================================
# start.sh — install deps and launch the FastAPI LLM service
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Python venv ───────────────────────────────────────────────
if [ ! -d ".venv" ]; then
  echo "Creating Python virtual environment..."
  python3 -m venv .venv
fi

source .venv/bin/activate

echo "Installing dependencies..."
pip install -q --upgrade pip
pip install -q -r requirements.txt

# ── Ollama check ──────────────────────────────────────────────
echo ""
if curl -s --connect-timeout 3 http://localhost:11434/api/tags > /dev/null 2>&1; then
  echo "✅  Ollama is running at http://localhost:11434"
else
  echo "⚠️   Ollama is NOT running. Start it with: ollama serve"
  echo "     Then pull the model: ollama pull llama3.2"
fi

# ── Start FastAPI ─────────────────────────────────────────────
echo ""
echo "🚀  Starting LLM service on http://localhost:8001"
echo "    Docs: http://localhost:8001/docs"
echo ""
uvicorn main:app --host 0.0.0.0 --port 8001 --reload

