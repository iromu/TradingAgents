# Production Deployment

## Module Stability

Embabel modules carry one of three stability levels. This determines what you can safely ship.

| Status | Meaning |
|--------|---------|
| **Stable** | Production-ready. Breaking changes are avoided. |
| **Incubating** | Generally usable but may have breaking changes in minor releases. Graduates to Stable over time. Use with caution. |
| **Experimental** | Early-stage. Breaking changes in any release. May be removed without replacement. Not for production. |

### Modules relevant to production

| Module | Status |
|--------|--------|
| `embabel-agent-api` | Stable |
| `embabel-agent-code` | Stable |
| `embabel-agent-mcpserver` | Stable |
| `embabel-agent-openai` | Stable |
| `embabel-agent-shell` | Stable |
| `embabel-agent-rag-core` | Stable |
| `embabel-agent-rag-lucene` | Stable |
| `embabel-agent-starter` (all) | Stable |
| `embabel-agent-a2a` | Incubating |
| `embabel-agent-onnx` | Incubating |
| `embabel-agent-starter-onnx` | Incubating |
| `embabel-agent-discord` | Experimental |
| `embabel-agent-eval` | Experimental |
| `embabel-agent-remote` | Experimental |
| `embabel-agent-skills` | Experimental |
| `embabel-agent-spec` | Experimental |

> **Rule:** Pin your dependencies to Stable modules for production. Incubating modules are acceptable if you accept minor-version breakage risk. Never ship Experimental modules.

## Running in Production

### Prerequisites

- Java 21+ (JRE is sufficient for runtime)
- API key from a supported provider (OpenAI, Anthropic, Google, etc.)
- Maven 3.9+ only needed at build time

### Set API keys

```bash
export OPENAI_API_KEY="your_key"
export ANTHROPIC_API_KEY="your_key"
export GOOGLE_API_KEY="your_key"
```

For Google GenAI you can use either `GOOGLE_API_KEY` (AI Studio) or Vertex AI with `GOOGLE_PROJECT_ID` and `GOOGLE_LOCATION`.

### Quick start with a Spring Boot starter

Pick a starter matching your LLM provider:

```xml
<!-- OpenAI -->
<dependency>
    <groupId>io.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-openai</artifactId>
</dependency>

<!-- Anthropic -->
<dependency>
    <groupId>io.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-anthropic</artifactId>
</dependency>

<!-- Ollama (local) -->
<dependency>
    <groupId>io.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-ollama</artifactId>
</dependency>
```

## Docker Deployment

### Multi-stage Dockerfile

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
docker build -t my-agent:latest .
docker run -e OPENAI_API_KEY="$OPENAI_API_KEY" -p 8080:8080 my-agent:latest
```

### Docker-based local models

Embabel includes a `Docker Models` starter for running OpenAI-compatible models locally via Docker Desktop AI:

```xml
<dependency>
    <groupId>io.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-dockermodels</artifactId>
</dependency>
```

Configuration:

```yaml
embabel:
  docker:
    models:
      base-url: http://host.docker.internal:1234
      max-attempts: 3
      backoff-millis: 1000
      backoff-multiplier: 2.0
      backoff-max-interval: 30000
```

### MCP Docker integration

For agents needing Docker tools, use the Docker MCP Gateway (Docker Desktop MCP Toolkit extension):

```yaml
spring:
  ai:
    mcp:
      client:
        features:
          docker-mcp:
            command: docker
            args: []
```

## Health Checks

Embabel exposes Spring Boot Actuator endpoints and adds MCP-specific health indicators.

### Actuator endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator/health` | Liveness + readiness |
| `GET /actuator/health/liveness` | Liveness only |
| `GET /actuator/health/readiness` | Readiness only |
| `GET /actuator/info` | Application info |

Enable actuator:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### MCP server health

When exposing agents as MCP servers, enable the MCP health indicator:

```yaml
agent:
  mcpserver:
    health:
      enabled: true
      min-tools: 1
```

`min-tools` marks the component `DOWN` if fewer tools are registered than the threshold — useful for readiness probes that should wait until agent tools are fully exported.

### Kubernetes probes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```

## Container CPU Limits (Java 25+)

Java 25 accurately reads container cgroup CPU limits. In a container limited to 1 CPU, `availableProcessors()` returns 1, serializing code that relies on `ForkJoinPool.commonPool()`.

**Embabel core is unaffected** — it uses its own `Asyncer` abstraction with a dedicated unbounded executor. If you have custom code that may be affected, check for:

- `CompletableFuture.supplyAsync()` without an explicit executor
- Kotlin `Dispatchers.Default` (use `Dispatchers.IO` instead)
- Spring `applicationTaskExecutor` misconfiguration

Give the container enough CPU:

```yaml
resources:
  limits:
    cpu: "4"
  requests:
    cpu: "2"
```

## Security

### API key management

- Never hardcode API keys in source or Dockerfiles.
- Use environment variables, Kubernetes Secrets, or a vault (HashiCorp Vault, AWS Secrets Manager).

```yaml
# Kubernetes Secret example
env:
  - name: OPENAI_API_KEY
    valueFrom:
      secretKeyRef:
        name: api-keys
        key: openai
```

### Guardrails

Always enable guardrails in production to filter input and output. Guardrails provide safety/compliance boundaries that are essential when agents interact with external systems.

### Cost controls

Configure an `EarlyTerminationPolicy` to cap spending:

```yaml
embabel:
  agent:
    toolloop:
      max-iterations: 20
```

Set a listener to track and alert on costs in real time.

### MCP security

Two complementary layers protect MCP servers:

1. **HTTP transport** — All `/sse/**`, `/mcp/**`, `/message/**` endpoints require a valid JWT Bearer token.
2. **`@SecureAgentTool`** — Per-tool SpEL expressions that check what the caller's badge permits.

### HTTPS

Always use HTTPS in production for REST and SSE endpoints. Configure your reverse proxy or ingress controller to terminate TLS.

## Production Checklist

- [ ] Pin dependencies to Stable modules only
- [ ] Set `toolloop.max-iterations` to a reasonable limit (e.g., 20)
- [ ] Configure `EarlyTerminationPolicy` for cost caps
- [ ] Enable guardrails for input/output validation
- [ ] Use environment variables or a secrets manager for API keys (never hardcode)
- [ ] Set up a `CostTrackingListener` for spend monitoring
- [ ] Configure logging level — DEBUG is for development, not production
- [ ] Enable actuator health checks (`health,info`)
- [ ] Configure MCP server health with `min-tools` for readiness probes
- [ ] Set Kubernetes resource requests and limits
- [ ] Use HTTPS for all endpoints
- [ ] Test `EarlyTerminationPolicy` to verify cost caps work under load
- [ ] Set up monitoring/alerting for process failures and SSE events
- [ ] Configure backup for `ContextRepository` if using persistent context
- [ ] On Java 25+, verify container CPU limits are sufficient for parallelism needs
---

*Source: Embabel Agent v1.0.0 documentation*
