package com.yymod.mcpforllm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class McpServer {
    private static final Gson GSON = new Gson();
    private static final String HOST = "127.0.0.1";
    private static final int COMMAND_TIMEOUT_SECONDS = 10;
    private static final LogMonitor LOG_MONITOR = new LogMonitor();

    private static HttpServer server;
    private static ExecutorService executor;

    private McpServer() {
    }

    public static synchronized void startIfEnabled() {
        if (!McpConfig.AUTO_START_SERVER.get()) {
            McpForLlmMod.LOGGER.info("MCP server auto-start disabled by config.");
            return;
        }
        start();
    }

    public static synchronized void start() {
        if (server != null) {
            return;
        }
        int port = McpConfig.SERVER_PORT.get();
        try {
            server = HttpServer.create(new InetSocketAddress(HOST, port), 0);
        } catch (IOException e) {
            McpForLlmMod.LOGGER.error("Failed to start MCP server on port {}.", port, e);
            server = null;
            return;
        }
        executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "mcp-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/health", new HealthHandler());
        server.createContext("/command", new CommandHandler());
        server.createContext("/logs/check", new LogCheckHandler());
        server.createContext("/world/open", new WorldOpenHandler());
        server.createContext("/player/teleport", new TeleportHandler());
        server.createContext("/client/close", new CloseClientHandler());
        server.start();
        McpForLlmMod.LOGGER.info("MCP server started on http://{}:{}/", HOST, port);
    }

    public static synchronized void stop() {
        if (server == null) {
            return;
        }
        server.stop(0);
        server = null;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        McpForLlmMod.LOGGER.info("MCP server stopped.");
    }

    private static void sendJson(HttpExchange exchange, int statusCode, JsonObject body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static JsonObject error(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("success", false);
        json.addProperty("error", message);
        return json;
    }

    private static JsonObject success(List<String> output, int result) {
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("result", result);
        JsonArray lines = new JsonArray();
        for (String line : output) {
            lines.add(line);
        }
        json.add("output", lines);
        return json;
    }

    private static final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JsonObject json = new JsonObject();
            Minecraft minecraft = Minecraft.getInstance();
            json.addProperty("status", "ok");
            json.addProperty("worldLoaded", minecraft.level != null);
            json.addProperty("serverRunning", minecraft.getSingleplayerServer() != null);
            json.addProperty("screen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName());
            if (minecraft.level != null) {
                json.addProperty("levelName", minecraft.level.dimension().location().toString());
            }
            List<String> errors = LOG_MONITOR.readNewErrors();
            json.addProperty("newErrors", errors.size());
            if (!errors.isEmpty()) {
                json.add("recentErrors", GSON.toJsonTree(errors));
            }
            sendJson(exchange, 200, json);
        }
    }

    private static final class LogCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, error("Use GET."));
                return;
            }
            List<String> errors = LOG_MONITOR.readNewErrors();
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("newErrors", errors.size());
            json.add("recentErrors", GSON.toJsonTree(errors));
            sendJson(exchange, 200, json);
        }
    }

    private static final class CommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, error("Use POST."));
                return;
            }
            String body = readBody(exchange);
            JsonObject request = GSON.fromJson(body, JsonObject.class);
            if (request == null || !request.has("command")) {
                sendJson(exchange, 400, error("Missing 'command' in JSON body."));
                return;
            }
            String command = Objects.requireNonNull(request.get("command")).getAsString();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            try {
                McpCommandExecutor.CommandResult result = McpCommandExecutor.executeBlocking(command, COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!result.success()) {
                    sendJson(exchange, 409, error(result.error()));
                    return;
                }
                sendJson(exchange, 200, success(result.output(), result.result()));
            } catch (Exception e) {
                sendJson(exchange, 500, error("Command handler failed: " + e.getMessage()));
            }
        }
    }

    private static final class WorldOpenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, error("Use POST."));
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                JsonObject json = new JsonObject();
                json.addProperty("success", true);
                json.addProperty("message", "World already loaded.");
                json.addProperty("worldLoaded", true);
                json.addProperty("levelName", minecraft.level.dimension().location().toString());
                sendJson(exchange, 200, json);
                return;
            }
            String body = readBody(exchange);
            JsonObject request = body == null || body.isBlank() ? new JsonObject() : GSON.fromJson(body, JsonObject.class);
            String worldId = request != null && request.has("worldId") ? request.get("worldId").getAsString() : null;
            if (worldId == null || worldId.isBlank()) {
                worldId = McpWorldOpener.findLastWorld();
            }
            if (worldId == null || worldId.isBlank()) {
                sendJson(exchange, 404, error("No world found to open."));
                return;
            }
            String finalWorldId = worldId;
            minecraft.execute(() -> McpWorldOpener.openWorld(minecraft,
                    minecraft.screen != null ? minecraft.screen : new TitleScreen(),
                    finalWorldId));
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("worldId", finalWorldId);
            json.addProperty("message", "World open requested.");
            sendJson(exchange, 200, json);
        }
    }

    private static final class TeleportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, error("Use POST."));
                return;
            }
            String body = readBody(exchange);
            JsonObject request = GSON.fromJson(body, JsonObject.class);
            if (request == null || !request.has("x") || !request.has("y") || !request.has("z")) {
                sendJson(exchange, 400, error("Missing 'x', 'y', or 'z' in JSON body."));
                return;
            }
            double x = request.get("x").getAsDouble();
            double y = request.get("y").getAsDouble();
            double z = request.get("z").getAsDouble();
            String command = "tp @s " + x + " " + y + " " + z;
            if (request.has("yaw") && request.has("pitch")) {
                command += " " + request.get("yaw").getAsDouble() + " " + request.get("pitch").getAsDouble();
            }
            McpCommandExecutor.CommandResult result = McpCommandExecutor.executeBlocking(command, COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!result.success()) {
                sendJson(exchange, 500, error(result.error()));
                return;
            }
            sendJson(exchange, 200, success(result.output(), result.result()));
        }
    }

    private static final class CloseClientHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, error("Use POST."));
                return;
            }
            if (!McpConfig.ALLOW_CLOSE_CLIENT.get()) {
                sendJson(exchange, 403, error("Client close disabled by config."));
                return;
            }
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("message", "Client closing.");
            sendJson(exchange, 200, json);
        }
    }
}
