#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v python3 >/dev/null
sudo docker info >/dev/null
trabajo=$(mktemp -d)
trap 'rm -rf "$trabajo"' EXIT
cp -R aplicacion-sesiones/. "$trabajo/"
echo 'Construyendo aplicación con sesiones en memoria...'
sudo docker build -t escaparate:sesion-10-local "$trabajo"
python3 - "$trabajo/pom.xml" <<'PYTHON'
from pathlib import Path
import sys
p=Path(sys.argv[1]);t=p.read_text()
needle='    <dependencies>'
assert t.count(needle)==1
extra='    <dependencies>\n        <dependency>\n            <groupId>org.springframework.boot</groupId>\n            <artifactId>spring-boot-starter-session-data-redis</artifactId>\n        </dependency>'
p.write_text(t.replace(needle,extra,1))
PYTHON
echo 'Construyendo aplicación con sesiones en Redis...'
sudo docker build -t escaparate:sesion-10-redis "$trabajo"
echo 'Listas: escaparate:sesion-10-local y escaparate:sesion-10-redis'
