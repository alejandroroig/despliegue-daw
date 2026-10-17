#!/usr/bin/env bash
set -euo pipefail

echo "Preparando el host para la pila de observabilidad..."

mem_kb="$(awk '/MemTotal/ {print $2}' /proc/meminfo)"
if (( mem_kb < 3500000 )); then
  echo "AVISO: la instancia tiene menos de 4 GiB de RAM."
  echo "La práctica está pensada para una instancia con al menos 4 GiB."
fi

printf 'vm.max_map_count=1048576\n' | sudo tee /etc/sysctl.d/99-daw-observabilidad.conf >/dev/null
sudo sysctl -p /etc/sysctl.d/99-daw-observabilidad.conf >/dev/null

if ! swapon --show=NAME --noheadings 2>/dev/null | grep -qx '/swapfile'; then
  if [[ ! -f /swapfile ]]; then
    echo "Creando 2 GiB de swap temporal..."
    if ! sudo fallocate -l 2G /swapfile; then
      sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 status=progress
    fi
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile >/dev/null
  fi
  sudo swapon /swapfile
fi

echo
echo "vm.max_map_count:"
sysctl vm.max_map_count
echo
echo "Memoria:"
free -h
echo
echo "Host preparado."
