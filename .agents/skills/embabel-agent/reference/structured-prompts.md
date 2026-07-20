# Structured Prompts

Embabel provides **prompt contributors** — a way to structure, inject, and reuse content across LLM prompts. You can build prompts as plain strings, but contributors give you consistency and composability across actions and agents.

---

## PromptContributor

All contributors implement `PromptContributor` with a single method:

```java
public interface PromptContributor {
    String contribution();
}
```

Add them to a `PromptRunner` via `withPromptContributor()`:

```java
context.ai().withDefaultLlm()
    .withPromptContributor(persona)
    .creating(Analysis.class)
    .fromPrompt("Analyze the data");
```

---

## Persona

`Persona` gives a structured way to define an agent's personality, voice, and objective.

```java
var persona = Persona.create(
    "Alex the Analyst",
    "A detail-oriented data analyst with expertise in financial markets",
    "Professional yet approachable, uses clear explanations",
    "Help users understand complex financial data through clear analysis"
);
```

**Generated prompt contribution:**

```
You are Alex the Analyst.
Your persona: A detail-oriented data analyst with expertise in financial markets.
Your objective is Help users understand complex financial data through clear analysis.
Your voice: Professional yet approachable, uses clear explanations.
```

---

## RoleGoalBackstory

Follows the Crew AI pattern — useful for migration or familiar task framing.

```java
var agent = RoleGoalBackstory.withRole("Senior Software Engineer")
    .andGoal("Write clean, maintainable code")
    .andBackstory("10+ years experience in enterprise software development");
```

**Generated prompt contribution:**

```
Role: Senior Software Engineer
Goal: Write clean, maintainable code
Backstory: 10+ years experience in enterprise software development
```

---

## Building Structured Prompts

### Custom PromptContributor

Implement the interface directly for full control over prompt content:

```java
public class DomainContext implements PromptContributor {
    private final String domainContext;

    public DomainContext(String domainContext) {
        this.domainContext = domainContext;
    }

    @Override
    public String contribution() {
        return "Domain Context:\n" + domainContext;
    }
}
```

### Conditional PromptContributor

Include content only when a condition is met:

```java
public class ConditionalPrompt implements PromptContributor {
    private final Supplier<Boolean> condition;
    private final String trueContent;
    private final String falseContent;

    public ConditionalPrompt(Supplier<Boolean> condition,
                             String trueContent,
                             String falseContent) {
        this.condition = condition;
        this.trueContent = trueContent;
        this.falseContent = falseContent;
    }

    @Override
    public String contribution() {
        return condition.get() ? trueContent : falseContent;
    }
}
```

### LlmReference (Content + Tools)

`LlmReference` extends `PromptContributor` to also expose `@Tool` methods. Use it when you need both prompt content and tools from the same object:

```java
// Programmatic creation
var reference = new LlmReference("git-tools", "Git operations") {
    @Tool
    public String getFile(String path) { ... }
};

context.ai().withDefaultLlm()
    .withReference(reference)
    .creating(Result.class)
    .fromPrompt("Get the file from the repo");
```

**When to use `LlmReference` vs `PromptContributor`:**

| Use `LlmReference` | Use `PromptContributor` |
|---|---|
| Need to provide content AND tools | Just need to inject text |
| Want specific tool usage instructions | Simple static content |
| Data may be content or tools depending on context | |

### YML-based LlmReference Providers

Define references in `references.yml` (under `src/main/resources/`):

```yaml
- fqn: com.embabel.agent.api.reference.LiteralText
  name: domain-context
  description: Domain context for agents
  notes: |
    Here is the domain context...
- fqn: com.embabel.agent.api.reference.WebPage
  name: api-docs
  description: API documentation
  url: https://api.example.com/docs
```

Load them programmatically:

```java
List<LmmReference> references = LlmReferenceProviders.fromYml("references.yml");
```

**Built-in providers:**

| Provider | Purpose |
|---|---|
| `LiteralText` | Static text via `notes` field |
| `SpringResource` | Contents of a Spring resource path |
| `WebPage` | Content of a fetchable web page |
| `GitHubRepository` | GitHub repositories (`embabel-agent-code`) |
| `ApiReferenceProvider` | API from classpath (`embabel-agent-code`) |

---

## Best Practices

- Keep contributors focused and single-purpose
- Use `Persona` and `RoleGoalBackstory` for common patterns
- Implement custom `PromptContributor` for domain-specific content
- Use `LlmReference` when you need both content and tools from the same source
- Test contributions to verify desired LLM behavior
---

*Source: Embabel Agent v1.0.0 documentation*
