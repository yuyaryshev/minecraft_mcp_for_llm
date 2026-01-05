# MCP for LLM minecraft - LLM Instruction

Goal: let another Codex instance add this mod as a dependency, run it for automated testing, and report missing MCP features.

## Quick Summary
- This mod provides a local HTTP MCP-style server to run commands in Minecraft.
- It is published to a local file maven repo: `minecraft_mcp_for_llm/mcmodsrepo`.
- You can add it as a `fg.deobf` dependency in another Forge 1.20.1 mod.

## Install As Dependency (in another mod)

1) Add the local maven repo:
```gradle
repositories {
    maven { url = uri("D:/b/Mine/GIT_Work/minecraft/mods_2025-12-23/minecraft_mcp_for_llm/mcmodsrepo") }
}
```

2) Add dependency:
```gradle
dependencies {
    implementation fg.deobf("com.yymod.mcpforllm:mcp_for_llm:0.1.0-1.20.1")
}
```

3) Sync/refresh Gradle.

Notes:
- If the mod is updated, run `gradlew publish` in `minecraft_mcp_for_llm` again.
- If your workspace path is different, adjust the `maven { url = ... }` to that path.

## Run and Test Without Human Interaction

1) Start the client via Gradle:
```powershell
.\gradlew.bat runClient
```

2) Wait for MCP server:
```powershell
Invoke-RestMethod http://127.0.0.1:25576/health
```

3) When `worldLoaded` is true, issue commands:
```powershell
$body = @{ command = "/time query day" } | ConvertTo-Json
Invoke-RestMethod -Uri http://127.0.0.1:25576/command -Method Post -ContentType "application/json" -Body $body
```

4) Optional: teleport player
```powershell
$body = @{ x = 0; y = 80; z = 0 } | ConvertTo-Json
Invoke-RestMethod -Uri http://127.0.0.1:25576/player/teleport -Method Post -ContentType "application/json" -Body $body
```

5) Close client when done:
```powershell
Invoke-RestMethod -Uri http://127.0.0.1:25576/client/close -Method Post -ContentType "application/json" -Body "{}"
```

## What To Report Back (Missing Features)

Report to the main Codex instance:
- Missing endpoints needed for your test workflow.
- Need to target specific players instead of "first connected player".
- Need to run server-side-only commands before a player joins.
- Need world selection or seed selection instead of "last modified save".
- Need access to logs, chat messages, or screenshots.
- Need event subscriptions (player join, entity spawn, errors).
- Any command failures or timeouts.

Include:
- The command you tried.
- MCP response JSON.
- Game state (world loaded, player present, singleplayer/multiplayer).
- Expected vs actual behavior.

## Config (optional)
Edit `run/config/mcp_for_llm-common.toml`:
- `mcp.autoStart` (default true)
- `mcp.port` (default 25576)
- `client.autoOpenLastWorld` (default true)
- `mcp.allowCloseClient` (default true)
