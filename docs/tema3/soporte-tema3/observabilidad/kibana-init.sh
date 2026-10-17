#!/bin/sh
set -eu

KIBANA_URL="http://kibana:5601"

echo "Esperando a que Kibana esté preparado..."
intento=0
until curl --max-time 5 -fsS "$KIBANA_URL/api/status" >/dev/null 2>&1; do
  intento=$((intento + 1))
  if [ "$intento" -ge 120 ]; then
    echo "Kibana no está preparado. Revisa sus logs y vuelve a ejecutar kibana-init."
    exit 1
  fi
  sleep 3
done

echo "Creando/actualizando el data view Escaparate..."
curl -fsS \
  -X POST "$KIBANA_URL/api/data_views/data_view" \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{
    "data_view": {
      "id": "escaparate",
      "name": "Escaparate",
      "title": "escaparate-*",
      "timeFieldName": "@timestamp",
      "allowNoIndex": true
    },
    "override": true
  }' >/dev/null

curl -fsS \
  -X POST "$KIBANA_URL/api/data_views/default" \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{
    "data_view_id": "escaparate",
    "force": true
  }' >/dev/null

echo "Data view Escaparate preparado."
