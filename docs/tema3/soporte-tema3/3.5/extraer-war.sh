#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/../compose"
mkdir -p ../aplicaciones/tomcat
imagen=$(sudo docker compose config --format json | python3 -c \
 'import json,sys; print(json.load(sys.stdin)["services"]["app-1"]["image"])')
echo "Extrayendo el WAR de $imagen"
cid_lab=$(sudo docker create "$imagen")
trap 'sudo docker rm "$cid_lab" >/dev/null' EXIT
sudo docker cp "$cid_lab:/app/app.war" ../aplicaciones/tomcat/escaparate.war
sudo chmod 644 ../aplicaciones/tomcat/escaparate.war
ls -lh ../aplicaciones/tomcat/escaparate.war
