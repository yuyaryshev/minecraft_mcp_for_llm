package com.yymod.mcpforllm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

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
        if (minecraft.screen != null && !(minecraft.screen instanceof TitleScreen)) {
            return;
        }
        String lastWorld = McpWorldOpener.findLastWorld();
        if (lastWorld == null || lastWorld.isBlank()) {
            attempted = true;
            return;
        }
        attempted = true;
        minecraft.execute(() -> McpWorldOpener.openWorld(minecraft, minecraft.screen, lastWorld));
    }
}
