---
name: embabel-drivine4j
description: Build type-safe graph database clients with Drivine4j for Neo4j, FalkorDB, Amazon Neptune, and Memgraph. Use this skill whenever the user wants to set up a graph database client with Drivine4j, create annotated graph models with @GraphView and @NodeFragment, write low-level Cypher queries with PersistenceManager, use the high-level GraphObjectManager API with type-safe DSL, configure multi-database connections, or integrate Drivine4j with DICE knowledge graphs or Embabel agents. Also use when the user mentions Drivine4j, graph database in Java/Kotlin, Cypher queries, Neo4j client, graph mapping, @GraphView, @NodeFragment, @RelationshipFragment, or composition over inheritance for graph data.
---

# Drivine4j — Type-Safe Graph Database Client

A graph database client library for Java and Kotlin supporting **Neo4j**, **FalkorDB**, **Amazon Neptune**, and *
*Memgraph** with two approaches to graph mapping:

1. **PersistenceManager** — low-level API with manual Cypher queries (full control)
2. **GraphObjectManager** — high-level API with annotated models and type-safe DSL (convenience)

## Philosophy: Composition Over Inheritance

Traditional ORMs generate queries from a single object model, which degrades performance on complex queries and becomes
unwieldy for different use cases. Graph databases don't have this impedance mismatch — you compose views as needed:

```kotlin
@GraphView
data class HolidayingPerson(
    @Root val person: Person,
    @GraphRelationship(type = "BOOKED_HOLIDAY")
    val holidays: List<Holiday>
)
```

Behind the scenes, Drivine generates efficient Cypher. Composition lets you mix and match roles (a person can be a
holiday-maker, an employee, a person of interest) without a monolithic model.

## Requirements

- **Java 21+**
- **Kotlin 2.2.0+** for GraphObjectManager API (requires context parameters)
- Any Kotlin version for PersistenceManager API

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("org.drivine:drivine4j:0.0.30")
}
```

### Code Generation for Type-Safe DSL

If you want the type-safe query DSL (auto-generated from `@GraphView` classes):

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

See `references/multi-db.md` for multi-database configuration, dialects, and FalkorDB/Neptune/Memgraph setup.

## Two APIs

| Aspect      | PersistenceManager          | GraphObjectManager                                  |
|-------------|-----------------------------|-----------------------------------------------------|
| Level       | Low-level                   | High-level                                          |
| Cypher      | Manual, full control        | Auto-generated from annotations                     |
| Best for    | Complex queries, migrations | CRUD, domain models, type-safe DSL                  |
| Annotations | Not required                | `@NodeFragment`, `@GraphView`, `@GraphRelationship` |
| Code gen    | Not needed                  | Optional KSP for type-safe DSL                      |

### When to use which

- **PersistenceManager** when you need full Cypher control, complex graph algorithms, or one-off queries
- **GraphObjectManager** when you have a stable domain model and want type-safe, annotation-driven CRUD with dirty
  tracking

## PersistenceManager — Low-Level Cypher API

Use `PersistenceManager` when you need explicit Cypher queries.

### Basic Query

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

    @Transactional
    fun create(person: Person): Person {
        return manager.getOne(
            QuerySpecification
                .withStatement("CREATE (p:Person) SET p = \$props RETURN properties(p)")
                .bindObject("props", person)
                .transform(Person::class.java)
        )
    }
}
```

### RETURN Clause Best Practices

Always return a **single map** or **scalar value** — never multiple columns:

```cypher
-- CORRECT: Single map
RETURN { name: a.name, age: a.age, title: b.title } AS result

-- CORRECT: Single property map
RETURN properties(p)

-- WRONG: Multiple columns
MATCH (a:Actor), (b:Director) RETURN a.name, b.name
```

See `references/persistence-manager.md` for advanced patterns: batch operations, transaction management, and result
transformation.

## GraphObjectManager — High-Level Annotated Model API

`GraphObjectManager` generates Cypher from annotated models and provides a type-safe DSL for filtering and ordering.

### NodeFragment — Mapping Nodes

```kotlin
@NodeFragment
data class Person(
    @NodeId val uuid: String,
    val name: String,
    val bio: String?
)
```

**Defaults for missing properties:**

```kotlin
@NodeFragment(labels = ["User"])
data class UserNode(
    @NodeId val id: String,
    @Default val roles: List<String> = emptyList(),  // missing/null → []
    @Default val status: String = "active",          // missing/null → "active"
    @EmptyWhenAbsent val tags: List<String>,         // missing/null → []
)
```

- **`@Default`** — falls back to the property's declared default value
- **`@EmptyWhenAbsent`** — maps absent/null collections/maps to empty (no default needed)

### RelationshipFragment — Capturing Edge Properties

```kotlin
@RelationshipFragment
data class WorkHistory(
    val startDate: LocalDate,  // Property on the edge
    val role: String,           // Property on the edge
    val target: Organization    // Target node
)
```

### GraphView — Composing Views

A `@GraphView` composes multiple fragments and relationships into a single query result:

```kotlin
@GraphView
data class PersonCareer(
    @Root val person: Person,  // Root fragment
    @GraphRelationship(type = "WORKS_FOR")
    val employmentHistory: List<WorkHistory>
)
```

### Advanced GraphView Features

**Recursive relationships** (hierarchies, ontologies):

```kotlin
@GraphView
data class LocationHierarchy(
    val location: Location,
    @GraphRelationship(type = "HAS_LOCATION", direction = Direction.OUTGOING, maxDepth = 3)
    val subLocations: List<LocationHierarchy>  // Self-referential
)
```

**Path traversal** (skip intermediary nodes):

```kotlin
@GraphView
data class ActorDirectors(
    @Root val actor: Actor,
    @GraphPath([
        Hop("ACTED_IN",    Direction.OUTGOING, label = "Movie"),  // through Movie
        Hop("DIRECTED_BY", Direction.OUTGOING),                   // to Director
    ])
    val directors: List<Director>
)
```

**Aggregates** (count & summarize without loading):

```kotlin
@GraphView
data class ActorStats(
    @Root val actor: Actor,
    @Count("ACTED_IN")                                              val movieCount: Long,
    @Aggregate(AggregateFunction.AVG, type = "RATED", property = "score") val avgRating: Double,
)
```

### Loading, Filtering, and Saving

```kotlin
// Load all / by ID / count
graphObjectManager.loadAll<PersonCareer>()
graphObjectManager.load<PersonCareer>(uuid)
graphObjectManager.count(Issue::class.java)

// Type-safe DSL filtering
graphObjectManager.loadAll<PersonCareer> {
    where { person.bio contains "Lead" }
}

// Multiple conditions (AND)
graphObjectManager.loadAll<PersonCareer> {
    where {
        person.name eq "Alice"
        person.bio.isNotNull()
    }
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

// Ordering
graphObjectManager.loadAll<PersonCareer> {
    orderBy { person.name.asc() }
}

// Saving with dirty tracking
val person = graphObjectManager.loadOrThrow<PersonCareer>(uuid)
val updated = person.copy(person = person.person.copy(bio = "Updated bio"))
graphObjectManager.save(updated)  // Only dirty fields written
```

### CASCADE Policies

| Policy           | Behavior                                                                           |
|------------------|------------------------------------------------------------------------------------|
| `NONE` (default) | Only deletes the relationship, leaves target nodes intact                          |
| `DELETE_ORPHAN`  | Deletes relationship and target only if no other relationships exist to the target |
| `DELETE_ALL`     | Always deletes both the relationship and target nodes (destructive)                |

### Polymorphic Relationships

```kotlin
@NodeFragment(labels = ["WebUser"])
sealed class WebUser {
    abstract val uuid: UUID
    abstract val displayName: String
}

@NodeFragment(labels = ["WebUser", "Anonymous"])
data class AnonymousWebUser(
    override val uuid: UUID,
    override val displayName: String,
    val anonymousToken: String
)
```

See `references/graph-object-manager.md` for the full API reference including all annotations, DSL operators, and
advanced patterns.

## Integration with DICE and Embabel Agents

Drivine4j is used by DICE for Neo4j projections — DICE propositions can be materialized as a graph in Neo4j via
Drivine4j's `@GraphView` models. This gives agents graph-traversal capabilities over their knowledge base.

To integrate:

1. Define `@GraphView` models that represent your DICE proposition schema
2. Use `GraphObjectManager` to read/write propositions as graph objects
3. Query the graph for relationship-based recall (e.g., "what entities is Alice connected to?")

See the DICE skill for how propositions project to Neo4j and how agents consume graph-structured knowledge.

## Common Pitfalls

- **Multiple RETURN columns** — Always wrap multiple values in a single map (`RETURN { ... } AS result`)
- **Missing `-Xcontext-parameters`** — GraphObjectManager with KSP requires this compiler flag or the DSL won't generate
- **Kotlin version mismatch** — GraphObjectManager needs Kotlin 2.2.0+; PersistenceManager works with any Kotlin version
- **Dirty tracking on detached objects** — `GraphObjectManager.save()` only works on objects loaded within the same
  manager context
- **Cypher dialect mismatch** — Ensure the dialect matches your database version (e.g., `NEO4J_4` vs `NEO4J_5`); see
  `references/multi-db.md`
