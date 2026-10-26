# Schema Management, Vector/Full-Text Search, Keyset Pagination

## Table of Contents

1. [Naming: there is no vectorSearch / fullTextSearch](#naming)
2. [Imperative Schema API](#imperative-schema-api)
3. [Specs](#specs)
4. [Declarative: SchemaCatalog](#declarative-schemacatalog)
5. [Declarative: Annotations on Fragments](#declarative-annotations-on-fragments)
6. [SchemaManager, Versioning, Configuration](#schemamanager-versioning-configuration)
7. [Schema Grammar Capability Flags](#schema-grammar-capability-flags)
8. [Engine Matrix](#engine-matrix)
9. [Vector Search: loadNearest](#vector-search-loadnearest)
10. [Full-Text Search: loadMatching](#full-text-search-loadmatching)
11. [Keyset Pagination: seek](#keyset-pagination-seek)

## Naming

The docs use the loose labels "vector search", "full-text search" and "keyset pagination". Those
describe real features, but **`vectorSearch` and `fullTextSearch` do not exist as API names**. Use
the code names:

| Feature | Public call | Declaring annotation | Spec |
|---------|-------------|----------------------|------|
| vector kNN | `GraphObjectManager.loadNearest(...)` | `@VectorIndex` | `VectorIndexSpec` |
| full-text | `GraphObjectManager.loadMatching(...)` | `@FullTextIndex` | `FullTextIndexSpec` |
| range index | `indexes.ensure` / `ensureRange` | `@RangeIndex` | `RangeIndexSpec` |
| uniqueness | `constraints.ensure` / `ensureUnique` | `@Unique` | `UniquenessConstraintSpec` |
| keyset | `seek { }` in the DSL (`KeysetPlanner`) | mirror the cursor with `@RangeIndex` | -- |

Internal machinery you may meet in stack traces -- all `internal` declarations, so none is an entry
point: `VectorIndexResolver`, `FullTextIndexResolver`, `VectorSearchPlanner`,
`FullTextSearchPlanner`, `GraphViewVectorSearchBuilder`, `FragmentVectorSearchBuilder`,
`GraphViewFullTextSearchBuilder`, `FragmentFullTextSearchBuilder`, `QueryIndexAdvisor`,
`KeysetPlanner`. Go through the manager instead.

## Imperative Schema API

Every `PersistenceManager` exposes `indexes: IndexManager` and `constraints: ConstraintManager`.
All operations are idempotent, drift-aware, and run in auto-commit (DDL cannot run inside a data
transaction):

```kotlin
manager.indexes.ensure(VectorIndexSpec("Proposition", "embedding", dimensions = 1536))
manager.indexes.ensure(RangeIndexSpec("Proposition", "contextId"))
manager.indexes.ensure(RangeIndexSpec("Message", listOf("sessionId", "createdAt")))   // composite
manager.indexes.ensure(FullTextIndexSpec("Chunk", "text"))                             // single prop
manager.indexes.ensure(FullTextIndexSpec("Entity", listOf("name", "description")))      // multi prop
manager.constraints.ensure(UniquenessConstraintSpec("ChatSession", "sessionId"))
manager.constraints.ensure(UniquenessConstraintSpec("Membership", listOf("tenantId", "userId")))
```

`IndexManager`: `ensure`, `ensureVector`, `ensureRange(label, vararg properties)`, `find`,
`list`, `drop`, `recreate`.
`ConstraintManager`: `ensure(spec, violationSampleSize)`, `ensureUnique(label, vararg properties)`,
`find`, `list`, `drop`, `recreate`.

`ensure` returns an `EnsureResult`:

| Outcome | Meaning |
|---------|---------|
| `Created(info)` | nothing existed; created |
| `AlreadyMatching(info)` | a matching item exists; nothing changed |
| `Drift(existing, requested)` | same identity, different shape; nothing changed -- call `recreate` |
| `Recreated(previous, current)` | from an explicit `recreate` -- destructive |
| `Violation(requested, conflictingSample)` | constraint only: existing data violates it, with a bounded sample |

Drift is only ever reported for what introspection can **see**. A null on the observed side means
the engine did not report that attribute, which is not evidence of a mismatch -- otherwise `ensure`
would complain forever on engines that do not surface the field. The same rule covers pinned HNSW
parameters and the Neo4j-only analyzer.

## Specs

```kotlin
VectorIndexSpec(label, property, dimensions,
                similarity = SimilarityFunction.COSINE,
                name = null, hnswM = null, hnswEfConstruction = null,
                engineOptions = emptyList())
RangeIndexSpec(label, properties /* or a single property */, name = null)
FullTextIndexSpec(label, properties /* or a single property */, name = null, analyzer = null)
UniquenessConstraintSpec(label, properties /* or a single property */, name = null)
```

`dimensions` must be positive; specs requiring properties reject empty lists. Engine-specific
vector tuning goes in `engineOptions` as `EngineVectorOptions` implementations --
`Neo4jVectorOptions(quantizationEnabled = ...)` and `FalkorDbVectorOptions` are the shipped ones.
Options addressed to another engine are never reported as unsupported: declaring Neo4j options
does not make a FalkorDB deployment incorrect, it just does not apply there.

## Declarative: SchemaCatalog

```kotlin
@Bean
fun propositionSchema(embeddings: EmbeddingService) = SchemaCatalog.of(
    VectorIndexSpec("Proposition", "embedding", embeddings.dimensions),
    RangeIndexSpec("Proposition", "contextId"),
    UniquenessConstraintSpec("Proposition", "id"),
)
```

Registering a `SchemaCatalog` bean makes Drivine ensure everything on startup -- indexes before
constraints. Multiple catalog beans merge (`plus`/`merge`), identical declarations deduplicate,
and conflicting declarations for the same `(kind, label, properties)` fail startup.

Targeting, per catalog: `forDefaultDatabase()` (the first-registered datasource),
`forDatabase("users")`, `forDatabases("a", "b")`, `forAllDatabases()`. The default target is
`DatabaseTarget.All` -- a **broadcast** that skips schema-incapable engines with a warning, while an
explicitly **named** target is strict and fails startup on an unknown or incapable datasource.
Targeting is last-wins, not additive: use one `forDatabases(...)` call for several names.

## Declarative: Annotations on Fragments

```kotlin
@NodeFragment(labels = ["Proposition"])
@FullTextIndex(properties = ["title", "body"])            // multi-property, class-level
data class PropositionNode(
    @NodeId @RangeIndex @Unique val id: String,
    @RangeIndex val contextId: String,
    val title: String,
    @FullTextIndex val body: String,                       // single-property, property-level
    @VectorIndex(similarity = SimilarityFunction.COSINE, hnswM = 32, hnswEfConstruction = 200)
    val embedding: List<Float>? = null,
)

@Bean
fun schema(embeddings: EmbeddingService) = SchemaCatalog.fromFragments(
    VectorDimensionProvider { _, _ -> embeddings.dimensions },
    PropositionNode::class,
)
```

`@RangeIndex`, `@Unique` and `@FullTextIndex` are `@Repeatable` and work property-level (single
property, `properties` left empty) or class-level (composite, `properties` in order). Each takes an
explicit `name` (empty derives one from label and properties) and `@FullTextIndex` an `analyzer`
(Neo4j only). Dimensions are deliberately **not** on `@VectorIndex` -- they come from the embedding
model at runtime through `VectorDimensionProvider`. `fromFragments` accepts Kotlin `KClass` or Java
`Class` forms.

The annotation is the *query-side* declaration that an embedding is searchable and is independent of
how the index was created: it is needed even when the index comes from a hand-written
`VectorIndexSpec`. Declaring the same index in both places is expected -- the tuned declaration
wins, and keeping the annotation in place is what lets `VectorIndexResolver` find the query.

## SchemaManager, Versioning, Configuration

Catalogs are applied by a `SchemaManager` bean, enforced once on startup, and it is injectable for
runtime schema changes:

```kotlin
@Component
class Reindexer(private val schema: SchemaManager) {
    fun afterBulkLoad() = schema.enforce()        // idempotent
    fun afterReembed() = schema.recreateAll()     // the destructive hammer
}
```

Drift detection catches only what introspection can see. Swapping in an embedding model of the
**same** dimensions leaves stale vectors, so tag the catalog with a token:
`SchemaCatalog.of(...).withVersion(embeddings.modelId)` -- a changed token drops and recreates that
catalog's items once, then records the new token in a reserved `_DrivineSchema` marker node. A
first-ever token is *adopted* without recreating, so switching versioning on never nukes a healthy
schema. Recreating rebuilds the index from the **stored** embedding properties; it does not
re-embed. Re-embed first, then `enforce()`.

```yaml
drivine:
  schema:
    enabled: true              # startup initialization only; the bean still works at runtime
    mode: FAIL_FAST            # FAIL_FAST (default) | WARN
    recreate-on-drift: false   # destructive
    recreate-on-startup: false # destructive
    violation-sample-size: 10
  query:
    index-advice: FAIL         # WARN (default) | OFF
```

## Schema Grammar Capability Flags

`SchemaGrammar` carries the divergence flags the managers branch on:
`supportsIfNotExists`, `supportsNamedItems`, `constraintsRequireBackingIndex`,
`constraintCreationIsAsync`, and **`indexOperationsAreAsync`**.

`FalkorDbSchemaGrammar` sets `engine = "FalkorDB"`, `supportsIfNotExists = false`,
`supportsNamedItems = false`, `constraintsRequireBackingIndex = true`,
`constraintCreationIsAsync = true` and `indexOperationsAreAsync = true`. Because its index DDL
returns while the engine builds or tears down in the background, `IndexManager.awaitDropped` polls
`find(spec)` until the index really is gone (bounded by an async drop timeout, then
`DrivineException`). Without that wait a drop-then-`ensure` would see the index it had just removed.
On synchronous engines the flag is `false` and no polling happens.

FalkorDB also manages indexes per label, so `narrowTo` keeps a drop from taking more properties
than the spec asked for, and `createIndex` emits only the properties missing from the label's
existing index (re-adding an indexed property is an error there). FalkorDB uniqueness constraints
are not Cypher at all: they go out as a `SchemaStatement.Native` Redis command
(`GRAPH.CONSTRAINT CREATE / DROP`) executed at driver level, with the backing index auto-created
and the asynchronous build polled.

Ask the manager, not the enum: gate on `PersistenceManager.supportsSchemaManagement`
(see `references/persistence-manager.md`) rather than matching `type` against known-good names.

## Engine Matrix

| | Neo4j | Memgraph | FalkorDB |
|---|---|---|---|
| Vector | `CREATE VECTOR INDEX ... IF NOT EXISTS` | `WITH CONFIG {...}`, uSearch metrics | `OPTIONS {...}`, unnamed |
| Full-text | `CREATE FULLTEXT INDEX ... ON EACH [...]` + analyzer | `CREATE TEXT INDEX ... ON :L(props)` | Redis `db.idx.fulltext.createNodeIndex`, per property, unnamed |
| Range | named, composites supported | label-property style | per-label coverage, extended incrementally |
| Uniqueness | `REQUIRE ... IS UNIQUE` | `ASSERT ... IS UNIQUE` | Redis command, async build |
| Item names | yes | vector only | no |

Full-text search itself runs on Neo4j (`db.index.fulltext`), FalkorDB (`db.idx.fulltext`, by
label) and Memgraph (`text_search`, GA -- no experimental flag). **Neptune and generic openCypher
have no schema management**: they resolve to `UnsupportedSchemaGrammar` and fail loudly rather
than silently no-op.

## Vector Search: loadNearest

Works on a `@GraphView` (searches the **root** fragment's embedding, returns the projected view)
and on a bare `@NodeFragment` (searches and returns the nodes). Each hit is
`Scored<T>(value, score)`, normalised to **similarity, higher = more similar** on every engine, so
ordering and `threshold` mean the same thing regardless of backend.

```kotlin
graphObjectManager.loadNearest(PropositionView::class.java, queryEmbedding, topK = 20)
graphObjectManager.loadNearest(PropositionView::class.java, "titleEmbedding", queryEmbedding,
                               topK = 20, threshold = 0.8)          // several embeddings on one node
graphObjectManager.loadNearest<PropositionView>(queryVector, topK = 20) {
    where {
        proposition.contextId eq ctx
        mentions.any { resolvedId eq entityId }
    }
}
```

The searched embedding is named by `@VectorIndex` on the searched fragment -- inferred when the
fragment declares one embedding, named explicitly to choose among several.

**`topK` is the index's `k`, not a guaranteed result count.** On a view, the required relationships
and `threshold` apply **after** the kNN, so a top-K candidate that fails the filter is dropped. A
fragment search has no relationship filter and returns the full top K minus any `threshold` cut.

**`k` is also the beam width** -- on Lucene-backed engines the result queue *is* the HNSW candidate
queue, so `k` decides how much of the graph the search explores. A small `k` can miss a genuinely
nearest vector; upstream measured a vector at true global rank 3 that no `k <= 100` returned and
that came back at rank 3 at `k = 200`. Neo4j exposes no separate `ef_search`, so raising `k` is the
only query-time recall lever. Use `searchK` to widen the beam without widening the result:

```kotlin
graphObjectManager.loadNearest<PropositionView>(dsl, queryEmbedding, topK = 40, searchK = 200) {
    where { proposition.contextId eq ctx }
}
```

`searchK` is what the index is asked for; `topK` becomes a `LIMIT` applied after the filter, so
over-fetching recovers rows the filter would have thinned away. `searchK < topK` throws, since it
could only lose results. Omit it and the emitted query is unchanged. On Neo4j, over-fetching also
re-ranks the beam by **exact** similarity before trimming, because a quantized index scores against
quantized vectors and does not order identically to exact similarity.

Filtered searches dilute: you get roughly `k x selectivity` rows, and they are the globally
nearest that happen to be in scope rather than the nearest within scope. `searchK` mitigates that.

`partitionLabel` searches inside one index per scope (tenant, corpus), pre-filtered by construction
so `k` is no longer diluted. The label changes only which index is located -- the node keeps its own
labels, so `where { }` and the projection are unaffected. The name re-derives as
`"${label}_${property}_vector"`, so a fragment whose `@VectorIndex` pins an explicit `name` cannot
be partitioned, and says so.

Pin physical shape portably on the annotation (`hnswM`, `hnswEfConstruction`; `< 1` means "engine
default") and engine specifics in the spec (`engineOptions`). An unpinned parameter is the
engine's to choose and can never drift; a pinned one the engine contradicts does. The effective
configuration is logged after every creation either way. Backends with no native vector index
(Neptune) throw `UnsupportedOperationException`.

## Full-Text Search: loadMatching

The full-text mirror of `loadNearest`: finds the `topK` nodes most relevant to a text query and
returns them scored and typed, normalised to a consistent `[0, 1]` similarity across engines, with
no consumer Cypher and no per-engine score wrangling.

```kotlin
graphObjectManager.loadMatching<ChunkNode>("graph databases", topK = 20)
graphObjectManager.loadMatching<ChunkNode>("graph databases", topK = 20, threshold = 0.5)
graphObjectManager.loadMatching<ChunkView>("graph databases", topK = 20)   // a view searches its root
graphObjectManager.loadMatching(ChunkNode::class.java, ChunkNodeQueryDsl.INSTANCE,
                                "graph databases", topK = 20) {
    where { query.containerSectionId eq "sec-1" }
}
```

The index resolves from `@FullTextIndex` on the searched fragment -- property-level for one field,
class-level `@FullTextIndex(properties = [...])` for several -- the same annotation the schema
feature uses to create it. `threshold` defaults to `0.0` (keep everything); `topK` is a trailing
`LIMIT`. Polymorphic dispatch works: `loadMatching<SealedBase>` returns each hit as its concrete
subtype. The query string passes through to the engine's full-text language (Lucene syntax on
Neo4j: `AND` / `OR` / `"phrase"` / `field:term`) and is **not** escaped for you -- never forward raw
user input. `where { }` behaves exactly as in `loadNearest`. Backends without a native full-text
index throw `UnsupportedOperationException`.

## Keyset Pagination: seek

`limit(n)` / `skip(n)` push a row bound into the generated Cypher after `ORDER BY`, so "top 20 by
recency" happens database-side instead of over-fetching. For large or mutating result sets prefer
`seek`: its properties must match the root `orderBy` properties in the same order, Drivine derives
each comparison from the sort direction and builds the lexicographic continuation predicate, and the
page must end on a unique key so ties can neither be skipped nor duplicated.

```kotlin
@NodeFragment(labels = ["Session"])
@RangeIndex(properties = ["lastActivityAt", "sessionId"])   // mirrors the cursor
data class SessionNode(
    @NodeId val sessionId: String,
    val lastActivityAt: Instant,
)

graphObjectManager.loadAll<SessionView> {
    orderBy { session.lastActivityAt.desc(); session.sessionId.desc() }
    seek { session.lastActivityAt after cursor.lastActivityAt
           session.sessionId  after cursor.sessionId }
    limit(pageSize)
}
```

Each cursor value is the corresponding property of the previous page's last row. For `hasMore`
without a second query, ask for `limit(pageSize + 1)`, return the first `pageSize`, and treat the
extra row as the signal.

**Sort keys must be non-null in the data.** A row with a null sort key satisfies no comparison and
is dropped from every page after the first, and engines disagree about where nulls sort, so the
shape of that loss is not portable. Use non-null keys, or filter nulls out in `where`.

**The index must mirror the cursor** -- a range index over exactly the `orderBy` properties, in the
same order -- or the engine scans the whole continuation and takes the top n, no better than `skip`.
Upstream profiled a 20-row page from the middle of a 25k-node relation on Neo4j: 27 database
accesses with the composite index over both keys, 200,020 with a single-property index on the leading
key, 500,010 with none. With the matching index the plan is `NodeIndexSeek` then `Limit`, with no
sort at all, since the index supplies the order. A partially covering index is much worse because
Drivine constrains every cursor key with `IS NOT NULL` (a Neo4j index excludes nodes lacking the
property, so the planner will not use a composite index unless the query provably excludes those
same nodes): that conjunct is what makes the composite index usable, and against an index that does
not contain the property it is just an extra property read per row.

**Neo4j only.** Memgraph and FalkorDB cannot satisfy an `ORDER BY` from an index -- both place a
blocking sort between scan and limit, measured even for one indexed property and no predicate -- so
keyset there costs O(remaining) rather than O(page), no index changes that, and Drivine neither
emits the `IS NOT NULL` conjuncts (which cost Memgraph its range bound) nor gives index advice.
`seek` is still worth using for its *stability* under concurrent writes, just not for its cost.

Drivine tells you when the index is missing: any root-level `orderBy`, with or without `seek`, is
checked against the database's indexes and reports when nothing mirrors it, naming both the
`@RangeIndex` and the `indexes.ensure(RangeIndexSpec(...))` fix. Ordering without an index is
correct, merely unindexed, so the default warns once per label/property. Set
`drivine.query.index-advice: FAIL` in development or CI to fail the build, or
`graphObjectManager.indexAdvice = IndexAdvicePolicy.FAIL` without Spring / to override one manager
after hand-out. A query already pinned to a single root (a top-level `where` equality on the
`@NodeId`, or on a property carrying a single-property uniqueness constraint) is never advised on,
because its ordering is over one row. An equality inside `anyOf` does not count, since it constrains
only one branch of the OR. The index list is read once and cached per manager, so the check costs
one round trip per process rather than one per query -- if you ensure indexes lazily after the first
ordered query, that cache is stale; construct the manager after schema setup, or use `OFF`.

`seek` rejects missing or misaligned order keys, null cursor values, use together with `skip`, and
use on any operation that would ignore it (`count`, `deleteAll`, `loadNearest`, `loadMatching`) --
those fail loudly instead of quietly returning an unpaginated result. Drivine owns query planning
but not cursor serialization: choose an opaque cursor format appropriate to your API. Java callers
use `.seek(q -> List.of(...))` with `PropertyReference.after(value)`.

## See Also

- [SKILL.md](../SKILL.md) -- version pinning, requirements, API choice
- [PersistenceManager reference](persistence-manager.md) -- `indexes`/`constraints` host, capability flags
- [GraphObjectManager reference](graph-object-manager.md) -- annotations and DSL operators
- [Multi-database reference](multi-db.md) -- engines, dialects, connection properties
