#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MODEL="${DOCWEAVER_OLLAMA_MODEL:-qwen2.5vl:7b}"

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
      echo "Warning: failed to pull model '$MODEL'. App will still start with BASIC fallback."
    fi
  fi
else
  echo "Ollama not found on this machine. Install Ollama for local AI suggestions."
fi

exec "$ROOT_DIR/start.sh"
