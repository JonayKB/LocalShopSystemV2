# LocalShopSystemV2 · Kiosco Botánico

Punto de venta y gestión de la tienda: caja con lector de códigos de barras, tickets en impresora
ESC/POS, facturas simplificadas con **VERI\*FACTU**, stock con libro de movimientos, historial de
precios e informes de ventas, beneficio e inventario.

| Carpeta | Qué es |
|---|---|
| `shopsystem/` | API (Spring Boot 3, Java 17, MySQL 8, Flyway) |
| `localshopsystemv2/` | Web pública y panel de administración (React 19, Vite, TypeScript) |
| `db-tools/` | MySQL + Adminer locales para ver la base de datos y **ensayar migraciones** |
| `ops/` | Copias de seguridad y restauración en el servidor |
| `docker-compose.yml` | Producción (el que se usa en el servidor) |

## Desarrollo en local

Necesitas Java 17, Node 20 y Docker.

```bash
# 1. Base de datos (MySQL en 127.0.0.1:3307, Adminer en http://127.0.0.1:8081)
./db-tools/db.sh up

# 2. API: copia la configuración y ajústala
cp shopsystem/.env.example shopsystem/.env
#    DB_URL=jdbc:mysql://localhost:3307/shopdb   DB_USERNAME=root   DB_PASSWORD=root
cd shopsystem && ./mvnw spring-boot:run      # http://localhost:56784/kiosco (Flyway crea el esquema)

# 3. Web (otra terminal). /kiosco se reenvía a la API: sin CORS
cd localshopsystemv2 && npm install && PORT=5173 npm run dev   # http://localhost:5173
```

Para entrar en el panel hace falta un usuario en la tabla `users` con la contraseña cifrada con
BCrypt (`htpasswd -nbBC 10 x 'tu-clave' | cut -d: -f2`).

## Tests

```bash
cd shopsystem && ./mvnw verify          # unitarios + integración contra MySQL 8 real (Testcontainers, necesita Docker)
cd localshopsystemv2 && npm test        # Vitest
npm run typecheck && npm run build
```

La CI (GitHub Actions) ejecuta todo esto en cada push y pull request. **Si algo falla, no se publica
ninguna imagen ni se despliega.**

## Base de datos y migraciones

El esquema lo gestiona **Flyway** (`shopsystem/src/main/resources/db/migration` y el paquete
`es.jonay.kb.shopsystem.migration`). Hibernate no lo modifica (`ddl-auto=none`).

- **V1** es el esquema que creaba Hibernate antes de Flyway. En producción no se ejecuta: Flyway la
  marca como aplicada (*baseline*).
- **V2** añade el historial de precios, el libro de movimientos de stock, coste y categoría en las
  líneas de venta, los refresh tokens, etc. Es *adaptativa*: comprueba el estado real antes de
  tocar nada y se puede volver a ejecutar sin duplicar datos.
- Las migraciones se aplican **solas al arrancar** la API.

Reglas:

1. Nunca se modifica una migración ya publicada: los cambios van en una versión nueva (V3, V4…).
2. **Ensaya antes con una copia de producción**:

   ```bash
   ./db-tools/db.sh prod-dump                       # cómo sacar la copia del servidor
   ./db-tools/db.sh rehearse db-tools/dumps/prod.sql
   ```

   Enseña las tablas y columnas que se crean o cambian y unas comprobaciones de integridad
   (stock = suma de movimientos, todas las ventas con líneas, etc.). `db-tools/dumps/` no se sube a git.
3. `./db-tools/db.sh schema` muestra en cualquier momento migraciones, tablas, columnas, índices y
   claves foráneas.

## Despliegue

Cada push a `main`:

1. ejecuta los tests del backend y del frontend;
2. construye y publica `jonaykb/shop-app` y `jonaykb/shop-web` con la etiqueta del commit y `latest`;
3. en el servidor: **copia de la base de datos** en `~/backups/pre-deploy-*`, `docker compose pull`,
   `docker compose up -d` (sin parar antes) y espera a que la API esté sana. Si no arranca, el job
   falla y muestra el log.

Secretos y variables del repositorio (GitHub → Settings → Secrets and variables → Actions):

| Nombre | Tipo | Para qué |
|---|---|---|
| `DOCKER_HUB_USERNAME`, `DOCKER_HUB_ACCESS_TOKEN` | secret | publicar las imágenes |
| `SSH_HOST`, `SSH_USER` | variable | servidor |
| `SSH_KEY` | secret | **recomendado**: clave privada SSH para desplegar |
| `SSH_PASSWORD` | secret | alternativa mientras no haya `SSH_KEY` |

En el servidor, junto al `docker-compose.yml`, va un `.env` con todo lo de
`shopsystem/.env.example` más `MYSQL_ROOT_PASSWORD`. `DB_URL` debe ser `jdbc:mysql://db:3306/shopdb`.

Para volver a una versión anterior: `IMAGE_TAG=<commit> docker compose up -d` (y, si hiciera falta,
restaurar la copia previa con `ops/restore.sh`).

## Copias de seguridad

`ops/backup.sh` vuelca la base de datos (comprobando que el volcado está completo) y las fotos de
productos. Guarda 14 copias diarias y 24 mensuales, y si se define `RCLONE_REMOTE`, las sube fuera del
servidor. Instalación en el propio script (cron diario). Con VERI\*FACTU hay obligación de conservar
los registros de facturación: **la copia fuera del servidor no es opcional**.

`ops/restore.sh <copia.sql.gz>` restaura una copia (hace antes una copia de lo que haya).

## Seguridad

- Login con usuario y contraseña en el cuerpo de la petición. Token de acceso de 15 min y refresh
  token de 30 días, rotatorio y revocable; si se reutiliza uno ya usado, se cierran todas las sesiones.
- CORS solo para `https://kioscobotanico.com` (`CORS_ALLOWED_ORIGINS`).
- Ningún secreto en el repositorio: todo va en `.env`. Los certificados van fuera del código
  (`shopsystem/certs/`, ignorado por git y por Docker).

## Arquitectura de la API

```
web/        Endpoints REST (*Controller): validan la entrada y llaman a los servicios
service/    Lógica de negocio (*Service): ventas, inventario, precios, informes, impresora
verifactu/  Registros de facturación, huella encadenada, envío a la AEAT y QR
model/      Entidades JPA y repositorios
migration/  Migraciones Java de Flyway
api/        DTOs, seguridad (JWT), excepciones, tareas programadas
```

- El **stock** solo cambia a través de `InventoryService`: bloquea la fila del producto y deja un
  movimiento con el saldo resultante.
- Las **ventas** guardan una copia de cada línea (precio, IGIC, coste y categoría de ese momento):
  cambiar después un producto nunca altera ventas ni informes pasados.
- Los **informes** se calculan con esas líneas y en la zona horaria de la tienda (`SHOP_TIMEZONE`,
  por defecto `Atlantic/Canary`).
