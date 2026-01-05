package com.yymod.mcpforllm;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class McpCommandExecutor {
    private McpCommandExecutor() {
    }

    public static CommandResult executeBlocking(String command, long timeout, TimeUnit unit) {
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return CommandResult.error("No integrated server running.");
        }
        CompletableFuture<CommandResult> future = new CompletableFuture<>();
        server.execute(() -> future.complete(executeOnServer(server, command)));
        try {
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.error("Command interrupted.");
        } catch (ExecutionException e) {
            return CommandResult.error("Command failed: " + e.getCause().getMessage());
        } catch (TimeoutException e) {
            return CommandResult.error("Command timed out.");
        }
    }

    private static CommandResult executeOnServer(MinecraftServer server, String command) {
        Optional<ServerPlayer> player = server.getPlayerList().getPlayers().stream().findFirst();
        if (player.isEmpty()) {
            return CommandResult.error("No player is connected.");
        }
        ServerPlayer serverPlayer = player.get();
        ServerLevel level = serverPlayer.serverLevel();
        CapturingCommandOutput output = new CapturingCommandOutput();
        CommandSourceStack source = new CommandSourceStack(
                output,
                serverPlayer.position(),
                new Vec2(serverPlayer.getXRot(), serverPlayer.getYRot()),
                level,
                4,
                serverPlayer.getName().getString(),
                serverPlayer.getDisplayName(),
                server,
                serverPlayer
        );
        int result = server.getCommands().performPrefixedCommand(source, command);
        return CommandResult.success(output.lines(), result);
    }

    public record CommandResult(boolean success, List<String> output, int result, String error) {
        public static CommandResult success(List<String> output, int result) {
            return new CommandResult(true, output, result, "");
        }

        public static CommandResult error(String error) {
            return new CommandResult(false, List.of(), 0, error);
        }
    }
}
