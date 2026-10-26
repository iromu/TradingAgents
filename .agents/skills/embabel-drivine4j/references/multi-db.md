# Multi-Database Configuration Reference

## Table of Contents

1. [Engines](#engines)
2. [ConnectionProperties](#connectionproperties)
3. [Multiple Datasources](#multiple-datasources)
4. [YAML Properties and the Starter](#yaml-properties-and-the-starter)
5. [Cypher Dialects](#cypher-dialects)
6. [Engine Notes](#engine-notes)
7. [Testing](#testing)

## Engines

Four engines are usable, from one codebase -- models, queries and DSL code stay the same:

| Engine | `DatabaseType` | Transactions | Collection sort | Auth |
|--------|----------------|--------------|-----------------|------|
| Neo4j 5.x | `NEO4J` | full ACID | APOC (default) or `CALL` subquery | basic (user/pass) |
| Neo4j 4.x | `NEO4J` | full ACID | APOC required | basic (user/pass) |
| FalkorDB | `FALKORDB` | passthrough, no multi-statement | `CALL` subquery | none |
| Amazon Neptune | `NEPTUNE` | full ACID | `CALL` subquery | IAM SigV4 or none (tunnel) |
| Memgraph | `MEMGRAPH` | full ACID | `CALL` subquery | basic or none |
| Embabel in-process Cypher | `EMBABEL` | per the provider you register | per the provider | n/a -- no wire |

`POSTGRES` is declared in the enum but has no provider, no connection and no builder branch -- it
throws on registration. See `SKILL.md` section  DatabaseType.

## ConnectionProperties

```kotlin
data class ConnectionProperties(
    val type: DatabaseType,               // REQUIRED
    val host: String = "",                // empty for engines with no wire
    val port: Int? = null,
    val userName: String? = null,         // capital N
    val password: String? = null,
    val idleTimeout: Int? = null,
    val connectionTimeout: Int? = null,
    val poolMax: Int? = null,
    val databaseName: String? = null,
    val defaultGraphPath: String? = null,
    val protocol: String? = null,
    val cypherDialect: CypherDialect? = null,
    val falkorDbTransactionMode: FalkorDbTransactionMode? = null,
    val region: String? = null,
    val neptuneAuth: NeptuneAuthMode? = null,
)
```

> **Doc-vs-code divergence, recorded deliberately.** The upstream README's "Quick Start" and
> "API Reference" sections show `host = "localhost"`, `port = 7687`, `username = ...`,
> `database = ...`, `encrypted = false` and no `type`. None of those names compile against
> `src/main/kotlin/org/drivine/connection/ConnectionProperties.kt`: `type` is a required first
> parameter, the fields are `userName` and `databaseName`, and there is no `encrypted` field. The
> README's own "Manual TestContainers Setup" section uses the correct names (`userName`,
> `type = DatabaseType.NEO4J`, `databaseName`), so the README is internally inconsistent. Trust the
> signature above; the README's narrative is otherwise authoritative.

`type` drives everything downstream, and `DatabaseType.buildableFromProperties` is the single
topology fact the registry needs -- whether host/port can describe a connection at all.

## Multiple Datasources

```kotlin
@Configuration
class MultiDbConfig {
    @Bean
    fun dataSourceMap() = DataSourceMap(mapOf(
        "analytics" to ConnectionProperties(
            type = DatabaseType.NEO4J, host = "analytics.neo4j.com", databaseName = "analytics",
        ),
        "users" to ConnectionProperties(
            type = DatabaseType.MEMGRAPH, host = "users.neo4j.com", databaseName = "users",
        ),
    ))
}

@Component
class AnalyticsRepository @Autowired constructor(
    factory: PersistenceManagerFactory,
) {
    val manager: PersistenceManager = factory.get("analytics")
}

@Component
class UserRepository @Autowired constructor(
    factory: PersistenceManagerFactory,
) {
    val manager: PersistenceManager = factory.get("users")
}
```

Each key is the datasource name passed to `PersistenceManagerFactory.get(database)` /
`GraphObjectManagerFactory.get(database)`, and is also the `ConnectionProvider.name`.
`DatabaseRegistry` keys providers by name and preserves registration order;
`connectionProvider(name = "default")` and `resolveDatabaseName("default")` both resolve to the
**first-registered** datasource, which is what `SchemaCatalog.forDefaultDatabase()` targets too.

> **Doc-vs-code divergence, recorded deliberately.** The README shows
> `@Autowired @Qualifier("analytics") val manager: PersistenceManager`. Neither the starter nor the
> sample uses `@Qualifier` or registers per-datasource manager beans, and the README's other section
> uses a different convention (`@Qualifier("neoManager")`) -- the two do not agree. The factories are
> what the starter actually publishes, so name the datasource at the `get(...)` call as shown above.
> `PersistenceManagerType` (`DELEGATING` by default) is the second parameter.

Without Spring, build providers directly:

```kotlin
val registry = DatabaseRegistry(dataSourceMap)
registry.builder()
    .withType(DatabaseType.FALKORDB)
    .host("localhost").port(6379).databaseName("mygraph")
    .cypherDialect(CypherDialect.FALKORDB)
    .register("graph")
```

`register` requires a host -- except for an engine with `buildableFromProperties = false`, which it
rejects up front with a message telling you to supply your own `ConnectionProvider` instead of
puzzling over a missing host.

## YAML Properties and the Starter

`drivine4j-spring-boot-starter` publishes `@EnableDrivine`, `@EnableDrivineTestConfig`,
`@EnableDrivinePropertiesConfig`, and two `@ConfigurationProperties` bindings:
`DrivineProperties` at prefix `database` (a `datasources: Map<String, ConnectionProperties>`), and
`DrivineQueryProperties` at prefix `drivine.query`.

```yaml
database:
  datasources:
    graph:
      type: FALKORDB
      host: localhost
      port: 6379
      database-name: mygraph
      falkor-db-transaction-mode: STRICT     # default WARN
```

```yaml
database:
  datasources:
    graph:
      type: NEPTUNE
      host: your-cluster.us-east-1.neptune.amazonaws.com
      port: 8182
      region: us-east-1
      neptune-auth: IAM
```

The starter artifact coordinate is not named anywhere in the README; `org.drivine:drivine4j-spring-boot-starter`
is inferred from `settings.gradle` (`include 'drivine4j-spring-boot-starter'`) plus the module's
uniform `group = 'org.drivine'` / `version = rootProject.version` with `maven-publish`. Treat it as
build-derived rather than documentation-attested.

## Cypher Dialects

`CypherDialect` selects the `CypherGrammar` (DML divergence: existence checks, collection sort
emission, nested view projection) and the `SchemaGrammar` (DDL). Members: `NEO4J_5`, `NEO4J_4`,
`OPEN_CYPHER`, `FALKORDB`, `NEPTUNE`, `MEMGRAPH`.

- `grammar(sortEmitterOverride)` builds the DML grammar -- Neo4j variants default to
  `ApocSortMapsEmitter`, the openCypher family to `CallSubqueryEmitter`. Neo4j 4 and 5 differ only
  in DML, so they share one schema grammar.
- `schemaGrammar()` returns `Neo4jSchemaGrammar` / `MemgraphSchemaGrammar` /
  `FalkorDbSchemaGrammar`, and `UnsupportedSchemaGrammar(name)` for `NEPTUNE` and `OPEN_CYPHER`.
- `ConnectionProvider.cypherDialect` defaults to `OPEN_CYPHER`; `Neo4jConnectionProvider` defaults
  to `NEO4J_5`. Override per datasource via `ConnectionProperties.cypherDialect` or
  `ConnectionProviderBuilder.cypherDialect(...)`.

> There is **no** `QuerySpecification.withDialect(...)` -- per-query dialect override is not part of
> the API. Dialect is a property of the datasource.

Set `OPEN_CYPHER` as the starting point when targeting a new openCypher-compatible engine.

## Engine Notes

### Neo4j

The default engine; works out of the box with Testcontainers or a local instance. Bolt on 7687.
Neo4j 5.x supports `EXISTS { pattern }`, APOC Extended, `CALL { }` subqueries and `COLLECT { }`;
collection sorting defaults to APOC and can be overridden to a `CALL` subquery emitter. Neo4j 4.x
uses the deprecated `exists(pattern)` function, requires APOC Extended for collection sorting, and
has no `CALL { }` subquery support.

### FalkorDB

In-memory graph database built on Redis; very fast, no multi-statement transactions. Port 6379.

- `FalkorDbTransactionMode.WARN` (default): `startTransaction`/`commitTransaction` are no-ops and
  `rollbackTransaction` warns, because writes already executed. `STRICT` throws on `@Transactional`
  if you want the misuse to be loud.
- Schema divergence is substantial -- see `references/schema-and-search.md` section  Schema Grammar
  Capability Flags (`indexOperationsAreAsync`, `constraintCreationIsAsync`,
  `constraintsRequireBackingIndex`, no `IF NOT EXISTS`, unnamed items, Redis-command constraints).
- Known upstream gaps Drivine works around: nested pattern comprehensions returning NULL
  (`FalkorDB#1888`, handled with `CALL` subquery prologs) and `collect()` including null maps
  (`FalkorDB#1889`, filtered with `CASE WHEN IS NOT NULL`).
- `DELETE_ORPHAN` needs a build with `FalkorDB#1890` fixed; older builds are unsupported for
  orphan delete.
- No native full-text on the Neo4j model -- full-text goes through `db.idx.fulltext.createNodeIndex`
  per property.

### Amazon Neptune

AWS's managed graph database, reached over Bolt with two auth modes (`NeptuneAuthMode.IAM` or
`NONE`). Port 8182.

- **IAM SigV4** (recommended for production) needs `neptune-auth: IAM` plus `region`, AWS
  credentials from the standard chain (`~/.aws/credentials`, env vars, IAM role) and the AWS SDK on
  the classpath (`software.amazon.awssdk:auth`, `:regions`, `:http-client-spi`). Tokens refresh
  before expiry.
- **No auth** (`neptune-auth: NONE`) pairs with an SSH tunnel to a cluster that has IAM disabled:
  `ssh -N -o ServerAliveInterval=60 -L 8182:your-cluster.neptune.amazonaws.com:8182 ec2-user@bastion-ip`
- No `date()` function -- use string dates or `datetime()`.
- No list/array property values -- annotate collection fields with `@JsonPacked`.
- `collSortMaps` uses `{key: 'prop', order: 'asc'}` syntax, which differs from APOC.
- **No schema management at all**, and no native vector or full-text index -- those operations and
  `loadNearest`/`loadMatching` throw `UnsupportedOperationException`.

### Memgraph

In-memory, Bolt-compatible, Neo4j-flavoured Cypher. Drivine reuses the Neo4j driver stack; only the
dialect differs. Port 7687. Empty credentials work by default.

Full ACID (`startTransaction` / `commit` / `rollback` all behave), `EXISTS { pattern }` and nested
pattern comprehensions work, so `@GraphView` queries use the same inline projector as Neo4j. No
APOC -- use MAGE for procedures, and collection sorting goes through `CALL` subqueries by default.
`text_search` is GA, so full-text needs no experimental flag. Switch the image to
`memgraph/memgraph-platform` for MAGE algorithms or Memgraph Lab.

## Testing

`@EnableDrivine` + `@EnableDrivineTestConfig` over datasources declared in `application-test.yml`
gives one configuration for both local development and CI:

```kotlin
@Configuration
@EnableDrivine
@EnableDrivineTestConfig
class TestConfig
```

| Mode | Trigger | Behaviour |
|------|---------|-----------|
| Local dev | `USE_LOCAL_NEO4J=true` | uses your `application-test.yml` settings as-is against your running Neo4j -- fast, and inspectable after a failure with `@Rollback(false)` |
| CI (default) | `USE_LOCAL_NEO4J` unset/false | starts a Neo4j Testcontainer and overrides host/port/password from the properties -- isolated |

For manual control, `DrivineTestContainer` is a `Neo4jContainer` subclass whose companion exposes
`getConnectionUrl()`, `getConnectionUsername()`, `getConnectionPassword()` and `useLocalNeo4j()`;
`FalkorDbTestContainer` and `MemgraphTestContainer` cover the other two engines, with
`FalkorDbCleanupListener` / `TestCleanup` handling per-engine teardown between tests.

```kotlin
@Configuration
@EnableDrivine
class TestConfig {
    @Bean
    fun dataSourceMap(): DataSourceMap {
        val url = DrivineTestContainer.getConnectionUrl()
        return DataSourceMap(mapOf("neo" to ConnectionProperties(
            type = DatabaseType.NEO4J,
            host = url.substringAfter("bolt://").substringBefore(":"),
            port = url.substringAfter("bolt://").substringAfter(":").toIntOrNull() ?: 7687,
            userName = DrivineTestContainer.getConnectionUsername(),
            password = DrivineTestContainer.getConnectionPassword(),
            databaseName = "neo4j",
        )))
    }
}
```

Mount server-side Neo4j plugins (APOC Extended) into the container rather than adding them as Java
dependencies -- the build resolves them through a dedicated `neo4jPlugins` configuration and mounts
them into the container's plugins directory.

## See Also

- [SKILL.md](../SKILL.md) -- version pinning, requirements, API choice
- [PersistenceManager reference](persistence-manager.md) -- manual Cypher, capability flags
- [GraphObjectManager reference](graph-object-manager.md) -- annotations and DSL operators
- [Schema and search reference](schema-and-search.md) -- indexes, vector/full-text, keyset
