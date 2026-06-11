---
name: embabel-drivine4j
description: >-
  Build type-safe graph database clients with Drivine4j for Neo4j, FalkorDB, Amazon Neptune, and Memgraph.
  Use when the user wants to set up a Drivine4j client, create annotated graph models, write Cypher queries,
  configure graph database connections, or integrate Drivine4j with DICE knowledge graphs.
  Also trigger on mentions of Drivine4j, graph database in Java/Kotlin, or Cypher queries.
---

# Drivine4j — Type-Safe Graph Database Client

A graph database client library for Java and Kotlin supporting **Neo4j**, **FalkorDB**, **Amazon Neptune**, and **Memgraph** with two complementary APIs:

| API | Level | Cypher | Best for |
|-----|-------|--------|----------|
| **PersistenceManager** | Low-level | Manual, full control | Complex queries, migrations, graph algorithms |
| **GraphObjectManager** | High-level | Auto-generated from annotations | CRUD, domain models, type-safe DSL |

## Output Quality

When producing code or documentation:

- **Be comprehensive** — Provide complete, working examples with full imports and annotations, not partial snippets
- **Show composition** — Demonstrate `@GraphView` composition patterns (not monolithic models)
- **Include configuration** — Always show the `DataSourceMap` bean setup alongside repository code
- **Use correct API** — PersistenceManager for manual Cypher; GraphObjectManager for annotated models. Do not mix them inappropriately.
- **Reference the right doc** — Point to `references/graph-object-manager.md` for annotation details and DSL operators; `references/persistence-manager.md` for batch/transaction patterns; `references/multi-db.md` for dialects and multi-database setup

## Philosophy: Composition Over Inheritance

Traditional ORMs generate queries from a single object model, which degrades performance on complex queries and becomes unwieldy for different use cases. Graph databases don't have this impedance mismatch — you compose views as needed:

```kotlin
@GraphView
data class HolidayingPerson(
    @Root val person: Person,
    @GraphRelationship(type = "BOOKED_HOLIDAY")
    val holidays: List<Holiday>
)
```

Behind the scenes, Drivine generates efficient Cypher. Composition lets you mix and match roles (a person can be a holiday-maker, an employee, a person of interest) without a monolithic model.

## Requirements

- **Java 21+**
- **Kotlin 2.2.0+** for GraphObjectManager API (requires `-Xcontext-parameters`)
- Any Kotlin version for PersistenceManager API

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("org.drivine:drivine4j:0.0.30")
}
```

### Code Generation for Type-Safe DSL (GraphObjectManager only)

If you want the type-safe query DSL auto-generated from `@GraphView` classes:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
    kotlin("jvm") version "2.2.0"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters")
    }
}

dependencies {
    implementation("org.drivine:drivine4j:0.0.30")
    ksp("org.drivine:drivine4j-codegen:0.0.30")
}
```

> **Pitfall:** Without `-Xcontext-parameters` and the KSP dependency, the type-safe DSL will not generate. The `@GraphView` annotations still work, but DSL methods like `where { ... }` won't be available.

## Configuration

```kotlin
@Configuration
@ComponentScan("org.drivine")
class AppConfig {
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
}
```

See `references/multi-db.md` for multi-database connections, FalkorDB/Neptune/Memgraph setup, and Cypher dialect configuration.

## Choose Your API

### Use PersistenceManager when:
- You need full Cypher control (complex graph algorithms, custom queries)
- You're running migrations or one-off data operations
- You're working with an existing Cypher codebase

### Use GraphObjectManager when:
- You have a stable domain model and want annotation-driven CRUD
- You want type-safe filtering and ordering via DSL
- You need dirty tracking for efficient updates
- You want cascade save/delete of related objects

## PersistenceManager — Quick Start

Inject via qualifier and execute manual Cypher:

```kotlin
@Component
class PersonRepository @Autowired constructor(
    @Qualifier("neoManager") val manager: PersistenceManager
) {
    @Transactional
    fun findByCity(city: String): List<Person> {
        return manager.query(
            QuerySpecification
                .withStatement("MATCH (p:Person {city: \$city}) RETURN properties(p)")
                .bind(mapOf("city" to city))
                .transform(Person::class.java)
        )
    }
}
```

**Key rules:**
- Always wrap multiple RETURN values in a single map: `RETURN { name: a.name, age: a.age } AS result`
- Use `@Transactional` for write operations
- See `references/persistence-manager.md` for batch operations, transaction management, and custom result transformation

## GraphObjectManager — Quick Start

### Define annotated models

```kotlin
@NodeFragment
data class Person(
    @NodeId val uuid: String,
    val name: String,
    val bio: String?
)

@GraphView
data class PersonCareer(
    @Root val person: Person,
    @GraphRelationship(type = "WORKS_FOR")
    val employmentHistory: List<WorkHistory>
)
```

### Load, filter, and save

```kotlin
// Load all / by ID / count
graphObjectManager.loadAll<PersonCareer>()
graphObjectManager.load<PersonCareer>(uuid)
graphObjectManager.count(Issue::class.java)

// Type-safe DSL filtering
graphObjectManager.loadAll<PersonCareer> {
    where { person.bio contains "Lead" }
}

// OR conditions
graphObjectManager.loadAll<PersonCareer> {
    where {
        anyOf {
            person.name eq "Alice"
            person.name eq "Bob"
        }
    }
}

// Save with dirty tracking
val person = graphObjectManager.loadOrThrow<PersonCareer>(uuid)
val updated = person.copy(person = person.person.copy(bio = "Updated bio"))
graphObjectManager.save(updated)  // Only dirty fields written
```

**Key rules:**
- Exactly one `@Root` per `@GraphView`
- `@NodeId` must be `String` or `UUID`
- Dirty tracking only works on objects loaded within the same manager context
- Use `@GraphRelationship(cascade = ...)` to control save/delete behavior of related objects

See `references/graph-object-manager.md` for the full annotation reference, DSL operators, polymorphic relationships, recursive hierarchies, and cascade policies.

## Integration with DICE and Embabel Agents

Drivine4j is used by DICE for Neo4j projections — DICE propositions can be materialized as a graph in Neo4j via Drivine4j's `@GraphView` models. This gives agents graph-traversal capabilities over their knowledge base.

To integrate:
1. Define `@GraphView` models that represent your DICE proposition schema
2. Use `GraphObjectManager` to read/write propositions as graph objects
3. Query the graph for relationship-based recall (e.g., "what entities is Alice connected to?")

See the DICE skill for how propositions project to Neo4j and how agents consume graph-structured knowledge.

## Common Pitfalls

| Pitfall | Fix |
|---------|-----|
| Multiple RETURN columns in Cypher | Wrap in single map: `RETURN { ... } AS result` |
| Missing `-Xcontext-parameters` compiler flag | Add to `freeCompilerArgs` when using GraphObjectManager DSL |
| Kotlin version < 2.2.0 with GraphObjectManager | Use PersistenceManager, or upgrade Kotlin |
| Dirty tracking on detached objects | Load within the same manager context before saving |
| Using `@RelationshipCascadeType` for cascade | Always use `@GraphRelationship(cascade = ...)` — the other annotation does not work |
| Cypher dialect mismatch | Ensure dialect matches database version (NEO4J_4 vs NEO4J_5); see `references/multi-db.md` |

## See Also

- [PersistenceManager reference](references/persistence-manager.md) — low-level Cypher API, batch operations, transactions, result transformation
- [GraphObjectManager reference](references/graph-object-manager.md) — full annotation reference, DSL operators, cascade policies, polymorphic/recursive relationships
- [Multi-database configuration](references/multi-db.md) — connection setup, dialects, FalkorDB/Neptune/Memgraph, testing with Testcontainers
