#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES=(postgres backend frontend)
MODEL="${DOCWEAVER_OLLAMA_MODEL:-qwen2.5vl:7b}"

mkdir -p "$ROOT_DIR/data/uploads" "$ROOT_DIR/data/output" "$ROOT_DIR/data/postgres"
chmod 755 "$ROOT_DIR/data/uploads" "$ROOT_DIR/data/output" "$ROOT_DIR/data/postgres"

cd "$ROOT_DIR"

is_running() {
  local svc="$1"
  docker compose ps --status running --services 2>/dev/null | grep -qx "$svc"
}

all_running=true
for svc in "${SERVICES[@]}"; do
  if ! is_running "$svc"; then
    all_running=false
    break
  fi
done

if [ "$all_running" = true ]; then
  echo "DocWeaver is already running. No restart performed."
  echo "Frontend: http://localhost:5173"
  echo "Backend API: http://localhost:8080/api"
  exit 0
fi

if command -v ollama >/dev/null 2>&1; then
  echo "Checking local Ollama model: $MODEL"
  if ! ollama list >/dev/null 2>&1; then
    if pgrep -f "ollama serve" >/dev/null 2>&1; then
      echo "Ollama service appears to be starting. Waiting..."
      sleep 2
    else
      echo "Starting Ollama service..."
      nohup ollama serve >/tmp/docweaver-ollama.log 2>&1 &
      sleep 3
    fi
  fi

  if ollama list 2>/dev/null | awk 'NR>1 {print $1}' | grep -qx "$MODEL"; then
    echo "Ollama model already available: $MODEL"
  else
    echo "Pulling Ollama model: $MODEL"
    if ! ollama pull "$MODEL"; then
      echo "Warning: failed to pull model '$MODEL'. App will start but AI features may be unavailable."
    fi
  fi
else
  echo "Warning: Ollama not found on this machine. AI features will be unavailable."
fi

echo "Starting DocWeaver services..."
docker compose up --build -d "${SERVICES[@]}"

echo "DocWeaver started."
echo "Frontend: http://localhost:5173"
echo "Backend API: http://localhost:8080/api"
