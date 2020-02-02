package me.egg82.antivpn.storage;

import com.google.common.primitives.Ints;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import ninja.egg82.core.SQLQueryResult;
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
                boolean legacyMySQL = false;
                if (sqlResourceName.equalsIgnoreCase("mysql")) {
                    legacyMySQL = isLegacyMySQL(storage);
                }

                if (!storage.sql.tableExists(storage.database, storage.prefix + "data")) {
                    InputStream stream = SQLVersionUtil.class.getClassLoader().getResourceAsStream(sqlResourceName + ".sql");
                    StringBuilder builder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8.name()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (legacyMySQL) {
                                builder.append(line.replace("{prefix}", storage.prefix).replace("datetime", "timestamp"));
                            } else {
                                builder.append(line.replace("{prefix}", storage.prefix));
                            }
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

        private static boolean isLegacyMySQL(AbstractSQL storage) throws StorageException {
            try {
                SQLQueryResult result = storage.sql.query("SHOW VARIABLES LIKE 'version';");
                if (result.getData().length != 1) {
                    throw new StorageException(false, "Could not get database version.");
                }
                String version = (String) result.getData()[0][1];
                String type = parseType(version);
                String numbers = getNumbers(version);
                if (
                        type.equalsIgnoreCase("mysql") && isAtLeast("5.6.5", numbers)
                        || type.equalsIgnoreCase("mariadb") && isAtLeast("10.0.1", numbers)
                ) {
                    return false;
                } else {
                    return true;
                }
            } catch (SQLException ex) {
                throw new StorageException(false, "Could not get database version.", ex);
            }
        }

        private static String parseType(String version) {
            int currentIndex = version.indexOf('-');
            if (currentIndex > -1) {
                return version.substring(currentIndex + 1);
            }
            return "MySQL";
        }

        private static String getNumbers(String version) {
            int currentIndex = version.indexOf('-');
            if (currentIndex > -1) {
                return version.substring(0, currentIndex);
            }
            return version;
        }

        private static boolean isAtLeast(String version, String currentVersion) {
            if (version == null) {
                throw new IllegalArgumentException("version cannot be null.");
            }

            int[] v1 = parseVersion(version);
            int[] v2 = parseVersion(currentVersion);

            boolean equalOrGreater = true;
            for (int i = 0; i < v1.length; i++) {
                if (i > v2.length) {
                    // We're looking for a version deeper than what we have
                    // eg. 1.12.2 -> 1.12
                    equalOrGreater = false;
                    break;
                }

                if (v2[i] < v1[i]) {
                    // The version we're at now is less than the one we want
                    // eg. 1.11 -> 1.13
                    equalOrGreater = false;
                    break;
                }
            }

            return equalOrGreater;
        }

        private static int[] parseVersion(String version) {
            List<Integer> ints = new ArrayList<>();

            int lastIndex = 0;
            int currentIndex = version.indexOf('.');

            while (currentIndex > -1) {
                int current = tryParseInt(version.substring(lastIndex, currentIndex));
                if (current > -1) {
                    ints.add(current);
                }

                lastIndex = currentIndex + 1;
                currentIndex = version.indexOf('.', currentIndex + 1);
            }
            int current = tryParseInt(version.substring(lastIndex));
            if (current > -1) {
                ints.add(current);
            }

            return Ints.toArray(ints);
        }

        private static int tryParseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (Exception ex) {
                return -1;
            }
        }
    }

    protected abstract void setKey(String key, String value) throws SQLException;

    protected abstract double getDouble(String key) throws SQLException;

    protected abstract boolean isAutomaticallyRecoverable(SQLException ex);
}
