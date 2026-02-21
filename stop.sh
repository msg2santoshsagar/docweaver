#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES=(postgres backend frontend)

cd "$ROOT_DIR"

is_running() {
  local svc="$1"
  docker compose ps --status running --services 2>/dev/null | grep -qx "$svc"
}

any_running=false
for svc in "${SERVICES[@]}"; do
  if is_running "$svc"; then
    any_running=true
    break
  fi
done

if [ "$any_running" = false ]; then
  echo "DocWeaver is already stopped."
  exit 0
fi

echo "Stopping DocWeaver services..."
docker compose down --remove-orphans

echo "DocWeaver stopped."
