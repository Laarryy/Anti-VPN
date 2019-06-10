package me.egg82.antivpn.utils;

import java.io.*;
import java.nio.file.Files;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerIDUtil {
    private static final Logger logger = LoggerFactory.getLogger(ServerIDUtil.class);

    private ServerIDUtil() {}

    public static UUID getID(File idFile) {
        String id;

        try {
            id = readID(idFile);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            id = null;
        }

        if (id == null || id.isEmpty() || !ValidationUtil.isValidUuid(id)) {
            id = UUID.randomUUID().toString();
            try {
                writeID(idFile, id);
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        return UUID.fromString(id);
    }

    private static String readID(File idFile) throws IOException {
        if (!idFile.exists() || (idFile.exists() && idFile.isDirectory())) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        try (FileReader reader = new FileReader(idFile); BufferedReader in = new BufferedReader(reader)) {
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
        }
        return builder.toString().trim();
    }

    private static void writeID(File idFile, String id) throws IOException {
        if (idFile.exists() && idFile.isDirectory()) {
            Files.delete(idFile.toPath());
        }
        if (!idFile.exists()) {
            if (!idFile.createNewFile()) {
                throw new IOException("Stats file could not be created.");
            }
        }

        try (FileWriter out = new FileWriter(idFile)) {
            out.write(id + System.lineSeparator());
        }
    }
}
