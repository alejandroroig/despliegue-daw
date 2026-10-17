#!/usr/bin/env bash
set -euo pipefail
url=${1:?Uso: probar-sesion.sh URL_HTTPS}
url=${url%/}
case "$url" in https://*) ;; *) echo 'Introduce la URL completa https://...' >&2; exit 2;; esac
cookies=$(mktemp)
trap 'rm -f "$cookies"' EXIT
echo 'Creando una sesión NUEVA y guardando su cookie...'
curl --max-time 60 -fsS -c "$cookies" -X POST "$url/api/sesion/abrir?usuario=demo"
printf '\n\nConsultando doce veces con la MISMA cookie...\n'
for i in $(seq 1 12); do
    printf '\nPetición %s: ' "$i"
    curl --max-time 60 -sS -b "$cookies" \
      -w '\nHTTP %{http_code}\n' "$url/api/sesion/estado"
done
