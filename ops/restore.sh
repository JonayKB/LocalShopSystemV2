#!/usr/bin/env bash
# Restaura una copia de la base de datos en producción. ¡Sustituye los datos actuales!
# Uso: ./ops/restore.sh ~/backups/daily/shopdb-2026-01-01-031500.sql.gz
set -euo pipefail
file="${1:?Indica la copia .sql.gz a restaurar}"
DB_CONTAINER="${DB_CONTAINER:-mysql-db}"
[ -f "$file" ] || { echo "No existe $file" >&2; exit 1; }

read -r -p "Esto REEMPLAZA la base de datos shopdb por $file. Escribe 'restaurar' para seguir: " ok
[ "$ok" = "restaurar" ] || { echo "Cancelado."; exit 1; }

# Copia de lo que hay ahora, por si acaso
"$(dirname "$0")/backup.sh"
docker stop shop-app >/dev/null || true
gzip -dc "$file" | docker exec -i "$DB_CONTAINER" sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" shopdb'
docker start shop-app >/dev/null
echo "Restaurado. La app vuelve a arrancar (Flyway aplicará migraciones si la copia es anterior)."
