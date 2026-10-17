#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../compose"
modo=${1:?Uso: sesiones.sh local|redis COMANDO...}
shift
case "$modo" in local|redis) ;; *) echo 'Modo esperado: local o redis' >&2; exit 2;; esac
exec sudo docker compose -f compose.yaml -f compose.override.yaml \
  -f "compose.sesiones-${modo}.yaml" "$@"
