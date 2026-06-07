# Embabel Configuration Reference

## Enabling Embabel

Annotate your Spring Boot application class:

```java

@SpringBootApplication
public class MyAgentApplication {
    static void main(String[] args) {
        SpringApplication.run(MyAgentApplication.class, args);
    }
}
```

## Configuration Properties

### Default LLM and Roles

| Property                                 | Type                | Default        | Description                      |
|------------------------------------------|---------------------|----------------|----------------------------------|
| `embabel.models.default-llm`             | String              | `gpt-4.1-mini` | Default LLM name                 |
| `embabel.models.default-embedding-model` | String              | `null`         | Default embedding model          |
| `embabel.models.llms`                    | Map<String, String> | `{}`           | Map of role to LLM name          |
| `embabel.models.embedding-services`      | Map<String, String> | `{}`           | Map of role to embedding service |

```yaml
embabel:
  models:
    default-llm: gpt-4.1-mini
    default-embedding-model: text-embedding-3-small
    llms:
      cheapest: gpt-4o-mini
      best: gpt-4o
      reasoning: o1-preview
    embedding-services:
      fast: text-embedding-3-small
      accurate: text-embedding-3-large
```

### Platform Configuration

| Property                             | Type   | Default                          | Description                 |
|--------------------------------------|--------|----------------------------------|-----------------------------|
| `embabel.agent.platform.name`        | String | `embabel-default`                | Core platform identity name |
| `embabel.agent.platform.description` | String | `Embabel Default Agent Platform` | Platform description        |

### Logging Personality

| Property                            | Type   | Default  | Description             |
|-------------------------------------|--------|----------|-------------------------|
| `embabel.agent.logging.personality` | String | _(none)_ | Themed logging messages |

Available values: `starwars`, `severance`, `colossus`, `hitchhiker`, `montypython`

### Agent Scanning

| Property                                     | Type    | Default | Description                                      |
|----------------------------------------------|---------|---------|--------------------------------------------------|
| `embabel.agent.platform.scanning.annotation` | Boolean | `true`  | Auto-register beans with `@Agent` and `@Agentic` |
| `embabel.agent.platform.scanning.bean`       | Boolean | `false` | Auto-register Spring beans of type `Agent`       |

### Tool Loop

| Property                                                       | Type     | Default   | Description                                         |
|----------------------------------------------------------------|----------|-----------|-----------------------------------------------------|
| `embabel.agent.platform.toolloop.type`                         | String   | `default` | `default` (sequential) or `parallel` (experimental) |
| `embabel.agent.platform.toolloop.max-iterations`               | Int      | `20`      | Max tool loop iterations                            |
| `embabel.agent.platform.toolloop.parallel.per-tool-timeout`    | Duration | `30s`     | Timeout per tool in parallel mode                   |
| `embabel.agent.platform.toolloop.parallel.batch-timeout`       | Duration | `60s`     | Timeout for entire batch in parallel mode           |
| `embabel.agent.platform.toolloop.empty-response.max-retries`   | Int      | `0`       | Max retries for empty responses (weak models)       |
| `embabel.agent.platform.toolloop.empty-response.nudge-message` | String   | _(none)_  | Message appended when LLM goes silent               |

### Autonomy

| Property                                                   | Type   | Default | Description                               |
|------------------------------------------------------------|--------|---------|-------------------------------------------|
| `embabel.agent.platform.autonomy.agent-confidence-cut-off` | Double | `0.6`   | Confidence threshold for agent operations |
| `embabel.agent.platform.autonomy.goal-confidence-cut-off`  | Double | `0.6`   | Confidence threshold for goal achievement |

### HTTP Client

Add `embabel-agent-netty-client-autoconfigure` for Netty:

| Property                                             | Type     | Default | Description        |
|------------------------------------------------------|----------|---------|--------------------|
| `embabel.agent.platform.http-client.connect-timeout` | Duration | `25s`   | Connection timeout |
| `embabel.agent.platform.http-client.read-timeout`    | Duration | `1m`    | Read timeout       |

### REST Endpoints

| Property                                             | Type    | Default | Description                   |
|------------------------------------------------------|---------|---------|-------------------------------|
| `embabel.agent.platform.rest.process-status-enabled` | Boolean | `true`  | `GET /api/v1/process/{id}`    |
| `embabel.agent.platform.rest.process-kill-enabled`   | Boolean | `true`  | `DELETE /api/v1/process/{id}` |
| `embabel.agent.platform.rest.process-events-enabled` | Boolean | `true`  | `GET /events/process/{id}`    |

### Process Repository

| Property                                                | Type | Default | Description                  |
|---------------------------------------------------------|------|---------|------------------------------|
| `embabel.agent.platform.process-repository.window-size` | Int  | `1000`  | Max processes kept in memory |

### Test Configuration

| Property                                | Type    | Default | Description                  |
|-----------------------------------------|---------|---------|------------------------------|
| `embabel.agent.platform.test.mock-mode` | Boolean | `true`  | Enable mock mode for testing |
