# Multi-Database Configuration Reference

## Table of Contents

1. [Supported Databases](#supported-databases)
2. [Connection Configuration](#connection-configuration)
3. [Cypher Dialects](#cypher-dialects)
4. [Database-Specific Notes](#database-specific-notes)
5. [Testing](#testing)

## Supported Databases

| Database | Version | Dialect |
|----------|---------|---------|
| **Neo4j** | 5.x, 4.x | `NEO4J_5` (default), `NEO4J_4` |
| **FalkorDB** | Latest | `FALKORDB` |
| **Amazon Neptune** | Latest | `NEPTUNE` |
| **Memgraph** | Latest | `MEMGRAPH` |

## Connection Configuration

### Single Database

```kotlin
@Bean
fun dataSourceMap(): DataSourceMap {
    val props = ConnectionProperties(
        host = "localhost",
        port = 7687,
        username = "neo4j",
        password = "password",
        database = "neo4j"
    )
    return DataSourceMap(mapOf("neo" to props))
}
```

### Multiple Databases

```kotlin
@Bean
fun dataSourceMap(): DataSourceMap {
    return DataSourceMap(mapOf(
        "neo" to ConnectionProperties("localhost", 7687, "neo4j", "password", "neo4j"),
        "falkor" to ConnectionProperties("localhost", 6379, null, null, "falkor"),
        "neptune" to ConnectionProperties("neptune-cluster.region.amazonaws.com", 8182, "neptune-user", "neptune-pass", "neptune")
    ))
}
```

Each key becomes a qualifier for injection:

```kotlin
@Autowired
@Qualifier("neoManager")
val neoManager: PersistenceManager

@Autowired
@Qualifier("falkorManager")
val falkorManager: PersistenceManager
```

## Cypher Dialects

The dialect controls engine-specific Cypher generation — existence checks, collection sorting, and nested view projections.

### Global Default

Set in configuration to pick the right dialect for your database version.

### Per-Query Override

```kotlin
QuerySpecification
    .withStatement(query)
    .transform(Person::class.java)
    .withDialect(CypherDialect.NEO4J_4)
```

### Dialect Differences

| Feature | NEO4J_5 | NEO4J_4 | FALKORDB | NEPTUNE | MEMGRAPH |
|---------|---------|---------|----------|---------|----------|
| Existence checks | `IS NOT NULL` | `IS NOT NULL` | Varies | Varies | Varies |
| Collection sorting | Built-in | Workarounds | Native | Workarounds | Native |
| Nested views | Full support | Limited | Native | Limited | Native |

## Database-Specific Notes

### Neo4j

- Supports both Bolt (port 7687) and HTTP (port 7474)
- Bolt is recommended for `PersistenceManager`
- Version 5.x features: improved security, new syntax, better performance

### FalkorDB

- In-memory graph database with Redis-like interface
- Uses port 6379 by default
- Supports Cypher via its graph extension

### Amazon Neptune

- Managed graph database service
- Uses port 8182 (HTTPS)
- Requires IAM authentication for some configurations
- Neptune's Cypher dialect has some differences from Neo4j

### Memgraph

- In-memory graph database compatible with Neo4j's Cypher
- Uses port 7687 (Bolt) or 7688 (HTTP)
- Mostly compatible with Neo4j 4.x syntax

## Testing

For tests, use an in-memory or embedded database:

```kotlin
// Test configuration
@TestConfiguration
class TestConfig {
    @Bean
    fun testDataSourceMap(): DataSourceMap {
        // Use an embedded or test-specific database
        val props = ConnectionProperties(
            host = "localhost",
            port = 7687,
            username = "neo4j",
            password = "test-password",
            database = "test-neo4j"
        )
        return DataSourceMap(mapOf("test" to props))
    }
}
```

Consider using [Testcontainers](https://testcontainers.com/) for Neo4j in integration tests:

```kotlin
val neo4j = Neo4jContainer("neo4j:5")
    .withExposedPorts(7687)
    .withPassword("test-password")

@BeforeAll
fun setup() {
    neo4j.start()
    // Configure DataSourceMap with neo4j.getHost() and neo4j.getMappedPort(7687)
}

@AfterAll
fun teardown() {
    neo4j.stop()
}
```

## See Also

- [SKILL.md](../SKILL.md) — Core Drivine4j concepts, setup, and quick examples
- [PersistenceManager reference](persistence-manager.md) — low-level Cypher API
- [GraphObjectManager reference](graph-object-manager.md) — high-level annotated model API
