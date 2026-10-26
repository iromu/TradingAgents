# Step 10 - Metamodel versioning (EXPERIMENTAL)

Shape may change before 1.0. `DeclaredSchema` is declared in **`DeclaredSchemaSource.kt:47`**, not in
a `DeclaredSchema.kt` - a filename search for it returns nothing and wrongly reads as absence.

```kotlin
val version = MetamodelVersion(dictionary) {
    governedBy("Person", "Company")
    aliases {
        type("Organisation", formerly = setOf("Company"))
        property("Person", "emailAddress", formerly = setOf("email"))
    }
}
val declared = DeclaredSchema(dictionary) { governedBy("Person", "Company") }

// Java / chain form
val v = MetamodelVersion.stamping(dictionary)
    .governedBy(setOf("Person", "Company"))
    .withAliases(aliases)
    .stamp()          // or .declare()
```

Both DSL entries are `@JvmSynthetic operator fun invoke` on the companion, so Java sees only the
chain (`MetamodelVersion.from(dictionary[, selector[, aliases]])`, `DeclaredSchema.from(...)`).
Governance is per type and opt-in: adding or reshaping an **un**governed type leaves `contentHash`
untouched, so exploratory types churn without polluting version history.
`hasSameContentAs(other)` compares the hash and ignores `schemaName`; `equals` compares both.
Persist stamps through `MetamodelVersionStore` (`saveVersion` / `latestVersion` / `versionHistory`,
keyed `(schemaName, contentHash)`, upsert), with `InMemoryMetamodelVersionStore` for tests and
`dice-storage/DrivineMetamodelVersionStore.kt` for the graph backend.
Supporting types: `SchemaAliases` (`SchemaAliases.NONE`), `TypeIdentity`, `GovernedTypeSelector`
(`GovernedTypeSelector.ALL`), `MetamodelStamping`, `MetamodelDsl`, `PropertySignature`,
`ObservedSchema` / `ObservedSchemaSource`, `DriftReport` / `DriftCheckRunner`, `MetamodelDiffer`.
