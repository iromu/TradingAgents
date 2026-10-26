---
name: embabel-drivine4j
description: >-
  Build type-safe graph database clients with Drivine4j 0.0.81 for Neo4j, FalkorDB,
  Amazon Neptune, Memgraph and the in-process EMBABEL Cypher engine. Use when wiring a
  DataSourceMap or ConnectionProperties, choosing between the low-level PersistenceManager
  (manual Cypher) and the high-level GraphObjectManager (annotated @NodeFragment /
  @GraphView models with a KSP type-safe DSL), mapping @GraphRelationship / @GraphPath /
  @RelationshipFragment, querying with loadAll, loadNearest, loadMatching or seek,
  managing vector / full-text / range / unique schema with SchemaCatalog and
  indexes.ensure, or configuring drivine4j-spring-boot-starter, drivine4j-codegen (KSP)
  or drivine4j-codegen-java (APT). Also trigger on mentions of Drivine4j, org.drivine,
  Cypher in Java or Kotlin, graph database client, DatabaseType, HNSW vector index,
  full-text index, or keyset pagination.
metadata:
  author: "Iván Rodríguez Murillo <wantez@gmail.com>"
  version: 0.0.81
  category: development
  tags: [kotlin, java, graph-database, neo4j, cypher, drivine4j, embabel]
---

# Drivine4j -- Type-Safe Graph Database Client

Drivine4j is the graph database client for [Embabel](https://hub.embabel.com), agentic AI
on the JVM. It speaks Cypher to **Neo4j**, **FalkorDB**, **Amazon Neptune** and **Memgraph**
from one codebase, with two complementary APIs:

| API | Level | Cypher | Best for |
|-----|-------|--------|----------|
| **PersistenceManager** | Low-level | Manual, full control | Complex queries, migrations, one-off ops |
| **GraphObjectManager** | High-level | Generated from annotations | CRUD, domain views, type-safe DSL |

Both sit on the same connection layer: `DataSourceMap` -> `ConnectionProperties` ->
`DatabaseRegistry` -> `ConnectionProvider`.

## Instructions

1. Read "Pin the Version -- Read This First" before anything else — the API surface and artifact names differ between Drivine4j versions; confirm which version the project pins.
2. Confirm "Requirements" and "Installation" (which starter/codegen modules the project needs), then pick the API style in "Choose Your API": PersistenceManager for manual Cypher, GraphObjectManager for annotated models.
3. Use the exact code names from "Search & Pagination (use the code names)" when writing queries — don't paraphrase method names.
4. Model vector stores and constraints via "Schema Management"; check "Testing" for in-process EMBABEL engine tests.
5. Cross-check "Pitfalls" and "When NOT to Use" before finalizing; deeper material lives in the files listed under "Reference Files".

## Pin the Version -- Read This First

```kotlin
dependencies {
    implementation("org.drivine:drivine4j:0.0.81")
}
```

- **The upstream README still advertises `0.0.77` in every install snippet.** That is upstream
  documentation lag. The release version is `build.gradle:15` (`version = '0.0.81'`), not the
  README. Pin `0.0.81`.
- **The repository publishes no git tags at all** -- `main` is the only ref. There is no
  `v0.0.81` tag to check out and no tag to cite; do not invent one.
- Repo: `https://github.com/liberation-data/drivine4j` (the org is `liberation-data`;
  `embabel/drivine4j` does not exist -- `DatabaseType.EMBABEL` is an engine name, not an org).

## Requirements

| Need | Version | Why |
|------|---------|-----|
| Java | 21+ | `java.toolchain` language version 21 in `build.gradle` |
| Kotlin | **2.2.0+** for `GraphObjectManager` | the generated DSL uses **context parameters** -- compile with `-Xcontext-parameters` |
| Kotlin | any for `PersistenceManager` | no DSL generation involved |

## Installation

### Core library only (PersistenceManager)

```kotlin
dependencies {
    implementation("org.drivine:drivine4j:0.0.81")
}
```

### With the type-safe DSL (GraphObjectManager)

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
    kotlin("jvm") version "2.2.0"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters")   // required for the DSL
    }
}

dependencies {
    implementation("org.drivine:drivine4j:0.0.81")
    ksp("org.drivine:drivine4j-codegen:0.0.81")          // KSP processor, Kotlin sources
}
```

Codegen modules (both are separate published artifacts under `org.drivine`, same version):

| Artifact | Processor | Covers |
|----------|-----------|--------|
| `drivine4j-codegen` | KSP | Kotlin `@GraphView` sources -> Kotlin DSL extension objects |
| `drivine4j-codegen-java` | APT (annotation processor) | Java `@GraphView` sources |

> The KSP processor only walks **Kotlin** sources. A `@GraphView` declared in Java gets no
> generated Kotlin DSL -- define views in Kotlin, fragments in either language, and reach Java
> callers through `JavaQueryBuilderKt.query(manager, View::class.java).filterWith(...)`.
> Spring users add `org.drivine:drivine4j-spring-boot-starter` for `@EnableDrivine`,
> `@EnableDrivineTestConfig` and the `database:` / `drivine.*` property bindings.

## Configuration

```kotlin
@Configuration
@EnableDrivine
class AppConfig {
    @Bean
    fun dataSourceMap(): DataSourceMap = DataSourceMap(
        mapOf("neo" to ConnectionProperties(
            type = DatabaseType.NEO4J,
            host = "localhost",
            port = 7687,
            userName = "neo4j",
            password = "password",
            databaseName = "neo4j",
        ))
    )
}
```

Each `DataSourceMap` key is the datasource name you pass to the factories; `"default"` resolves to
the first-registered one.

> **Doc-vs-code divergence, recorded deliberately.** The README injects managers with
> `@Qualifier("neoManager")` in one section and `@Qualifier("analytics")` in another -- and those two
> conventions do not even agree with each other. Neither the starter nor the sample registers
> per-datasource manager beans or uses `@Qualifier` at all (checked: zero occurrences in
> `drivine4j-spring-boot-starter/src/main`). What the starter actually publishes is
> `PersistenceManagerFactory` and `GraphObjectManagerFactory`; the attested idiom, taken from
> `drivine4j-sample/.../SampleAppContext.kt`, is to expose the manager yourself from the factory:
>
> ```kotlin
> @Bean
> fun persistenceManager(
>     factory: PersistenceManagerFactory,
>     @Value("\${drivine.default-datasource:#{null}}") datasource: String?,
> ): PersistenceManager = factory.get(datasource ?: "default")
> ```
>
> Then inject `PersistenceManager` / `GraphObjectManager` by type, with no qualifier. For a second
> datasource, add a second `@Bean` calling `factory.get("users")`.

### DatabaseType -- the field most people get wrong

`ConnectionProperties.type` is **required**. Declared members: `NEO4J`, `POSTGRES`, `NEPTUNE`,
`FALKORDB`, `MEMGRAPH`, `EMBABEL` (plus `DatabaseType.fromValue(String)` for parsing).

Four of those are usable today. **`POSTGRES` is forward-declared only**: the name appears in
exactly one file in the whole source tree -- the enum itself. There is no `Connection`, no
`ConnectionProvider`, and no branch in `ConnectionProviderBuilder.register`, which falls through to
`DrivineException("Type POSTGRES is not supported by ConnectionProviderBuilder")`. It also carries
the default `buildableFromProperties = true`, so a `DataSourceMap` entry with `type = POSTGRES`
fails at registry construction. Do not configure it. (Upstream `docs/postgres-sql-backend-plan.md`,
an internal plan not collected here, describes it as future work.)

`EMBABEL` is **not a server**. It is Embabel's in-process Cypher engine -- compiler, evaluator
and store living inside the application process, with no wire and no container. So
`buildableFromProperties = false`: host/port cannot describe it and `DatabaseRegistry` will not
try to build a provider for it. `ConnectionProviderBuilder.register` names that explicitly ("X
engines supply their own ConnectionProvider"). The application supplies its own `ConnectionProvider`:

```kotlin
// Spring: publish the provider as a bean and the starter registers it, or:
databaseRegistry.register(myEmbabelConnectionProvider)   // keyed by provider.name
```

The other engines (`NEO4J`, `NEPTUNE`, `FALKORDB`, `MEMGRAPH`) have
`buildableFromProperties = true` and are built from properties by `ConnectionProviderBuilder`.

### Escape hatch to the driver

`NativeDriverSource<T>` hands out the engine's own client object, so maintenance code can speak
the driver directly without opening a second pool. `Neo4jConnectionProvider` implements
`NativeDriverSource<Driver>`; providers with nothing to hand out simply do not implement it, so
the documented downcast is a typed answer rather than a failure:

```kotlin
val driver = (provider as? NativeDriverSource<*>)?.nativeDriver as? Driver
    ?: error("${provider.name} speaks the bolt driver directly; ${provider.type} has none")
// owned by the provider -- closed by ConnectionProvider.end(); never close it here
```

## Choose Your API

Use **PersistenceManager** for manual Cypher, migrations, graph algorithms, or an existing
Cypher codebase. Use **GraphObjectManager** for annotation-driven CRUD, the type-safe DSL,
dirty tracking, cascade save/delete, vector/full-text search and keyset pagination.

### PersistenceManager quick start

```kotlin
@Component
class PersonRepository @Autowired constructor(
    val manager: PersistenceManager,          // by type; see the factory note above
) {
    @Transactional
    fun findByCity(city: String): List<Person> = manager.query(
        QuerySpecification
            .withStatement("MATCH (p:Person {city: \$city}) RETURN properties(p)")
            .bind(mapOf("city" to city))
            .transform(Person::class.java)
    )
}
```

Interface surface: `query`, `getOne`, `maybeGetOne`, `optionalGetOne`, `execute`,
`executeBatch`, `registerSubtype`, plus `database`, `type`, `grammar`, `indexes`,
`constraints`, `supportsSchemaManagement`.

**RETURN rule:** always return one map or one scalar, never several columns --
`RETURN { name: a.name, age: a.age } AS result`, or `RETURN properties(p)`.

### GraphObjectManager quick start

```kotlin
@NodeFragment(labels = ["Person"])
data class Person(
    @NodeId val uuid: String,
    val name: String,
    val bio: String?,
)

@GraphView
data class PersonCareer(
    @Root val person: Person,
    @GraphRelationship(type = "WORKS_FOR", direction = Direction.OUTGOING)
    val employmentHistory: List<WorkHistory>,
)
```

```kotlin
graphObjectManager.loadAll<PersonCareer>()
graphObjectManager.load<PersonCareer>(uuid)
graphObjectManager.count(RaisedAndAssignedIssue::class.java)

graphObjectManager.loadAll<PersonCareer> {
    where { person.bio contains "Lead" }
    orderBy { person.name.asc() }
    limit(20)
}

val p = graphObjectManager.loadOrThrow<PersonCareer>(uuid)
graphObjectManager.save(p.copy(person = p.person.copy(bio = "Updated bio")))  // dirty fields only
```

**Cascade is a call argument, not an annotation attribute.** `@GraphRelationship` carries only
`type`, `direction`, `maxDepth`. Save/delete cascade is the `CascadeType` parameter on
`save` / `saveAll` / `delete` / `deleteAll`: `NONE` (default), `DELETE_ORPHAN`, `DELETE_ALL`,
`PRESERVE` (append-only -- add edges, never remove snapshot-detected ones).

Composition over inheritance: build a `@GraphView` per use case instead of one monolithic model.
Drivine emits one pattern comprehension per projected relationship, so one root is one row.

## Search & Pagination (use the code names)

The loose labels "vector search" / "full-text search" name real features, but there is **no**
`vectorSearch` or `fullTextSearch` in the API. The attested entry points are:

| Feature | Call | Score type | Declaring annotation |
|---------|------|-----------|----------------------|
| Vector (HNSW kNN) | `graphObjectManager.loadNearest(...)` | `Scored<T>(value, score)` | `@VectorIndex` |
| Full-text | `graphObjectManager.loadMatching(...)` | `Scored<T>(value, score)` | `@FullTextIndex` |
| Keyset paging | `seek { ... }` in the DSL (`KeysetPlanner`) | -- | `@RangeIndex` mirroring the cursor |

```kotlin
// vector: ranks by the @VectorIndex-ed embedding on the root fragment (or the fragment itself)
val hits: List<Scored<PropositionView>> =
    graphObjectManager.loadNearest(PropositionView::class.java, queryEmbedding, topK = 20)

// full-text
val docs: List<Scored<ChunkNode>> = graphObjectManager.loadMatching<ChunkNode>("graph databases", topK = 20)

// keyset: seek properties must match the root orderBy properties, in order, ending on a unique key
graphObjectManager.loadAll<SessionView> {
    orderBy { session.lastActivityAt.desc(); session.sessionId.desc() }
    seek { session.lastActivityAt after cursor.lastActivityAt
           session.sessionId  after cursor.sessionId }
    limit(pageSize)
}
```

Three things that cost people hours:
- **`topK` is the index's `k`, not a result count.** On a view, required relationships and
  `threshold` are applied *after* the kNN, so you can get fewer than `topK`. On Lucene-backed
  engines `k` is also the search beam: a genuinely nearest vector can be missed at small `k`.
  Widen the beam without widening the result with **`searchK`** (`searchK < topK` throws).
- **Keyset needs a mirroring index.** A `@RangeIndex` over exactly the `orderBy` properties, in
  the same order, or the query scans and sorts. `indexAdvice` (`WARN` default | `FAIL` | `OFF`,
  or `drivine.query.index-advice`) tells you when it is missing. Neo4j only -- Memgraph and
  FalkorDB always place a blocking sort.
- Full-text and vector search are engine features. Backends without the index throw
  `UnsupportedOperationException`.

See `references/schema-and-search.md` for specs, `SchemaCatalog`, `EnsureResult`, and the
engine matrix.

## Schema Management

`PersistenceManager` exposes `indexes` (`IndexManager`) and `constraints` (`ConstraintManager`)
with idempotent, drift-aware `ensure` / `recreate` / `drop` / `list` / `find`, always in
auto-commit (DDL cannot run inside a data transaction):

```kotlin
manager.indexes.ensure(VectorIndexSpec("Proposition", "embedding", dimensions = 1536))
manager.indexes.ensure(RangeIndexSpec("Message", listOf("sessionId", "createdAt")))
manager.indexes.ensure(FullTextIndexSpec("Entity", listOf("name", "description")))
manager.constraints.ensure(UniquenessConstraintSpec("ChatSession", "sessionId"))
```

**Ask capability, not identity.** `supportsSchemaManagement` reports whether DDL can do anything
at all. Neptune and generic openCypher resolve to `UnsupportedSchemaGrammar`, which throws by
design; `TransactionalPersistenceManager` forwards it to its schema source and reports `false`
when none was supplied. Gate on this flag before provisioning -- do not match `type` against a
list of known-good engines.

**Async index engines wait for you.** `SchemaGrammar.indexOperationsAreAsync` is `true` on
`FalkorDbSchemaGrammar` (as are `constraintCreationIsAsync` and
`constraintsRequireBackingIndex`). `IndexManager` then polls until a dropped index really
disappears, so a drop-then-`ensure` does not resurrect a phantom. On synchronous engines the flag
is `false` and no polling happens.

## Examples

**"Store our domain objects in Neo4j without hand-written Cypher."** "Choose Your API" -> `GraphObjectManager` with an annotated `@NodeFragment` model and the codegen modules named in "Installation".

**"Vector index for embeddings, FalkorDB-backed."** "Schema Management" -> `SchemaCatalog` with the HNSW recipe and `indexes.ensure`; confirm FalkorDB support and exact module names against "Pin the Version" first.

**"Test graph mapping without a server."** "Testing" -> the in-process EMBABEL Cypher engine; reach for `PersistenceManager` only when custom Cypher genuinely has no high-level equivalent ("use the code names" rule applies to `loadAll`/`seek` either way).

## Pitfalls

| Pitfall | Fix |
|---------|-----|
| Copying `0.0.77` from the README | Pin `0.0.81` (from `build.gradle`) |
| Citing a `v0.0.81` tag | No tags exist; `main` is the only ref |
| `ConnectionProperties(type = ...)` omitted | `type` is required; fields are `userName` / `databaseName`, not `username` / `database` |
| Missing `-Xcontext-parameters` | Add it; annotations still work but the DSL does not generate |
| Kotlin < 2.2.0 with GraphObjectManager | Use PersistenceManager, or upgrade Kotlin |
| `@GraphRelationship(cascade = ...)` | No such attribute -- pass `CascadeType` to `save`/`delete` |
| `QuerySpecification.withDialect(...)` | Does not exist -- set `ConnectionProperties.cypherDialect` |
| Treating `execute(spec)` as returning rows | It returns `Unit`; read rows via `query`/`getOne`/`maybeGetOne` |
| Multiple RETURN columns in Cypher | Wrap in one map: `RETURN { ... } AS result` |
| `loadNearest` returns fewer than `topK` | Expected on views; widen the beam with `searchK` |
| Keyset page costs a full scan | Add the mirroring `@RangeIndex`, set `indexAdvice = FAIL` in CI |
| Expecting `EMBABEL` to take host/port | In-process engine; register your own `ConnectionProvider` |
| Dirty tracking on detached objects | Load within the same manager session before saving |
| Null silently clearing a property | `NullPolicy.IGNORE` is the default; opt into `CLEAR` deliberately |

## Testing

One configuration for local dev and CI: `@EnableDrivine` + `@EnableDrivineTestConfig` against
datasources in `application-test.yml`. `USE_LOCAL_NEO4J=true` uses your running Neo4j (fast,
inspectable); the default starts a Neo4j Testcontainer and overrides host/port/password -- the
right choice for CI isolation. `DrivineTestContainer` (a `Neo4jContainer`) also exposes
`getConnectionUrl()` / `getConnectionUsername()` / `getConnectionPassword()` for manual wiring,
alongside `FalkorDbTestContainer` and `MemgraphTestContainer`.

## When NOT to Use

- **Not a general persistence library.** Relational data belongs in Spring Data JPA/JDBC --
  `POSTGRES` here exists for graph-shaped Cypher work, not as an ORM substitute.
- **Not an Embabel agent framework.** Agent/`@Action`/plumbing code goes in the `embabel-agent`
  skill; DICE propositions and their extraction pipeline go in `embabel-dice`. Reach for this
  skill only for the graph client layer beneath them.
- **Not for Gremlin/TinkerPop or RDF/SPARQL stores** -- Drivine4j speaks Cypher dialects only.
- **Not for embedded JVM graph engines used as caches** (TinkerGraph, Neo4j Embedded) -- `EMBABEL`
  covers the in-process Cypher case; nothing here is an in-memory property-graph API.
- **Not for embedding generation.** Drivine manages schema and search; it never computes vectors.
  Supply your own embeddings and a `VectorDimensionProvider`.

## Reference Files

- `references/persistence-manager.md` -- QuerySpecification builder, transactions, subtypes,
  `supportsSchemaManagement`, `NativeDriverSource`, RETURN rules
- `references/graph-object-manager.md` -- annotations, DSL operators, cascade/`NullPolicy`,
  polymorphic and recursive relationships, Java interop
- `references/schema-and-search.md` -- `SchemaCatalog`, specs, `EnsureResult`, engine matrix,
  `loadNearest`/`loadMatching`/`seek` semantics and tuning
- `references/multi-db.md` -- `DatabaseType` incl. `EMBABEL`/`POSTGRES`, dialects, FalkorDB and
  Neptune specifics, multi-datasource setup, testing

## Sources

- Upstream: `https://github.com/liberation-data/drivine4j` (`main`, tree `55249b4`)
- Local distillation inputs: `raw/drivine4j-docs-0.0.81/README.md` (byte-identical to upstream
  `main`) and `raw/drivine4j-docs-0.0.81/docs/*.md` (20 per-release notes, 0.0.48-0.0.79).
  Code-level identifiers were checked against `src/main/kotlin/**`, which the docs do not
  document.
