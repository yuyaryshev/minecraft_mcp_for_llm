Todo
- Provide local MCP-style HTTP endpoints for commands, health checks, player teleport, and client close.
- Auto-open last played world on client startup.
- Remove template mod content and unused dependencies/resources.

Developed
- Implemented `McpServer` HTTP server bound to `127.0.0.1:25576` with `/health`, `/command`, `/player/teleport`, and `/client/close`.
- Implemented command execution pipeline with captured output using server-side `CommandSourceStack`.
- Added auto-open logic that selects the most recently modified save under `run/saves`.
- Trimmed Gradle dependencies and removed Create/JEI/Jade/KubeJS assets.

Tested
- Built with `gradlew.bat build`.
- Started client with `gradlew.bat runClient`.
- Verified `/health` returns `worldLoaded: true` after world loads.
- Summoned a zombie via `/command` and retrieved entity data via `/data get entity ...`.
