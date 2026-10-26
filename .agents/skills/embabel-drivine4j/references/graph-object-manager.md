# GraphObjectManager -- High-Level Annotated Model API

## Table of Contents

1. [Overview](#overview)
2. [Annotations](#annotations)
3. [Type-Safe DSL](#type-safe-dsl)
4. [Loading, Saving, Deleting](#loading-saving-deleting)
5. [Null Handling](#null-handling)
6. [Polymorphic Relationships](#polymorphic-relationships)
7. [Recursive Relationships & Paths](#recursive-relationships--paths)
8. [Java Interoperability](#java-interoperability)

## Overview

`GraphObjectManager` maps annotated models to graph queries. It generates Cypher for you and
exposes a type-safe DSL built on Kotlin context parameters (hence Kotlin 2.2.0+). It is created by
`GraphObjectManagerFactory.get(database, type)`, which wraps a `PersistenceManager` and a
`SessionManager`.

## Annotations

### @NodeFragment

```kotlin
@NodeFragment(labels = ["Person"])
data class Person(
    @NodeId val uuid: String,
    val name: String,
    val bio: String?,
)
```

Single attribute: `labels: Array<String>`. It is deliberately **not** a 1-to-1 class-to-node
mapping -- several fragments may describe overlapping slices of the same node.

`@NodeId` marks the identity property used for MERGE keys and load `WHERE` clauses. A
`@GraphProperty("...")` on it overrides the on-disk name for both.

### @GraphView

```kotlin
@GraphView
data class PersonCareer(
    @Root val person: Person,
    @GraphRelationship(type = "WORKS_FOR")
    val employmentHistory: List<WorkHistory>,
)
```

`@GraphView` takes **no attributes** -- there is no `name` parameter. Exactly one field carries
`@Root`; the rest describe edges.

### @GraphRelationship

```kotlin
@GraphRelationship(type = "WORKS_FOR", direction = Direction.OUTGOING, maxDepth = 1)
val employmentHistory: List<WorkHistory>
```

| Attribute | Default | Notes |
|-----------|---------|-------|
| `type` | required | relationship type |
| `direction` | `Direction.OUTGOING` | `OUTGOING` / `INCOMING` / `UNDIRECTED` |
| `maxDepth` | `1` | expansion depth for recursive relationships |

**There is no `cascade` attribute.** Cascade is a parameter on the call, not on the mapping --
see [Loading, Saving, Deleting](#loading-saving-deleting).

Field cardinality is the optionality signal: `List<T>` is a to-many edge, `T?` an optional
single, `T` a required single (roots without it are filtered out of the view, and out of `count`).

### @RelationshipFragment

Captures properties carried by the edge itself, with the target as a field:

```kotlin
@RelationshipFragment
data class WorkHistory(
    val startDate: LocalDate,   // edge property
    val role: String,           // edge property
    @JsonPacked val tags: List<String>? = null,
    val target: Organization,
)
```

`@JsonPacked` stores a collection as one JSON string -- the portable answer for engines without
list property values (Neptune).

### @GraphPath / @Hop

Multi-hop traversal that projects only the final node, skipping intermediaries:

```kotlin
@GraphView
data class ActorDirectors(
    @Root val actor: Actor,
    @GraphPath(hops = [
        Hop("ACTED_IN",    Direction.OUTGOING, label = "Movie"),   // not mapped
        Hop("DIRECTED_BY", Direction.OUTGOING),                    // projected
    ])
    val directors: List<Director>,
)
```

Targets are de-duplicated. `Hop(type, direction, label)` -- `label` constrains intermediate hops;
on the final hop the element type supplies the labels. A path is a fixed heterogeneous hop list,
so `maxDepth` does not apply.

### @Count / @Aggregate

Per-root scalars computed in the load query, without materializing the collection:

```kotlin
@GraphView
data class ActorStats(
    @Root val actor: Actor,
    @Count("ACTED_IN") val movieCount: Long,
    @Aggregate(AggregateFunction.AVG, type = "RATED", property = "score") val avgRating: Double,
)
```

`AggregateFunction`: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`. `property` is required for
SUM/AVG/MIN/MAX and ignored for COUNT. Both are single-hop only; for multi-hop use `@GraphPath`
and aggregate in the application. `@Count(type, direction)` is shorthand for
`@Aggregate(COUNT, type, direction)`.

### @Default / @EmptyWhenAbsent

Both rescue a non-nullable field whose graph value is absent or null:

- `@Default` falls back to the property's declared Kotlin constructor default (or Java field
  initializer), so it works for any type: `@Default val status: String = "active"`.
- `@EmptyWhenAbsent` needs no declared default and always yields an empty collection/map -- which
  makes it the tool for **Java records**, whose state is defined solely by components:
  `@EmptyWhenAbsent List<String> roles`. Collections and maps only.

A provided non-null value always wins.

### @SortedBy

Client-side sort of a projected collection: `@SortedBy("person.name")` (dot paths supported;
`ascending = true` by default). For database-side sorting of a nested collection, use `orderBy`
in the DSL and let the engine's sort emitter handle it.

### @GraphProperty / @PropertyBag

- `@GraphProperty("container_section_id")` overrides the **on-disk** property name while the
  Kotlin field name stays the identity in code, the DSL, mapping and dirty tracking. Not
  combinable with `@PropertyBag` on the same field; two fields mapping to one on-disk name fail
  at model build.
- `@PropertyBag(prefix = "", delimiter = ".")` maps a `Map<String, Any?>` to real, indexable,
  filterable node properties named `"<prefix or field><delimiter><key>"`. Values must be storable
  primitives or homogeneous arrays -- no nested maps. Declare a concrete value type
  (`Map<String, Int>`) when width must round-trip; an untyped `Map<String, Any?>` reads back
  driver-mapped types (an `Int` written comes back `Long`), which is inherent to the engines'
  single 64-bit integer type. `@CompositeProperty` is a recognised alias.

## Type-Safe DSL

Codegen emits an INSTANCE-injecting extension per DSL-spec method for each `@GraphView` --
`loadAll<T> { }`, `deleteAll<T> { }`, `count<T> { }`, and `loadNearest<T>(...) { }` for
`@VectorIndex`-bearing views. Inside the block, `query` (or the root field name) resolves property
references. The block receiver is `GraphQuerySpec<T>`, whose builders are `WhereBuilder`,
`OrderBuilder` and `SeekBuilder`; operators are the ones declared on `PropertyReference` and
`StringPropertyReference`, and the predicates compile to `ComparisonOperator` / `OrderSpec` /
`SeekValueSpec` values.

### Comparison and string

| Operator | Cypher |
|----------|--------|
| `eq` / `neq` | `=` / `<>` |
| `gt` / `gte` / `lt` / `lte` | `>` `>=` `<` `<=` |
| `in` / `inList` | `prop IN $list` |
| `notIn` | `NOT prop IN $list` |
| `hasItem` | `$value IN prop` -- membership in a **list-valued property** |
| `contains` / `startsWith` / `endsWith` | `CONTAINS` / `STARTS WITH` / `ENDS WITH` |
| `matches` | `=~` regex -- **not supported on FalkorDB** |
| `containsIgnoreCase` / `eqIgnoreCase` | `toLower(...) CONTAINS/= $lowered` |
| `isNull()` / `isNotNull()` | `IS NULL` / `IS NOT NULL` |

`hasItem` is named that way on purpose: Kotlin reserves `contains` for the `in` operator.
The comparison it mirrors is `ComparisonOperator.HAS_ELEMENT`.

### Structure

- `anyOf { }` -- OR of the enclosed conditions
- `not { }` -- `NOT ( ... )` over the sub-expression
- `any { }` / `none { }` on a node reference -- quantifiers over a projected to-many relationship
- `instanceOf<T>()` / `instanceOf(clazz)` -- node carries **all** labels of that `@NodeFragment`
- `hasAnyLabel("A", "B")` -- node carries **any** of the given labels
- `depth(relationshipName, maxDepth)` -- override recursive expansion per query
- `limit(n)` / `skip(n)` -- pushed after `ORDER BY`, bound as `$_limit` / `$_skip`
- `seek { ... }` -- keyset continuation, see `references/schema-and-search.md`

### Runtime-key predicates

When the key is not known at compile time (`@PropertyBag` keys, caller-supplied filters), the
untyped hatches still bind values as parameters and backtick-quote dotted paths:

```kotlin
where {
    query.property("metadata.source") eq "wiki"                       // stored path
    query.field("source") eq "wiki"                                    // resolves via @GraphProperty/@PropertyBag
    query.predicate("metadata.tags", ComparisonOperator.HAS_ELEMENT, "kotlin")
    query.predicateOn("sectionId", ComparisonOperator.EQUALS, "s1")    // resolving form
}
```

`property`/`predicate` take the **stored** name; `field`/`predicateOn` take a **logical** key and
resolve it, throwing if it is unresolvable.

### Ordering

`asc()` / `desc()` on a property reference produce the `OrderSpec` for `orderBy { }`. On a
`@GraphView`, `limit(n)` bounds **root entities** -- each returned view keeps its relationships
fully populated, because relationships are pattern comprehensions and one root is one row. Pair
`limit` with `orderBy` for a deterministic top-N. `count(...)` ignores `limit`/`skip`.

## Loading, Saving, Deleting

```kotlin
graphObjectManager.loadAll<View>()                    // or loadAll(View::class.java, "n.state = 'open'")
graphObjectManager.load<View>(uuid)
graphObjectManager.loadOrThrow<View>(uuid)
graphObjectManager.count(View::class.java)             // + (class, whereClause) + DSL overload
graphObjectManager.save(obj, cascade = CascadeType.NONE, nullPolicy = NullPolicy.IGNORE)
graphObjectManager.saveAll(objs, cascade = CascadeType.DELETE_ORPHAN)
graphObjectManager.delete(id, View::class.java, cascade)
graphObjectManager.deleteAll(View::class.java)         // + whereClause + DSL overload
```

`count` is **consistent with `loadAll`**, not a naive node count: a `@GraphView` counts only roots
that satisfy the view's *required* relationships (non-nullable, non-collection
`@GraphRelationship`s), while a plain `@NodeFragment` is a straight label count. Optional and
collection edges constrain nothing.

Three Java-friendly overloads exist alongside the DSL ones (`loadAll(class, whereClause)`,
`count(class, whereClause)`, `deleteAll(class, whereClause)`), using the same aliases: `n` for
fragments, the root field name for views.

Dirty tracking is session-scoped: only objects loaded through the same manager are tracked, and
it merely skips re-writes of unchanged non-null fields -- it never decides whether a null clears.

### CascadeType

Passed to `save` / `saveAll` / `delete` / `deleteAll`:

| Value | Effect |
|-------|--------|
| `NONE` | default; touch only the edge, leave targets intact |
| `DELETE_ORPHAN` | drop target only when it ends up with no other relationships |
| `DELETE_ALL` | drop target and its relationships (recursive for nested views) -- destructive |
| `PRESERVE` | append-only: add new edges, silently skip snapshot-detected removals |

`DELETE_ORPHAN` needs a FalkorDB build with the orphan-delete fix; `DELETE_ALL` on FalkorDB is
tracked via `FalkorDB#1890`.

## Null Handling

`NullPolicy` is the single declared contract, identical across single/batch and every engine:

- `IGNORE` (the default on `save`/`saveAll`) -- merge-patch: write non-null fields, leave the rest
  untouched. A partial object can never destroy anything.
- `CLEAR` -- the object is authoritative; a null clears the stored property. Use only with a
  complete object.

No field is special: a null `@VectorIndex` embedding is preserved under `IGNORE` and cleared
under `CLEAR`, exactly like any other property. This is separate from low-level binding, where
`bindObject` includes nulls by default (suppress per property with
`@JsonInclude(JsonInclude.Include.NON_NULL)`).

## Polymorphic Relationships

Label-based discrimination over sealed classes or interfaces:

```kotlin
@NodeFragment(labels = ["WebUser"])
sealed class WebUser { abstract val uuid: UUID; abstract val displayName: String }

@NodeFragment(labels = ["WebUser", "Anonymous"])
data class AnonymousWebUser(
    override val uuid: UUID,
    override val displayName: String,
    val anonymousToken: String,
) : WebUser()
```

The label set selects the subtype on load. For DSL filtering use `instanceOf<AnonymousWebUser>()`.
Outside the graph API -- i.e. for `transform()` on hand-written Cypher -- register the mapping
explicitly with `manager.registerSubtype(baseClass, labels, subClass)`.

## Recursive Relationships & Paths

```kotlin
@GraphView
data class LocationHierarchy(
    val location: Location,
    @GraphRelationship(type = "HAS_LOCATION", direction = Direction.OUTGOING, maxDepth = 3)
    val subLocations: List<LocationHierarchy>,
)
```

`maxDepth = 3` expands to a variable-length match (`*1..3`); cycle detection prevents infinite
loops. Override per query with `depth("subLocations", 2)`.

## Java Interoperability

| Feature | Java | Note |
|---------|------|------|
| `@NodeFragment` / `@RelationshipFragment` | full | identical semantics to Kotlin |
| `@GraphView` runtime | full | load, save, polymorphism |
| Type-safe filtering | full | `filterWith(...)` + `where`/`whereAll`/`whereAny` |
| `instanceOf()` | full | pass the `Class`, or reified in Kotlin |
| DSL generation | Kotlin sources | KSP processor; use `drivine4j-codegen-java` (APT) for Java views |

```java
List<PersonContext> results = JavaQueryBuilderKt
    .query(graphObjectManager, PersonContext.class)
    .filterWith(PersonContextQueryDsl.class)
    .where(dsl -> dsl.getPerson().getName().contains("Alice"))
    .limit(20)
    .loadAll();
```

Also available on the Java builder: `orderBy`, `seek` (with `PropertyReference.after(value)`),
`skip`, `loadFirst`, `deleteAll`, `match`. For Java records, prefer `@EmptyWhenAbsent` over
`@Default` for collections.

## See Also

- [SKILL.md](../SKILL.md) -- setup, version pinning, API choice
- [PersistenceManager reference](persistence-manager.md) -- manual Cypher
- [Schema and search reference](schema-and-search.md) -- indexes, `loadNearest`, `loadMatching`, `seek`
- [Multi-database reference](multi-db.md) -- engines and dialects
