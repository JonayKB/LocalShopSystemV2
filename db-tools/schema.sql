-- Estado del esquema: migraciones aplicadas, tablas (con filas) y columnas.
SELECT '=== MIGRACIONES (flyway_schema_history) ===' AS '';
SELECT installed_rank AS n, version, description, type, success, installed_on
FROM flyway_schema_history ORDER BY installed_rank;

SELECT '=== TABLAS ===' AS '';
SELECT table_name AS tabla, table_rows AS filas_aprox, engine
FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_name;

SELECT '=== COLUMNAS ===' AS '';
SELECT table_name AS tabla, column_name AS columna, column_type AS tipo,
       IF(is_nullable = 'YES', 'NULL', 'NOT NULL') AS nulo,
       COALESCE(column_default, '') AS por_defecto, column_key AS clave, extra
FROM information_schema.columns WHERE table_schema = DATABASE()
ORDER BY table_name, ordinal_position;

SELECT '=== ÍNDICES ===' AS '';
SELECT table_name AS tabla, index_name AS indice, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columnas,
       IF(non_unique = 0, 'UNIQUE', '') AS unico
FROM information_schema.statistics WHERE table_schema = DATABASE()
GROUP BY table_name, index_name, non_unique ORDER BY table_name, index_name;

SELECT '=== CLAVES FORÁNEAS ===' AS '';
SELECT table_name AS tabla, column_name AS columna, referenced_table_name AS referencia, constraint_name AS nombre
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL ORDER BY table_name, column_name;
