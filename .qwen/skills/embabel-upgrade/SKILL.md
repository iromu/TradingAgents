---
name: embabel-upgrade
description: Upgrade an Embabel Spring Boot project to a newer version with custom OpenAI/LiteLLM configuration
source: auto-skill
extracted_at: '2026-06-07T11:44:13.707Z'
---

# Embabel Upgrade Procedure

Use this skill when upgrading an Embabel-based Spring Boot project to a newer version (e.g., 0.3.x → 0.5.x) and
configuring it to use a custom OpenAI-compatible endpoint (LiteLLM, local models, etc.).

## Step 1: Update pom.xml

### Dependency version

Update the `<embabel-agent.version>` property to the target version:

```xml

<embabel-agent.version>0.5.0-SNAPSHOT</embabel-agent.version>
```

### Replace LMStudio/Ollama starter with openai-custom

Remove the `embabel-agent-starter-lmstudio` or `embabel-agent-starter-ollama` dependency and add:

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-openai-custom</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
```

### Remove obsolete profiles

Remove `ollama-models`, `lmstudio-models`, and any other LLM-specific profiles that no longer exist in the new version.
Replace with a single `openai-custom` profile:

```xml
<profile>
    <id>openai-custom</id>
    <activation>
        <activeByDefault>true</activeByDefault>
    </activation>
    <dependencies>
        <dependency>
            <groupId>com.embabel.agent</groupId>
            <artifactId>embabel-agent-starter-openai-custom</artifactId>
            <version>${embabel-agent.version}</version>
        </dependency>
    </dependencies>
</profile>
```

### Ensure snapshot repository is configured

```xml
<repository>
    <id>embabel-snapshots</id>
    <url>https://repo.embabel.com/artifactory/libs-snapshot</url>
    <snapshots><enabled>true</enabled></snapshots>
</repository>
```

## Step 2: Configure application.yaml for Custom OpenAI

Add model role configuration and the custom OpenAI endpoint:

```yaml
embabel:
  models:
    default-llm: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
    llms:
      cheapest: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
      best: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
  agent:
    platform:
      models:
        openai:
          custom:
            api-key: ${OPENAI_API_KEY:your-dev-key}
            base-url: ${OPENAI_BASE_URL:http://spark.local:4000}
            models: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
      scanning:
        bean: false
```

Key points:

- **base-url**: Use the LiteLLM endpoint **without** `/v1` suffix (LiteLLM handles path internally).
- **Model roles**: `cheapest` and `best` map to `ModelProvider.CHEAPEST_ROLE` and `ModelProvider.BEST_ROLE` used in
  agent code.
- **Environment variables**: Use `${VAR:default}` syntax for flexibility across environments.
- **Logging personality**: Use a valid value from `[starwars, severance, colossus, hitchhiker, montypython]`.

## Step 3: Clean up application-{profile}.yaml

Remove any Ollama-specific model references (e.g., `OllamaModels.QWEN2_7B`) that no longer exist in the new version. If
the config record has required fields, provide null-safe defaults in the Java record constructor:

```java
public record TraderAgentConfig(LlmOptions tickerLlm, ...) {
    public TraderAgentConfig {
        if (tickerLlm == null) {
            tickerLlm = LlmOptions.withDefaultLlm();
        }
    }
}
```

## Step 4: Fix Common API Breaking Changes

### `TraderAgent.java` — parameter signatures

- `tickerFromUserInput(UserInput, Ai ai)` → `tickerFromUserInput(UserInput, OperationContext context)` and use
  `context.ai()` internally.
- `createObject(String.class, ...)` → `createObject(ActualRecordClass.class, ...)` for typed output (e.g.,
  `FundamentalsReport.class`).
- `RepeatUntilBuilder` still requires `ActionContext` in `debateInvestment`, not `OperationContext`.
- Use `StandardCharsets.UTF_8` instead of `Charset.defaultCharset()`.
- Remove unused inner records (e.g., dead `InvestmentDebateFeedback` that implements `Feedback` but is never used).

### `TraderAgentConfig.java` — record to class

If the record's null-defaults don't trigger (Spring Boot binds defaults for `@ConfigurationProperties`), convert to a
class with `@Nullable` fields and default-returning getters:

```java
@ConfigurationProperties("app.llm-options")
public class TraderAgentConfig {
    private LlmOptions tickerLlm;
    public LlmOptions getTickerLlm() {
        return tickerLlm != null ? tickerLlm : LlmOptions.withDefaultLlm();
    }
    public void setTickerLlm(LlmOptions tickerLlm) { this.tickerLlm = tickerLlm; }
}
```

### `FileCache.java` — security & concurrency

- Add path traversal validation in `fileForKey()`: reject keys containing `..` or absolute paths.
- Use `Files.createDirectories()` instead of `baseDir.mkdirs()` for thread safety.
- Add `ConcurrentHashMap`-based dedup for `getOrCompute()` to prevent double-computation.

### `AlphaVantageService.java` — reliability

- Include date range in cache keys for `getNews()`:
  `String cacheKey = String.format("%s_NEWS_%s_%s", ticker, startDate, endDate);`
- Configure `RestTemplate` with timeouts via `SimpleClientHttpRequestFactory` (e.g., 10s connect, 30s read).

### `TradingHtmxController.java` — form DTOs

- Convert `@Data class TickerForm { private final String content; }` → proper `record TickerForm(String content)`.
- Don't inject `TraderAgent` directly into controllers — use `AgentPlatform.agents()` lookup since `@Agent` classes are
  wrapped into `Agent` instances by the platform.

### Extract shared interfaces

If `TraderAgent.Report` was an inner interface used by domain records, extract it to a standalone `domain/Report.java`
to avoid circular dependencies.

## Step 5: Verify

1. Run `./mvnw compile` — should compile cleanly.
2. Run `./mvnw test` — all tests should pass.
3. If tests reference old model enums or APIs, update them to use the new patterns (e.g., `LlmOptions.withDefaultLlm()`
   instead of `OllamaModels.XXX`).

## Common Pitfalls

- **`/v1` suffix**: Don't append `/v1` to the base URL when using `openai-custom` — the library handles OpenAI path
  routing internally.
- **Missing model roles**: If agent code uses `ai.withLlmByRole(CHEAPEST_ROLE)` or `BEST_ROLE`, you must configure
  `embabel.models.llms.cheapest` and `embabel.models.llms.best`.
- **Broken config records**: If a `@ConfigurationProperties` record field becomes null after removing YAML config, add a
  null check in the record's compact constructor or convert to a class with default-returning getters.
- **Old model enums**: `OllamaModels`, `LmStudioModels` etc. may be removed in newer versions. Replace with
  `LlmOptions.withDefaultLlm()` or explicit model name strings.
- **`RepeatUntilBuilder` API**: The `.repeating()` function receives a `RepeatUntilActionContext` (not the result type).
  Use `.until(ctx -> ctx.lastAttempt() != null && ctx.lastAttempt().count() >= 2)` for convergence checks.
- **`TraderAgent` in controllers**: `@Agent`-annotated classes are wrapped by the platform. Inject `AgentPlatform` and
  look up agents by name, don't inject the raw class.
