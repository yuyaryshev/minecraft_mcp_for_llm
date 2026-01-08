# MCP for LLM minecraft

This mod starts a local HTTP server so you can issue Minecraft commands and get structured responses.

## Server
- Host: `127.0.0.1`
- Port: `25576` (configurable)
- Config file: `run/config/mcp_for_llm-common.toml`

## Endpoints

### GET /health
Returns server status and whether a world is loaded.

Example response:
```json
{
  "status": "ok",
  "worldLoaded": true,
  "serverRunning": true,
  "screen": "none",
  "levelName": "minecraft:overworld",
  "newErrors": 0
}
```

### POST /command
Execute any server command as the first connected player.

Request body:
```json
{ "command": "/time query day" }
```

Example response:
```json
{
  "success": true,
  "result": 1,
  "output": ["The time is 1234"]
}
```

### POST /world/open
Request loading a world by id or use last modified save if omitted.

Request body:
```json
{ "worldId": "MyWorld" }
```

### POST /player/teleport
Teleport the local player to coordinates (yaw/pitch optional).

Request body:
```json
{ "x": 0, "y": 80, "z": 0, "yaw": 0, "pitch": 0 }
```

### POST /client/close
Close the Minecraft client if enabled by config.

Request body:
```json
{}
```

### GET /logs/check
Returns new log error lines since the last check (useful for catching startup exceptions).

Example response:
```json
{
  "success": true,
  "newErrors": 1,
  "recentErrors": ["...Exception..."]
}
```

## Notes
- The command executor uses the first connected player, so open a singleplayer world first.
- To suppress auto-world loading on startup, set `client.autoOpenLastWorld=false` in `mcp_for_llm-common.toml`.
- To disable the server or close endpoint, edit `mcp_for_llm-common.toml`.
