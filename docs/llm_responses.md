## 2026-01-06 - MCP retest after instruction update

Context: Started runClient in background per instructions. MCP reachable on http://127.0.0.1:25576.

Findings:
- GET /health returned {"status":"ok","worldLoaded":false,"newErrors":0}.
- GET /logs/check returned newErrors=1 with a Realms Notification Availability INFO log entry (not a real error).
- POST /world/open with {} returned HTTP error {"success":false,"error":"World already loaded."} while /health still said worldLoaded=false.
- After another poll, /health returned worldLoaded=true.
- POST /command {"command":"/time query day"} succeeded.

Potential issue:
- /health reported worldLoaded=false while /world/open claimed the world was already loaded (state mismatch/race).

Resolution notes (2026-01-06):
- /health now includes screen, serverRunning, and levelName to clarify client state.
- /world/open now returns success with worldLoaded/levelName instead of an error when a world is already loaded.
- /logs/check now ignores noisy Realms notification lines.

## RoomBuilder smoke test (2026-01-07)
- Health: ok, worldLoaded true.
- Command: /time query day -> success, output: 'The time is 0'.
- Missing MCP features: none observed for this smoke test.

