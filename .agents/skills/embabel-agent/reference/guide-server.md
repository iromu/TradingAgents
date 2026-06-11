# Embabel Guide Server Reference

The Guide server is a companion project that exposes Embabel documentation, blogs, and API information via chat, Spring Shell, and MCP.

## Quick Start

### Running Locally

```bash
export OPENAI_API_KEY=your_key_here
./mvnw spring-boot:run
```

Server starts on port `1337`.

### Docker Compose

```bash
# Start Neo4j + Guide
docker compose --profile java up --build -d

# Start Neo4j only (for local Java dev)
docker compose up neo4j -d

# Stop
docker compose --profile java down --remove-orphans
```

#### Port Conflicts

```bash
GUIDE_PORT=1338 docker compose --profile java up --build -d
```

## Loading Data

```bash
# Load docs into RAG store
curl -X POST http://localhost:1337/api/v1/data/load-references

# Check stats
curl http://localhost:1337/api/v1/data/stats
```

RAG storage uses `ChunkingContentElementRepository` from `embabel-agent-rag-core`, defaulting to Neo4j via `DrivineStore`.

## Graph Database Backends

| Backend  | Profile    | Port  | Compose Profile |
|----------|------------|-------|-----------------|
| Neo4j    | `neo4j`    | 7687  | `neo4j`         |
| FalkorDB | `falkordb` | 6379  | `falkordb`      |
| Memgraph | `memgraph` | 7688  | `memgraph`      |

### Switching backends

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=falkordb
```

### Browsing data

- **Neo4j**: http://localhost:7474/browser/ (user `neo4j`, password `brahmsian`)
- **FalkorDB**: http://localhost:3001
- **Memgraph**: [Memgraph Lab](https://memgraph.com/lab) → `bolt://localhost:7688`

### Delete all data

```cypher
MATCH (n:ContentElement) DETACH DELETE n
```

## MCP Server

Exposes MCP tools at `http://localhost:1337/sse`.

### Verify

```bash
curl -i --max-time 3 http://localhost:1337/sse
# Expect: Content-Type: text/event-stream + event:endpoint
```

### MCP Inspector

```bash
npx @modelcontextprotocol/inspector
# Connect to http://localhost:1337/sse
```

## MCP Client Configurations

### Claude Desktop

`claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "embabel-dev": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:1337/sse", "--transport", "sse-only"]
    }
  }
}
```

### Claude Code

```bash
claude mcp add embabel --transport sse http://localhost:1337/sse
# Then in Claude Code: /mcp to test
```

Auto-approve tools in `.claude/settings.local.json` (wildcards don't work — approve each tool individually).

### Codex

`.codex/config.toml`:

```toml
[mcp_servers.embabel_guide]
command = "npx"
args = ["-y", "mcp-remote", "http://localhost:1337/sse", "--transport", "sse-only"]
startup_timeout_sec = 60
tool_timeout_sec = 120
```

### Cursor

`~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "embabel-dev": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:1337/sse", "--transport", "sse-only"]
    }
  }
}
```

Reload via **Command Palette → Developer: Reload Window** if server starts after Cursor.

### Antigravity

Open MCP store → "..." dropdown → "Manage MCP Servers" → "View raw config". Edit `mcp_config.json`.

### Copilot CLI

`$HOME/.copilot/mcp-config.json`:

```json
{
  "mcpServers": {
    "embabel-dev": {
      "type": "sse",
      "url": "http://localhost:1337/sse",
      "tools": ["*"]
    }
  }
}
```

## WebSocket Chat API

**Endpoint:** `ws://localhost:1337/ws` (STOMP over WebSocket with SockJS fallback)

### STOMP Channels

| Direction | Destination             | Purpose               |
|-----------|-------------------------|-----------------------|
| Subscribe | `/user/queue/messages`  | Chat responses        |
| Subscribe | `/user/queue/status`    | Typing/status updates |
| Publish   | `/app/chat.sendToJesse` | Send message to bot   |
| Publish   | `/app/presence.ping`    | Keep-alive (30s)      |

### Minimal JS Client

```javascript
import {Client} from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:1337/ws'),
    onConnect: () => {
        client.subscribe('/user/queue/messages', (frame) => {
            console.log(JSON.parse(frame.body).content);
        });
        client.publish({
            destination: '/app/chat.sendToJesse',
            body: JSON.stringify({body: 'Hello!'})
        });
    }
});
client.activate();
```

## REST API

CORS open (`*`).

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/hub/register` | POST | Register user |
| `/api/hub/login` | POST | Login (returns JWT) |
| `/api/hub/personas` | GET | List personas |
| `/api/hub/persona/mine` | PUT | Update persona (requires Bearer token) |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `GUIDE_PORT` | `1337` | Host port mapping |
| `OPENAI_API_KEY` | — | OpenAI key |
| `ANTHROPIC_API_KEY` | — | Anthropic key |
| `MISTRAL_API_KEY` | — | Mistral key |
| `DEEPSEEK_API_KEY` | — | DeepSeek key |
| `NEO4J_PASSWORD` | `brahmsian` | Neo4j password |
| `EMBABEL_KEY_SECRET` | — | AES key for BYOK |
| `DISCORD_TOKEN` | — | Discord bot token |

## Testing

```bash
export OPENAI_API_KEY=sk-your-key-here
USE_LOCAL_NEO4J=true ./mvnw test
```

### Kill orphaned server

```bash
lsof -ti:1337 | xargs kill -9
```
