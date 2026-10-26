# Modules - the real list

`pom.xml:25-35` declares exactly nine modules. The six `embabel-dice-*` artifactIds (`-core`,
`-neo4j`, `-prolog`, `-vector`, `-agent`, `-rest`) that older guidance listed are **fabricated** and
occur zero times upstream.

| artifactId (`com.embabel.dice`) | What it is |
|------|------|
| `dice` | Core: pipeline, propositions, resolvers, projections, agent `Memory`, `DiceMcpTools` |
| `dice-storage` | The **one** Drivine-parameterised store (Neo4j / FalkorDB / Memgraph) + `DrivineMetamodelVersionStore` |
| `dice-storage-autoconfigure` | Spring Boot autoconfig: `DiceStorageAutoConfiguration`, `DiceStoreProperties`, `CollectorProperties` |
| `dice-report` | `ReportProjector`, `StructuredReportProjector`, `RationaleProjector`, `SemanticLinkDiscoverer` |
| `dice-ingestion` | `TextIngestionHandler`, `IngestionLedger` - dedup ledger in front of the pipeline |
| `dice-metamodel` | Schema versioning: `MetamodelVersion`, `DeclaredSchema`, `MetamodelVersionStore` |
| `dice-mcp-autoconfigure` | Exports the DICE MCP tools |
| `dice-integration-tests` | Tests only |
| `dice-user-guide` | The AsciiDoc guide |

There is **no per-backend projection module**: vector, graph and Prolog are *features*, not
artifacts. Graph is one `dice-storage` module switched by `embabel.dice.store.type`. `Memory` is not
a separate module either - it is `dice/src/main/kotlin/com/embabel/dice/agent/Memory.kt`.

DICE publishes no aggregator starter. The quickstart uses two coordinates -
`dice-storage-autoconfigure` (pulls `dice` + `dice-storage`) plus `dice-report` - and an
embabel-agent runtime, because DICE declares `embabel-agent-api` and `embabel-agent-rag-core` as
`provided`.

```xml
<!-- Version comes from the aggregator's dependencyManagement when you build inside the reactor. -->
<dependency>
    <groupId>com.embabel.dice</groupId>
    <artifactId>dice-storage-autoconfigure</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
<repositories>
    <repository>
        <id>embabel-snapshots</id>
        <url>https://repo.embabel.com/artifactory/libs-snapshot</url>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
```
