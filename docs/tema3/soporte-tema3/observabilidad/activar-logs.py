from pathlib import Path
import re
p = Path(__file__).resolve().parent.parent / "nginx/conf.d/sitios.conf"
texto = p.read_text()
if "log_format observabilidad" in texto:
    print("El registro estructurado ya está configurado.")
else:
    formato = """log_format observabilidad escape=json
    '{"hora":"$time_iso8601","ip":"$remote_addr",'
    '"metodo":"$request_method","ruta":"$uri",'
    '"estado":$status,"duracion_s":$request_time,'
    '"destino":"$upstream_addr"}';

"""
    texto, cambios = re.subn(r"(?m)^server\s*\{[ \t]*$",
        "server {\n    access_log /dev/stdout observabilidad;", texto)
    if not cambios:
        raise SystemExit("No encuentro bloques server. No modifico nada.")
    p.with_suffix(".conf.antes-json").write_text(p.read_text())
    p.write_text(formato + texto)
    print("Registro JSON configurado.")
