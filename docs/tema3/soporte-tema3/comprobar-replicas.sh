#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/../compose"
fallos=0
for servicio in app-1 app-2 app-3; do
    preparado=no
    for intento in $(seq 1 60); do
        if sudo docker compose exec -T web wget -q -T 3 -O /dev/null \
            "http://${servicio}:8080/api/salud/listo" 2>/dev/null; then
            preparado=si
            break
        fi
        sleep 2
    done
    echo "$servicio preparado: $preparado"
    if [ "$preparado" != si ]; then fallos=1; fi
done
exit "$fallos"
