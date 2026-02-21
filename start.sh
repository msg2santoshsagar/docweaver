#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES=(postgres backend frontend)

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

echo "Starting DocWeaver services..."
docker compose up --build -d "${SERVICES[@]}"

echo "DocWeaver started."
echo "Frontend: http://localhost:5173"
echo "Backend API: http://localhost:8080/api"
