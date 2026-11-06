package es.jonay.kb.shopsystem.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas a information_schema para que las migraciones Java sean adaptativas:
 * comprueban el estado real antes de crear o modificar nada. Así funcionan igual en una
 * base de datos nueva y en la de producción, que pudo crearse con versiones antiguas.
 */
final class SchemaInspector {

    private final Connection connection;

    SchemaInspector(Connection connection) {
        this.connection = connection;
    }

    boolean tableExists(String table) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                table) > 0;
    }

    boolean columnExists(String table, String column) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = ? AND column_name = ?", table, column) > 0;
    }

    boolean columnIsNullable(String table, String column) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = ? AND column_name = ? AND is_nullable = 'YES'", table, column) > 0;
    }

    boolean columnIsAutoIncrement(String table, String column) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = ? AND column_name = ? AND extra LIKE '%auto_increment%'", table, column) > 0;
    }

    boolean indexExists(String table, String index) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                + "AND table_name = ? AND index_name = ?", table, index) > 0;
    }

    /** Nombres de las claves foráneas de {@code table} que apuntan a {@code referencedTable}. */
    List<String> foreignKeys(String table, String referencedTable) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DISTINCT constraint_name FROM information_schema.key_column_usage "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND referenced_table_name = ?")) {
            ps.setString(1, table);
            ps.setString(2, referencedTable);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
        }
        return names;
    }

    long count(String sql, String... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    int execute(String sql) throws SQLException {
        try (Statement st = connection.createStatement()) {
            return st.executeUpdate(sql);
        }
    }
}
