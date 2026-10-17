#!/usr/bin/env bash
set -u

BASE="${1:-}"
TOTAL="${2:-240}"

if [[ -z "$BASE" ]]; then
  echo "Uso: $0 https://escaparate.<ip>.nip.io [numero-peticiones]"
  exit 1
fi

BASE="${BASE%/}"

echo "Generando $TOTAL peticiones contra $BASE"
echo

ok=0
err=0

for i in $(seq 1 "$TOTAL"); do
  case $((i % 12)) in
    0)
      path="/no-existe-$((i % 7))"
      ;;
    1)
      path="/ruta%22rara"
      ;;
    2|3)
      path="/api/carga?ms=300"
      ;;
    4)
      path="/api/carga?ms=40"
      ;;
    5|6|7)
      path="/api/instancia"
      ;;
    8|9)
      path="/api/productos"
      ;;
    *)
      path="/"
      ;;
  esac

  code="$(
    curl -sS \
      --max-time 10 \
      -o /dev/null \
      -w '%{http_code}' \
      "${BASE}${path}" 2>/dev/null || printf '000'
  )"

  if [[ "$code" =~ ^2 ]]; then
    ok=$((ok + 1))
  else
    err=$((err + 1))
  fi

  if (( i % 40 == 0 )); then
    printf '  %d/%d peticiones\n' "$i" "$TOTAL"
  fi

  sleep 0.12
done

echo
echo "Terminado."
echo "Respuestas 2xx: $ok"
echo "Resto:          $err"
