#!/usr/bin/env bash
# Herramientas LOCALES para ver la base de datos y ensayar las migraciones de Flyway.
# Ejecuta ./db.sh help para ver los comandos.
set -euo pipefail

# Rutas de ficheros pasados como argumento: relativas a donde se ejecuta el comando.
abspath() { [ -n "${1:-}" ] && (cd "$(dirname "$1")" 2>/dev/null && echo "$(pwd)/$(basename "$1")") || echo "${1:-}"; }
ARG2="$(abspath "${2:-}")"

cd "$(dirname "$0")"
COMPOSE=(docker compose -f docker-compose.yml)
OUT=out
mkdir -p "$OUT" dumps

mysql_root() {
  "${COMPOSE[@]}" exec -T db mysql -uroot -proot --default-character-set=utf8mb4 "$@" 2>/dev/null
}

wait_db() {
  echo "Esperando a MySQL..."
  for _ in $(seq 1 60); do
    if "${COMPOSE[@]}" exec -T db mysqladmin ping -h 127.0.0.1 -uroot -proot --silent >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "MySQL no arrancó" >&2
  exit 1
}

cmd_up() {
  "${COMPOSE[@]}" up -d db adminer
  wait_db
  echo
  echo "MySQL:   127.0.0.1:3307  (root / root, base de datos shopdb)"
  echo "Adminer: http://127.0.0.1:8081  (servidor: db, usuario: root, contraseña: root)"
}

cmd_reset() {
  "${COMPOSE[@]}" --profile migrate down -v --remove-orphans
  cmd_up
}

# Carga un volcado SQL en shopdb (vacía la base de datos antes).
cmd_load() {
  local dump="${1:?Indica el fichero: ./db.sh load dumps/prod.sql}"
  [ -f "$dump" ] || { echo "No existe $dump" >&2; exit 1; }
  cmd_up >/dev/null
  echo "Cargando $dump en shopdb (se borra lo que hubiera)..."
  mysql_root -e "DROP DATABASE IF EXISTS shopdb; CREATE DATABASE shopdb CHARACTER SET utf8mb4;"
  # El volcado antiguo (database.sql) viene de MariaDB: se le quitan las cláusulas que MySQL no entiende.
  sed -e 's/utf8mb4_general_ci/utf8mb4_0900_ai_ci/g' "$dump" | mysql_root shopdb
  echo "Cargado."
}

# Imprime (y guarda en out/) tablas, columnas, índices, claves y migraciones aplicadas.
cmd_schema() {
  local file="${1:-$OUT/schema.txt}"
  mysql_root --force --table shopdb < schema.sql > "$file" || true
  cat "$file"
  echo
  echo "(guardado en db-tools/$file)"
}

# Esquema en formato "una línea por elemento" (tabulado), para comparar con diff.
schema_flat() {
  mysql_root --force --batch --skip-column-names shopdb < schema.sql 2>/dev/null \
    | grep -v "^===" | grep -vE "^[0-9]+\s+(1|2|3|4|5|6|7|8|9)" \
    | sed -E 's/\t[0-9]+\tInnoDB$/\tInnoDB/' > "$1" || true
}

# Arranca el backend contra esta BD: Flyway aplica las migraciones pendientes. Luego lo para.
cmd_migrate() {
  cmd_up >/dev/null
  echo "Construyendo y arrancando el backend (Flyway migra al arrancar)..."
  # --force-recreate: logs limpios (si no, se vería el "Started" de una ejecución anterior)
  "${COMPOSE[@]}" --profile migrate up -d --build --force-recreate app
  for _ in $(seq 1 120); do
    if "${COMPOSE[@]}" logs app 2>/dev/null | grep -q "Started ShopsystemApplication"; then
      break
    fi
    if "${COMPOSE[@]}" logs app 2>/dev/null | grep -qE "APPLICATION FAILED|Migration .* failed"; then
      "${COMPOSE[@]}" logs app | grep -E "Flyway|Migrat|V2:|ERROR|Caused by|Message" | tail -30
      "${COMPOSE[@]}" --profile migrate stop app
      echo "LA MIGRACIÓN HA FALLADO (ver arriba)" >&2
      exit 1
    fi
    sleep 2
  done
  "${COMPOSE[@]}" logs app | grep -E "Flyway|baseline|Migrating|Successfully|V2:|up to date" || true
  "${COMPOSE[@]}" --profile migrate stop app >/dev/null
  echo "Backend parado. Migraciones aplicadas."
}

# Ensayo completo: carga un volcado, guarda el esquema, migra, guarda el esquema y compara.
cmd_rehearse() {
  local dump="${1:?Indica el volcado: ./db.sh rehearse dumps/prod.sql}"
  cmd_load "$dump"
  cmd_schema "$OUT/antes.txt" >/dev/null
  schema_flat "$OUT/antes.flat"
  cmd_migrate
  cmd_schema "$OUT/despues.txt" >/dev/null
  schema_flat "$OUT/despues.flat"
  echo
  echo "=== CAMBIOS EN EL ESQUEMA (+ añadido, - quitado) ==="
  local changes
  changes="$( { diff "$OUT/antes.flat" "$OUT/despues.flat" || true; } | grep -E '^[<>]' | sed 's/^</-/; s/^>/+/' | grep -v flyway_schema_history || true)"
  if [ -n "$changes" ]; then echo "$changes" | column -t -s $'\t'; else echo "(sin cambios)"; fi
  echo
  echo "Esquema completo: db-tools/$OUT/antes.txt y db-tools/$OUT/despues.txt"
  cmd_checks
}

# Comprobaciones de integridad tras migrar.
cmd_checks() {
  echo
  echo "=== COMPROBACIONES ==="
  mysql_root --table shopdb <<'SQL'
SELECT 'Productos cuyo stock != suma de movimientos (debe ser 0)' AS comprobacion,
       COUNT(*) AS resultado
FROM items i
WHERE i.stock <> COALESCE((SELECT SUM(m.quantity) FROM stock_movements m WHERE m.item_id = i.id), 0)
UNION ALL
SELECT 'Ventas sin líneas (debe ser 0)', COUNT(*) FROM trades t
WHERE NOT EXISTS (SELECT 1 FROM trade_lines l WHERE l.trade_id = t.id)
UNION ALL
SELECT 'Ventas cuyo total != suma de líneas (debe ser 0)', COUNT(*) FROM trades t
WHERE t.total <> (SELECT SUM(l.line_total) FROM trade_lines l WHERE l.trade_id = t.id)
UNION ALL
SELECT 'Productos sin historial de precios (debe ser 0)', COUNT(*) FROM items i
WHERE NOT EXISTS (SELECT 1 FROM item_price_history h WHERE h.item_id = i.id)
UNION ALL
SELECT 'Productos sin precio de venta', COUNT(*) FROM items WHERE sale_price = 0
UNION ALL
SELECT 'Productos sin coste (beneficio desconocido)', COUNT(*) FROM items WHERE cost_price = 0
UNION ALL
SELECT 'Líneas de venta sin coste', COUNT(*) FROM trade_lines WHERE unit_cost IS NULL;
SQL
}

cmd_dump_help() {
  cat <<'EOF'
Para ensayar con una COPIA de producción (en el servidor):

  docker exec mysql-db mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction \
      --routines --no-tablespaces shopdb > prod.sql

Cópiala aquí a db-tools/dumps/prod.sql (esta carpeta no se sube a git) y ejecuta:

  ./db-tools/db.sh rehearse db-tools/dumps/prod.sql

OJO: el volcado tiene datos reales (ventas, contraseñas cifradas). No lo compartas.
EOF
}

case "${1:-help}" in
  up) cmd_up ;;
  down) "${COMPOSE[@]}" --profile migrate down ;;
  reset) cmd_reset ;;
  load) cmd_load "$ARG2" ;;
  schema) cmd_schema ;;
  migrate) cmd_migrate ;;
  rehearse) cmd_rehearse "$ARG2" ;;
  checks) cmd_checks ;;
  sql) shift; mysql_root --table shopdb -e "$*" ;;
  prod-dump) cmd_dump_help ;;
  *)
    cat <<'EOF'
Uso: ./db-tools/db.sh <comando>

  up                 Arranca MySQL (127.0.0.1:3307) y Adminer (http://127.0.0.1:8081)
  schema             Muestra migraciones aplicadas, tablas, columnas, índices y claves foráneas
  migrate            Arranca el backend contra esta BD para que Flyway aplique las migraciones
  load <volcado>     Carga un .sql en shopdb (la vacía antes)
  rehearse <volcado> Ensayo completo: carga, esquema antes, migra, esquema después, diferencias y comprobaciones
  checks             Comprobaciones de integridad (stock = movimientos, ventas con líneas...)
  sql "<consulta>"   Ejecuta una consulta en shopdb
  prod-dump          Cómo sacar una copia de producción para ensayar
  reset              Borra la BD local y la vuelve a crear vacía
  down               Para todo

Ejemplos:
  ./db-tools/db.sh up && ./db-tools/db.sh migrate && ./db-tools/db.sh schema   # BD nueva desde cero
  ./db-tools/db.sh rehearse db-tools/dumps/prod.sql                            # copia de producción
EOF
    ;;
esac
