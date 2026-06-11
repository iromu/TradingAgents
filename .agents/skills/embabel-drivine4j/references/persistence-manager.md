# PersistenceManager — Low-Level Cypher API Reference

## Overview

`PersistenceManager` is Drivine4j's low-level API for executing manual Cypher queries. It gives full control over query
construction, parameter binding, and result transformation.

## Core Types

### QuerySpecification

The builder for Cypher queries:

```kotlin
QuerySpecification
    .withStatement("MATCH (p:Person) RETURN properties(p)")
    .bind(mapOf("city" to "London"))
    .transform(Person::class.java)
```

| Method                       | Purpose                                              |
|------------------------------|------------------------------------------------------|
| `withStatement(String)`      | The Cypher query string                              |
| `bind(Map<String, Any>)`     | Bind named parameters                                |
| `bindObject(String, Any)`    | Bind an object as a map (e.g., for `SET p = $props`) |
| `transform(Class<T>)`        | Transform results to a Java/Kotlin type              |
| `withDialect(CypherDialect)` | Override the default dialect for this query          |

### PersistenceManager

Injected via `@Qualifier`:

```kotlin
@Autowired
@Qualifier("neoManager")
val manager: PersistenceManager
```

### Key Methods

| Method                        | Returns           | Description                                       |
|-------------------------------|-------------------|---------------------------------------------------|
| `query(QuerySpecification)`   | `List<T>`         | Execute and return all results                    |
| `getOne(QuerySpecification)`  | `T`               | Execute and return single result (throws if none) |
| `execute(QuerySpecification)` | `ExecutionResult` | Execute without transformation (raw access)       |

## Transaction Management

Use Spring's `@Transactional` annotation:

```kotlin
@Transactional
fun createAndLink(person: Person, organization: Organization) {
    // Create person
    manager.getOne(
        QuerySpecification
            .withStatement("CREATE (p:Person) SET p = \$props RETURN properties(p)")
            .bindObject("props", person)
            .transform(Person::class.java)
    )

    // Link to organization
    manager.getOne(
        QuerySpecification
            .withStatement("MATCH (p:Person {uuid: \$uuid}), (o:Organization {uuid: \$orgUuid}) CREATE (p)-[:WORKS_FOR]->(o)")
            .bind(mapOf("uuid" to person.uuid, "orgUuid" to organization.uuid))
    )
}
```

## Batch Operations

For bulk operations, reuse the same `QuerySpecification` pattern:

```kotlin
@Transactional
fun createAll(people: List<Person>): List<Person> {
    return people.map { person ->
        manager.getOne(
            QuerySpecification
                .withStatement("CREATE (p:Person) SET p = \$props RETURN properties(p)")
                .bindObject("props", person)
                .transform(Person::class.java)
        )
    }
}
```

For very large batches, consider using the Neo4j native batch API or Drivine's batch helpers to avoid N+1 query
patterns.

## Result Transformation

### Simple Transform

```kotlin
.transform(Person::class.java)  // Maps result map to Person
```

### Custom Transform

For complex transformations, use the raw `ExecutionResult`:

```kotlin
val result = manager.execute(querySpec)
return result.records.map { record ->
    val props = record["p"].asMap()
    Person(props["uuid"] as String, props["name"] as String)
}
```

## Cypher Dialects

| Dialect             | Database       |
|---------------------|----------------|
| `NEO4J_5` (default) | Neo4j 5.x      |
| `NEO4J_4`           | Neo4j 4.x      |
| `FALKORDB`          | FalkorDB       |
| `NEPTUNE`           | Amazon Neptune |
| `MEMGRAPH`          | Memgraph       |

Override per-query:

```kotlin
QuerySpecification
    .withStatement(query)
    .transform(Person::class.java)
    .withDialect(CypherDialect.NEO4J_4)
```

## RETURN Clause Rules

**Always return a single map or scalar value:**

```cypher
-- CORRECT
RETURN { name: a.name, age: a.age } AS result
RETURN properties(p)
RETURN count(*) AS total

-- WRONG (multiple columns)
MATCH (a:Actor), (b:Director) RETURN a.name, b.name
```

## See Also

- [GraphObjectManager reference](graph-object-manager.md) — high-level annotated model API
- [Multi-database configuration](multi-db.md) — dialects and connection setup
