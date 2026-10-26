# Step 9 - Expose DICE over MCP

`dice-mcp-autoconfigure` is real, registered and current (HEAD is a merged MCP PR). It is **not**
aspirational: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
contains exactly `com.embabel.dice.mcp.autoconfigure.DiceMcpAutoConfiguration`.

```xml
<dependency>
    <groupId>com.embabel.dice</groupId>
    <artifactId>dice-mcp-autoconfigure</artifactId>
</dependency>
<!-- UNRESOLVED: pick the MCP-server starter after checking embabel/embabel-agent; both candidate
     artifactIds are named above and neither is confirmed by the DICE corpus. -->
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-mcpserver</artifactId>   <!-- or embabel-agent-mcpserver -->
</dependency>
```

```yaml
embabel:
  dice:
    mcp:
      enabled: true          # the gate; off means no beans at all
      writes-enabled: true   # second, separate switch for dice_store
```

The four `@LlmTool` tools live in `dice/src/main/kotlin/com/embabel/dice/mcp/DiceMcpTools.kt`:

| Tool name | Kotlin function | Behaviour |
|-----------|-----------------|-----------|
| `dice_recall` | `recall(contextId, query?, limit?)` | hybrid semantic + keyword; omit `query` to list by confidence |
| `dice_list` | `listMemories(contextId, limit?)` | active propositions ordered by effective confidence |
| `dice_store` | `storeMemory(contextId, text, confidence?)` | writes without extraction; gated by `writes-enabled` |
| `dice_get` | `getProposition(contextId, propositionId)` | one by id, includes status so a stale fact does not read as active |

Gates on the autoconfiguration: `@AutoConfiguration(afterName =
["com.embabel.dice.storage.autoconfigure.DiceStorageAutoConfiguration"])`,
`@ConditionalOnClass(McpToolExport::class)`, `@ConditionalOnProperty(prefix = "embabel.dice.mcp",
name = ["enabled"], havingValue = "true")`, `@EnableConfigurationProperties(DiceMcpProperties::class)`.
Beans: `diceMcpTools(...)` (needs a `PropositionRepository` bean) and `diceMcpToolExport(...)`, which
filters out `DiceMcpTools.STORE` unless `writesEnabled`. `limit` is clamped to `MAX_LIMIT` (100) at
call time, but a `default-limit` outside `1..100` **fails startup** rather than being clamped.

Two security properties to state to users: `contextId` on every call is a **scope, not a credential**
(authorization is the host MCP server's job), and `dice_store` writes with empty mentions and no
provenance, so such facts are reachable by vector/keyword only - never by entity expansion or graph
projection. Use the ingestion pipeline when a fact must be wired into the graph.
