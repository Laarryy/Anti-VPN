package me.egg82.antivpn.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import ninja.egg82.sql.FileImporter;
import ninja.egg82.sql.SQL;

public abstract class AbstractSQL implements Storage {
    protected SQL sql;
    protected String database = "";
    protected String prefix = "";

    protected static class SQLVersionUtil {
        public static void conformVersion(AbstractSQL storage, String sqlResourceName) throws IOException, StorageException {
            FileImporter importer = new FileImporter(storage.sql);

            try {
                if (!storage.sql.tableExists(storage.database, storage.prefix + "data")) {
                    InputStream stream = SQLVersionUtil.class.getClassLoader().getResourceAsStream(sqlResourceName + ".sql");
                    StringBuilder builder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8.name()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line.replace("{prefix}", storage.prefix));
                            builder.append('\n');
                        }
                    } catch (UnsupportedEncodingException ignored) { }
                    importer.readString(builder.toString(), true);
                }

                double oldVersion = storage.getDouble("db_version");
                if (oldVersion < 1.0d) {
                    // Insert DB version
                    storage.setKey("db_version", "1.0");
                }
                /*if (oldVersion < 1.1d) {
                    toVersion(storage, sqlResourceName, "1.1", importer);
                }*/
            } catch (SQLException ex) {
                throw new StorageException(false, "Could not get/update SQL version.", ex);
            }
        }

        private static void toVersion(AbstractSQL storage, String sqlResourceName, String version, FileImporter importer) throws IOException, SQLException {
            // Update DB
            InputStream stream = SQLVersionUtil.class.getClassLoader().getResourceAsStream(sqlResourceName + "_" + version + ".sql");
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8.name()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line.replace("{prefix}", storage.prefix));
                    builder.append('\n');
                }
            } catch (UnsupportedEncodingException ignored) { }
            importer.readString(builder.toString(), true);

            // Update DB version
            storage.setKey("db_version", version);
        }
    }

    protected abstract void setKey(String key, String value) throws SQLException;

    protected abstract double getDouble(String key) throws SQLException;

    protected abstract boolean isAutomaticallyRecoverable(SQLException ex);
}
