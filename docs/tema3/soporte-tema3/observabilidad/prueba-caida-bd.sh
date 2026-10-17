#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/../compose"
: "${URL:?Exporta URL con la dirección HTTPS del catálogo}"
sudo -v
recuperar() {
    echo "Recuperando PostgreSQL..."
    sudo docker compose start bd
}
trap recuperar EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
sudo docker compose stop bd
for i in 1 2 3; do
    curl --max-time 60 -sS -o /dev/null \
      -w 'HTTP %{http_code} — %{time_total}s\n' \
      "${URL%/}/api/salud/listo" || true
done
