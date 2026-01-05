package com.yymod.mcpforllm;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(McpForLlmMod.MOD_ID)
public class McpForLlmMod {
    public static final String MOD_ID = "mcp_for_llm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public McpForLlmMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        McpConfig.register(ModLoadingContext.get());
        modBus.addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            McpServer.startIfEnabled();
            AutoWorldOpener.init();
        });
    }
}
