package com.yymod.mcpforllm;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class McpConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue AUTO_START_SERVER = BUILDER
            .comment("Start the MCP HTTP server when the client loads.")
            .define("mcp.autoStart", true);

    public static final ForgeConfigSpec.IntValue SERVER_PORT = BUILDER
            .comment("Port for the MCP HTTP server (localhost only).")
            .defineInRange("mcp.port", 25576, 1024, 65535);

    public static final ForgeConfigSpec.BooleanValue AUTO_OPEN_LAST_WORLD = BUILDER
            .comment("Automatically open the last played singleplayer world on startup.")
            .define("client.autoOpenLastWorld", true);

    public static final ForgeConfigSpec.BooleanValue ALLOW_CLOSE_CLIENT = BUILDER
            .comment("Allow MCP endpoint to close the Minecraft client.")
            .define("mcp.allowCloseClient", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private McpConfig() {
    }

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
