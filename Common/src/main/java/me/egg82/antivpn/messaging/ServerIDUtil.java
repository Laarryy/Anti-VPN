package me.egg82.antivpn.messaging;

import java.io.*;
import java.nio.file.Files;
import java.util.UUID;
import me.egg82.antivpn.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerIDUtil {
    private static final Logger logger = LoggerFactory.getLogger(ServerIDUtil.class);

    private ServerIDUtil() { }

    public static UUID getId(File idFile) {
        if (idFile == null) {
            throw new IllegalArgumentException("idFile cannot be null.");
        }

        UUID retVal;

        try {
            retVal = readId(idFile);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            retVal = null;
        }

        if (retVal == null) {
            retVal = UUID.randomUUID();
            try {
                writeId(idFile, retVal);
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        return retVal;
    }

    private static UUID readId(File idFile) throws IOException {
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
        String retVal = builder.toString().trim();

        return ValidationUtil.isValidUuid(retVal) ? UUID.fromString(retVal) : null;
    }

    private static void writeId(File idFile, UUID id) throws IOException {
        if (idFile.exists() && idFile.isDirectory()) {
            Files.delete(idFile.toPath());
        }
        if (!idFile.exists()) {
            if (!idFile.createNewFile()) {
                throw new IOException("Stats file could not be created.");
            }
        }

        try (FileWriter out = new FileWriter(idFile)) {
            out.write(id.toString() + System.lineSeparator());
        }
    }
}
