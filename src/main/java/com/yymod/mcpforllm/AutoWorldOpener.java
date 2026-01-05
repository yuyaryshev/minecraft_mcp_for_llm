package com.yymod.mcpforllm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

public final class AutoWorldOpener {
    private static boolean attempted;

    private AutoWorldOpener() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(AutoWorldOpener::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (attempted || !McpConfig.AUTO_OPEN_LAST_WORLD.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            attempted = true;
            return;
        }
        if (!(minecraft.screen instanceof TitleScreen)) {
            return;
        }
        String lastWorld = findLastWorld();
        if (lastWorld == null || lastWorld.isBlank()) {
            attempted = true;
            return;
        }
        attempted = true;
        minecraft.execute(() -> openWorldReflective(minecraft, minecraft.screen, lastWorld));
    }

    private static String findLastWorld() {
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

    private static void openWorldReflective(Minecraft minecraft, Screen parent, String worldId) {
        try {
            Method getLevelSource = Minecraft.class.getDeclaredMethod("m_91392_");
            Object levelSource = getLevelSource.invoke(minecraft);
            Constructor<WorldOpenFlows> constructor = WorldOpenFlows.class.getConstructor(Minecraft.class, levelSource.getClass());
            WorldOpenFlows flows = constructor.newInstance(minecraft, levelSource);
            Method openWorld = WorldOpenFlows.class.getDeclaredMethod("m_233133_", Screen.class, String.class);
            openWorld.invoke(flows, parent, worldId);
            McpForLlmMod.LOGGER.info("Opening last world: {}", worldId);
        } catch (Exception e) {
            McpForLlmMod.LOGGER.warn("Failed to open last world {}.", worldId, e);
        }
    }
}
