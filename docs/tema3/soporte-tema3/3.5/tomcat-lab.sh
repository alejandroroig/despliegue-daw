#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/../compose"
exec sudo docker compose -f compose.yaml -f compose.override.yaml \
  -f compose.tomcat-lab.yaml "$@"
