# IDE Tooling — IntelliJ IDEA Plugin

The Embabel Agent IntelliJ IDEA plugin eliminates false "never used" warnings on framework-annotated methods.

## What It Does

IntelliJ doesn't know that `@Action`, `@Condition`, and `@Cost` methods are invoked reflectively at runtime by the Embabel framework. The plugin registers an `ImplicitUsageProvider` that tells the IDE these methods are implicitly used.

**Without the plugin:**
```kotlin
@Action  // ⚠ IntelliJ: "Method 'summarize' is never used"
fun summarize(article: RawArticle): ArticleSummary { ... }
```

**With the plugin:**
```kotlin
@Action  // ✅ No warning
fun summarize(article: RawArticle): ArticleSummary { ... }
```

## Installation

Published on the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31142-embabel-agent) (ID: `31142`).

1. Open IntelliJ IDEA → *Settings* → *Plugins* → *Marketplace*
2. Search for **Embabel Agent**
3. Click *Install*, restart when prompted

## Compatibility

| Requirement | Value |
|-------------|-------|
| Minimum IDEA | 2023.3 (build 233) |
| Maximum IDEA | No upper cap |
| JVM | 21+ |
| Plugin ID | `com.embabel.agent.intellij-plugin` |

Works with both Community and Ultimate editions (merged into a single distribution as of IDEA 2025.3).

## Source

Plugin source: https://github.com/embabel/embabel-agent-intellij

---
*Source: Embabel Agent v1.5.0 documentation*
