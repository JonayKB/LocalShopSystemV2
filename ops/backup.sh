#!/usr/bin/env bash
# Copia de seguridad diaria (servidor de producción): base de datos + fotos de productos.
#
# Instalación (en el servidor, como el usuario que despliega):
#   chmod +x ~/ops/backup.sh
#   crontab -e   ->   15 3 * * * $HOME/ops/backup.sh >> $HOME/backups/backup.log 2>&1
#
# Copia FUERA del servidor (muy recomendable; con VERI*FACTU hay obligación de conservar los
# registros): configura rclone (rclone config) y define RCLONE_REMOTE, p. ej. "gdrive:kiosco".
set -euo pipefail

COMPOSE_DIR="${COMPOSE_DIR:-$HOME}"          # donde está docker-compose.yml
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"
KEEP_DAILY="${KEEP_DAILY:-14}"               # días de copias diarias que se conservan
KEEP_MONTHLY="${KEEP_MONTHLY:-24}"           # meses de copias mensuales (día 1) que se conservan
DB_CONTAINER="${DB_CONTAINER:-mysql-db}"
RCLONE_REMOTE="${RCLONE_REMOTE:-}"

stamp="$(date +%F-%H%M%S)"
mkdir -p "$BACKUP_DIR/daily" "$BACKUP_DIR/monthly"

# 1) Base de datos: volcado consistente (InnoDB) sin bloquear la tienda
db_file="$BACKUP_DIR/daily/shopdb-$stamp.sql.gz"
docker exec "$DB_CONTAINER" sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --no-tablespaces shopdb' \
  | gzip -9 > "$db_file.tmp"
# Un volcado vacío o cortado no debe sustituir a una copia buena
if ! gzip -dc "$db_file.tmp" | tail -c 200 | grep -q 'Dump completed'; then
  echo "$(date) ERROR: el volcado está incompleto" >&2
  rm -f "$db_file.tmp"
  exit 1
fi
mv "$db_file.tmp" "$db_file"

# 2) Fotos de productos
uploads_file="$BACKUP_DIR/daily/uploads-$stamp.tar.gz"
tar -C "$COMPOSE_DIR" --exclude='uploads/.thumbs' -czf "$uploads_file" uploads 2>/dev/null || true

# 3) El día 1 de cada mes se guarda también como copia mensual
if [ "$(date +%d)" = "01" ]; then
  cp "$db_file" "$BACKUP_DIR/monthly/"
fi

# 4) Rotación
find "$BACKUP_DIR/daily" -type f -mtime +"$KEEP_DAILY" -delete
find "$BACKUP_DIR/monthly" -name 'shopdb-*.sql.gz' -printf '%T@ %p\n' | sort -rn \
  | tail -n +"$((KEEP_MONTHLY + 1))" | cut -d' ' -f2- | xargs -r rm -f

# 5) Copia fuera del servidor
if [ -n "$RCLONE_REMOTE" ]; then
  rclone copy "$BACKUP_DIR" "$RCLONE_REMOTE" --exclude "*.log"
fi

echo "$(date) OK: $(du -h "$db_file" | cut -f1) BD, $(du -h "$uploads_file" 2>/dev/null | cut -f1) fotos"
