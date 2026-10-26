# PersistenceManager -- Low-Level Cypher API

## Table of Contents

1. [Overview](#overview)
2. [Interface Surface](#interface-surface)
3. [QuerySpecification](#queryspecification)
4. [Transactions and Batching](#transactions-and-batching)
5. [Result Mapping and Subtypes](#result-mapping-and-subtypes)
6. [Capability Flags](#capability-flags)
7. [Native Driver Access](#native-driver-access)
8. [RETURN Clause Rules](#return-clause-rules)

## Overview

`PersistenceManager` executes hand-written Cypher: you own the statement, the parameter bindings
and the mapping. Reach for it over `GraphObjectManager` for graph algorithms, migrations, bulk
one-off operations, or an existing Cypher codebase.

Managers come from `PersistenceManagerFactory.get(database, type)` keyed by a `DataSourceMap`
name (`"default"` = first registered). `PersistenceManagerType` selects `DELEGATING`,
transactional or non-transactional behaviour; `TransactionalPersistenceManager` and
`NonTransactionalPersistenceManager` are the two concrete halves and
`DelegatingPersistenceManager` routes between them.

## Interface Surface

```kotlin
interface PersistenceManager {
    val database: String
    val type: DatabaseType
    val grammar: CypherGrammar
    val indexes: IndexManager
    val constraints: ConstraintManager
    val supportsSchemaManagement: Boolean      // default true

    fun <T : Any> query(spec: QuerySpecification<T>): List<T>
    fun <T : Any> getOne(spec: QuerySpecification<T>): T
    fun <T : Any> maybeGetOne(spec: QuerySpecification<T>): T?
    fun <T : Any> optionalGetOne(spec: QuerySpecification<T>): Optional<T>
    fun execute(spec: QuerySpecification<*>)
    fun executeBatch(specs: List<QuerySpecification<*>>)
    fun registerSubtype(baseClass: Class<*>, labels: List<String>, subClass: Class<*>)
}
```

`getOne` throws when the count is not exactly one; `maybeGetOne` accepts zero or one;
`optionalGetOne` wraps that in an `Optional`. `execute` returns **`Unit`** -- it is for writes and
for statements whose rows you do not read. There is no `ExecutionResult` type and no
`result.records` to walk: to read rows, use `query`/`getOne`/`maybeGetOne` with `.transform(...)`,
or `.map { }` / `.mapWith(RowMapper)` on the spec.

## QuerySpecification

```kotlin
QuerySpecification
    .withStatement("MATCH (p:Person {city: \$city}) RETURN properties(p)")  // String or Statement
    .bind(mapOf("city" to city))
    .transform(Person::class.java)
```

| Method | Purpose |
|--------|---------|
| `withStatement(String \| Statement)` | start the builder (also accepts a loaded `Statement`) |
| `bind(Map<String, Any?>)` | named parameter bindings |
| `bindObject(key, value)` | serialize one object to a Neo4j-compatible map via `Neo4jObjectMapper` |
| `render(params)` / `renderParam(key, value)` | inline a value into the statement text (not a binding) |
| `transform(Class<U>)` | map result rows to a target type |
| `map((T) -> U)` | post-process each mapped result |
| `mapWith(RowMapper<U>)` | map raw rows yourself |
| `filter((T) -> Boolean)` | client-side predicate |
| `filterIsInstance(Class<U>)` | client-side type narrowing |
| `limit(n)` / `skip(n)` | row bounds |
| `addPostProcessors(vararg ResultPostProcessor)` / `addParameterCoercers(vararg ParameterCoercer)` | extension hooks |

There is **no** `withDialect(...)` on the builder. Dialect selection is a property of the
datasource: set `ConnectionProperties.cypherDialect`, or supply it through
`ConnectionProviderBuilder.cypherDialect(...)`. See `references/multi-db.md`.

`bindObject` goes through the Neo4j `ObjectMapper`, which converts `Enum` and `UUID` to `String`,
`Instant`/`Date` to `ZonedDateTime`, ignores unknown properties on read, and **includes nulls by
default** so a bound map can carry explicit nulls. To suppress nulls per property, annotate with
`@JsonInclude(JsonInclude.Include.NON_NULL)`. Note this governs low-level binding only -- whether a
null clears a property on a `GraphObjectManager` save is `NullPolicy`'s decision.

## Transactions and Batching

`@Transactional` wraps the work on the transactional manager. For an atomic group of statements
without Spring, `executeBatch(specs)` runs them together in one transaction on one connection --
fewer round trips than N `execute` calls, and the real managers override it to provide the
guarantee (the interface default just loops `execute`).

```kotlin
manager.executeBatch(listOf(createPersonSpec, linkToOrgSpec))
```

Engine caveat: FalkorDB has no multi-statement transactions. `FalkorDbTransactionMode` decides how
loudly that is reported -- `WARN` (the default: `startTransaction`/`commit` are no-ops, rollback
warns because writes already landed) or `STRICT` (throw on `@Transactional`). Set it per
datasource with `falkorDbTransactionMode`.

## Result Mapping and Subtypes

`transform(Class)` maps a single returned map or scalar onto a data class/record/POJO. For
polymorphic reads outside the graph API, register the label-to-subtype mapping:

```kotlin
manager.registerSubtype(SessionUser::class.java, listOf("SessionUser", "GuideUser"), GuideUser::class.java)
// now .transform(SessionUser::class.java) yields GuideUser instances
```

`GraphObjectManager` auto-registers sealed hierarchies; `registerSubtype` is the explicit form for
hand-written Cypher.

## Capability Flags

`supportsSchemaManagement` answers whether `indexes` and `constraints` can do anything on this
database. Ask it **before** issuing DDL:

- It is the grammar that can or cannot, not the engine name -- Neptune and generic openCypher
  resolve to `UnsupportedSchemaGrammar`, which throws on every operation by design, while an
  engine you have never heard of may be perfectly capable. Do not match `type` against a list of
  known-good engines.
- `TransactionalPersistenceManager` delegates the flag to its schema source and reports `false`
  when none was supplied, because nothing there could run DDL even on a capable engine. Its
  `indexes`/`constraints` accessors then throw with a message telling you to obtain managers
  through `PersistenceManagerFactory` or use a non-transactional manager.
- Schema DDL always runs in auto-commit; it cannot execute inside an open data transaction.

## Native Driver Access

`NativeDriverSource<T : Any>` is a capability interface for providers that can hand out the
engine's own client object -- the thing application code would otherwise build for itself:

```kotlin
interface NativeDriverSource<T : Any> { val nativeDriver: T }
```

`Neo4jConnectionProvider` implements `NativeDriverSource<Driver>`. The documented downcast idiom:

```kotlin
val driver = (provider as? NativeDriverSource<*>)?.nativeDriver as? Driver
    ?: error("$forWhom speaks the bolt driver directly; ${provider.type} has none")
```

Providers with no client to give do not implement the interface at all, so `as?` yields null and
the absence is a typed answer rather than a failure. Prefer this over rebuilding a client from
connection properties: that opens a second pool and drifts from the provider's configuration, and
for an engine living inside the application process there is nothing to rebuild. The returned
object stays owned by the provider -- it is closed by `ConnectionProvider.end()`, so never close it
yourself.

## RETURN Clause Rules

Return exactly one map or one scalar per row; several columns do not map cleanly and NULLs in
them break the mapping.

```cypher
-- WRONG
RETURN a.name, a.age, b.title

-- RIGHT
RETURN { name: a.name, age: a.age, title: b.title } AS result
RETURN properties(p)
RETURN count(p) AS total
```

Aggregate across nodes by composing into one map in the `RETURN`:

```cypher
MATCH (p:Proposition)-[:HAS_MENTION]->(m:Mention)
WITH m.type AS entityType, m.name AS name, count(p) AS mentionCount
ORDER BY mentionCount DESC LIMIT 30
RETURN { entityType: entityType, name: name, mentionCount: mentionCount } AS result
```

Then `.transform(MentionDto::class.java)` maps it directly.

## See Also

- [SKILL.md](../SKILL.md) -- version pinning, requirements, API choice
- [GraphObjectManager reference](graph-object-manager.md) -- annotated models and the DSL
- [Schema and search reference](schema-and-search.md) -- `indexes` / `constraints`, vector and full-text
- [Multi-database reference](multi-db.md) -- engines, dialects, connection properties
