package com.yymod.mcpforllm;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LogMonitor {
    private static final String LOG_FILE_NAME = "latest.log";
    private static final int MAX_ERRORS = 20;

    private long lastPosition;

    public synchronized List<String> readNewErrors() {
        Path logFile = FMLPaths.GAMEDIR.get().resolve("logs").resolve(LOG_FILE_NAME);
        if (!Files.exists(logFile)) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(logFile.toFile(), "r")) {
            long length = file.length();
            if (length < lastPosition) {
                lastPosition = 0;
            }
            file.seek(lastPosition);
            String line;
            while ((line = file.readLine()) != null) {
                if (isErrorLine(line)) {
                    errors.add(line);
                    if (errors.size() >= MAX_ERRORS) {
                        break;
                    }
                }
            }
            lastPosition = file.getFilePointer();
        } catch (IOException e) {
            McpForLlmMod.LOGGER.warn("Failed to read latest.log for errors.", e);
        }
        return errors;
    }

    private boolean isErrorLine(String line) {
        if (line.contains("Realms Notification Availability")
                || line.contains("RealmsClient")) {
            return false;
        }
        return line.contains("Exception")
                || line.contains("ERROR")
                || line.contains("Caused by");
    }
}
