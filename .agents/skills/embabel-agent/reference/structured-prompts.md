# Structured Prompts Reference

Embabel provides structured ways to organize and inject content into LLM prompts.

## PromptContributor

`PromptContributor` is a fundamental way to inject reusable content into prompts:

```java
public interface PromptContributor {
    String contribution();
}
```

Add to a `PromptRunner`:

```java
context.ai().withDefaultLlm()
    .withPromptContributor(new Persona("Alex", "Data analyst.", "Help users.", "Professional."))
    .creating(Analysis.class)
    .fromPrompt("Analyze the data");
```

### Built-in Convenience Classes

**Persona** — Define an AI agent's personality:

```java
var persona = new Persona(
    "Alex the Analyst",
    "A detail-oriented data analyst with expertise in financial markets.",
    "Help users understand complex financial data through clear analysis.",
    "Professional yet approachable, uses clear explanations."
);
```

Generates:
```
You are Alex the Analyst.
Your persona: A detail-oriented data analyst with expertise in financial markets.
Your objective is Help users understand complex financial data through clear analysis.
Your voice: Professional yet approachable, uses clear explanations.
```

**RoleGoalBackstory** — Follows the Crew AI pattern:

```java
var rgb = new RoleGoalBackstory(
    "Senior Software Engineer",
    "Write clean, maintainable code",
    "10+ years experience in enterprise software development"
);
```

Generates:
```
Role: Senior Software Engineer
Goal: Write clean, maintainable code
Backstory: 10+ years experience in enterprise software development
```

### Custom PromptContributor

```java
var custom = new PromptContributor() {
    @Override
    public String contribution() {
        return "Here is the domain context: " + domainContext;
    }
};
```

## LlmReference

A subinterface of `PromptContributor` that also provides tools via annotated `@Tool` methods:

```java
public interface LlmReference extends PromptContributor {
    String getName();
    String getDescription();
}
```

Add via `withReference()`:

```java
var reference = new LlmReference("git-repo", "GitHub repository tools") {
    @Tool
    public String getFile(String path) { ... }
};

context.ai().withDefaultLlm()
    .withReference(reference)
    .creating(Result.class)
    .fromPrompt("Get the file from the repo");
```

### When to Use LlmReference vs PromptContributor

| Use LlmReference | Use PromptContributor |
|------------------|----------------------|
| Need to provide both content AND tools | Just need to inject text |
| Want specific instructions on tool usage | Simple text injection |
| Data may be best as tools or content depending on context | Static content |

### Built-in LlmReference Providers

- `LiteralText` — Text in `notes` field
- `SpringResource` — Contents of a Spring resource path
- `WebPage` — Content of a fetchable web page
- `GitHubRepository` — GitHub repositories (`embabel-agent-code` module)
- `ApiReferenceProvider` — API from classpath (`embabel-agent-code` module)

### YML Configuration

Define references in `references.yml`:

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

Parse with:

```java
List<LlmReference> references = LlmReferenceProviders.fromYml("references.yml");
```

## Best Practices

- Keep prompt contributors focused and single-purpose
- Use convenience classes (`Persona`, `RoleGoalBackstory`) when they fit
- Implement custom `PromptContributor` for domain-specific requirements
- Consider dynamic contributors for context-dependent content
- Test prompt contributions to verify desired LLM behavior
- Use `LlmReference` when you need both content and tools from the same source
