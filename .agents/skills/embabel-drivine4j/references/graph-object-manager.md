# GraphObjectManager — High-Level Annotated Model API Reference

## Overview

`GraphObjectManager` provides a high-level API for working with graph-mapped objects using annotated models. It generates efficient Cypher queries automatically and provides a type-safe DSL for filtering and ordering.

## Annotations

### @NodeFragment

Maps a data class to a graph node.

```kotlin
@NodeFragment(labels = ["Person"])
data class Person(
    @NodeId val uuid: String,    // Required: unique identifier
    val name: String,
    val bio: String?
)
```

| Attribute | Default | Description |
|-----------|---------|-------------|
| `labels` | `["ClassName"]` | Neo4j labels for the node |

**Required properties on `@NodeId`:**
- Must be a `String` or `UUID`
- Used to identify nodes for updates and deletions
- Drivine uses this to generate `MATCH (n {uuid: $uuid})` clauses

### @GraphView

Composes a graph view from multiple fragments and relationships.

```kotlin
@GraphView
data class PersonCareer(
    @Root val person: Person,  // Exactly one @Root required
    @GraphRelationship(type = "WORKS_FOR")
    val employmentHistory: List<WorkHistory>
)
```

| Attribute | Default | Description |
|-----------|---------|-------------|
| `name` | class name | View name (used for DSL generation) |

**Rules:**
- Exactly one field must be annotated with `@Root`
- Other fields use `@GraphRelationship` to define edges
- Collections are supported for multi-edge relationships

### @Root

Marks the root node of a `@GraphView`. Exactly one per view.

### @GraphRelationship

Defines a relationship edge in a `@GraphView`.

```kotlin
@GraphRelationship(
    type = "WORKS_FOR",
    direction = Direction.OUTGOING,
    cascade = CascadeType.NONE
)
val employmentHistory: List<WorkHistory>
```

| Attribute | Default | Description |
|-----------|---------|-------------|
| `type` | required | Cypher relationship type |
| `direction` | `Direction.OUTGOING` | `INCOMING` or `OUTGOING` |
| `cascade` | `NONE` | Delete cascade policy |
| `maxDepth` | 1 | For recursive relationships |

### @RelationshipFragment

Captures properties on relationship edges (not just target nodes):

```kotlin
@RelationshipFragment
data class WorkHistory(
    val startDate: LocalDate,  // Edge property
    val role: String,           // Edge property
    val target: Organization    // Target node
)
```

### @GraphPath

Traverses multiple hops, mapping only the final node:

```kotlin
@GraphPath([
    Hop("ACTED_IN",    Direction.OUTGOING, label = "Movie"),  // through Movie
    Hop("DIRECTED_BY", Direction.OUTGOING),                   // to Director
])
val directors: List<Director>
```

### @Count

Adds a count aggregate per root node:

```kotlin
@Count("ACTED_IN")  // Count outgoing ACTED_IN relationships
val movieCount: Long
```

### @Aggregate

Adds a computed aggregate per root node:

```kotlin
@Aggregate(AggregateFunction.AVG, type = "RATED", property = "score")
val avgRating: Double
```

| AggregateFunction | Description |
|-------------------|-------------|
| `AVG` | Average |
| `SUM` | Sum |
| `MIN` | Minimum |
| `MAX` | Maximum |
| `COUNT` | Count (alias for `@Count`) |

### @Default

Provides a fallback value when a property is missing or null:

```kotlin
@Default val status: String = "active"  // missing → "active"
```

### @EmptyWhenAbsent

Maps absent or null collections/maps to empty:

```kotlin
@EmptyWhenAbsent val tags: List<String>  // missing → []
```

## Type-Safe DSL

The KSP code generator creates a DSL for each `@GraphView`. Available operators:

### Comparison

| Operator | Generated Cypher |
|----------|------------------|
| `eq` | `= $value` |
| `ne` | `<> $value` |
| `gt`, `gte` | `> / >= $value` |
| `lt`, `lte` | `< / <= $value` |
| `contains` | `=~ $regex` |
| `in` | `IN $values` |
| `isNotNull` | `IS NOT NULL` |
| `isNull` | `IS NULL` |

### Boolean

| Operator | Generated Cypher |
|----------|------------------|
| `anyOf { ... }` | `(condition1) OR (condition2)` |
| implicit AND | `(condition1) AND (condition2)` |

### Ordering

| Operator | Generated Cypher |
|----------|------------------|
| `.asc()` | `ORDER BY field ASC` |
| `.desc()` | `ORDER BY field DESC` |

### DSL Usage

```kotlin
// Single condition
graphObjectManager.loadAll<PersonCareer> {
    where { person.name eq "Alice" }
}

// AND (implicit)
graphObjectManager.loadAll<PersonCareer> {
    where {
        person.name eq "Alice"
        person.bio.isNotNull()
    }
}

// OR
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
    where { person.name startsWith "A" }
    orderBy { person.name.asc() }
}
```

## Loading and Saving

### Loading

```kotlin
// Load all matching
graphObjectManager.loadAll<PersonCareer>()
graphObjectManager.loadAll<PersonCareer> { where { ... } }

// Load by ID
graphObjectManager.load<PersonCareer>(uuid)
graphObjectManager.load<PersonCareer>(uuid) { where { ... } }

// Load or throw
graphObjectManager.loadOrThrow<PersonCareer>(uuid)

// Count
graphObjectManager.count(Issue::class.java)
```

### Saving (Dirty Tracking)

```kotlin
val person = graphObjectManager.loadOrThrow<PersonCareer>(uuid)
val updated = person.copy(person = person.person.copy(bio = "Updated bio"))
graphObjectManager.save(updated)  // Only dirty fields written
```

**Important:** Dirty tracking only works on objects loaded within the same manager context. Detached objects will not be tracked.

### Deleting

```kotlin
graphObjectManager.delete(personCareer)
```

Delete behavior follows the `cascade` policy on `@GraphRelationship`:

| Cascade Policy | Behavior |
|----------------|----------|
| `NONE` (default) | Only deletes the relationship, leaves target nodes intact |
| `DELETE_ORPHAN` | Deletes relationship and target only if no other relationships exist to the target |
| `DELETE_ALL` | Always deletes both the relationship and target nodes |

## Polymorphic Relationships

Drivine supports sealed classes with label-based type discrimination:

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

@NodeFragment(labels = ["WebUser", "Authenticated"])
data class AuthenticatedWebUser(
    override val uuid: UUID,
    override val displayName: String,
    val email: String
)
```

Drivine uses the label set to discriminate between subtypes when loading.

## Recursive Relationships

Self-referential `@GraphView` classes expand to a configurable depth:

```kotlin
@GraphView
data class LocationHierarchy(
    val location: Location,
    @GraphRelationship(type = "HAS_LOCATION", direction = Direction.OUTGOING, maxDepth = 3)
    val subLocations: List<LocationHierarchy>
)
```

`maxDepth = 3` means 3 levels of expansion. Cycle detection prevents infinite loops.

## See Also

- [PersistenceManager reference](persistence-manager.md) — low-level Cypher API
- [Multi-database configuration](multi-db.md) — dialects and connection setup
