package com.yymod.mcpforllm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

public final class McpWorldOpener {
    private McpWorldOpener() {
    }

    public static String findLastWorld() {
        Path savesDir = FMLPaths.GAMEDIR.get().resolve("saves");
        if (!Files.isDirectory(savesDir)) {
            return null;
        }
        try {
            Optional<Path> newest = Files.list(savesDir)
                    .filter(Files::isDirectory)
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
            return newest.map(path -> path.getFileName().toString()).orElse(null);
        } catch (IOException e) {
            McpForLlmMod.LOGGER.warn("Failed to scan saves directory for last world.", e);
            return null;
        }
    }

    public static boolean openWorld(Minecraft minecraft, Screen parent, String worldId) {
        try {
            Screen screen = parent != null ? parent : new TitleScreen();
            WorldOpenFlows flows = new WorldOpenFlows(minecraft, minecraft.getLevelSource());
            flows.loadLevel(screen, worldId);
            McpForLlmMod.LOGGER.info("Opening world: {}", worldId);
            return true;
        } catch (Exception e) {
            McpForLlmMod.LOGGER.warn("Failed to open world {}.", worldId, e);
            return false;
        }
    }

    public static String openLastWorld(Minecraft minecraft, Screen parent) {
        String lastWorld = findLastWorld();
        if (lastWorld == null || lastWorld.isBlank()) {
            return null;
        }
        boolean opened = openWorld(minecraft, parent, lastWorld);
        return opened ? lastWorld : null;
    }
}
