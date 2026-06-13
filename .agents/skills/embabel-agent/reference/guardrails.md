# Guardrails Reference

Guardrails validate user inputs and LLM responses using configurable policies. They are an essential component in agentic AI systems.

## Concepts

- **`UserInputGuardRail`** — validates user prompts (before LLM call)
- **`AssistantMessageGuardRail`** — validates LLM responses (after LLM call)
- **`ValidationResult`** — success or failure with severity
- **`ValidationSeverity`** — `INFO`, `WARNING`, `ERROR`, `CRITICAL`

## Custom Guardrails

### User Input Guardrail

```java
public class ToxicityGuardRail implements UserInputGuardRail {
    @Override
    public String getName() { return "toxicity-filter"; }

    @Override
    public ValidationResult validate(String userInput) {
        if (isToxic(userInput)) {
            return ValidationResult.failure(ValidationSeverity.CRITICAL,
                "Input contains toxic language");
        }
        return ValidationResult.success();
    }
}
```

### Assistant Message Guardrail

```java
public class ResponseValidator implements AssistantMessageGuardRail {
    @Override
    public String getName() { return "response-validator"; }

    @Override
    public ValidationResult validate(String assistantMessage) {
        if (containsSensitiveData(assistantMessage)) {
            return ValidationResult.failure(ValidationSeverity.ERROR,
                "Response contains sensitive data");
        }
        return ValidationResult.success();
    }
}
```

## Using Guardrails

### Per-Call Guardrails

```java
context.ai().withDefaultLlm()
    .withGuardRails(new ToxicityGuardRail(), new PiiGuardRail())
    .creating(Analysis.class)
    .fromPrompt("Analyze this data");
```

### CRITICAL Blocks Execution

A `CRITICAL` severity level causes a `GuardRailViolationException` to be thrown:
- For **user input guardrails**: prevents the LLM call from executing
- For **assistant message guardrails**: wrapped inside `ThinkingResponse` when using thinking mode

### Thinking Mode Analysis

Guardrails can analyze LLM thinking blocks when object creation fails:

```java
context.ai().withDefaultLlm()
    .withGuardRails(new ReasoningGuardRail())
    .createObjectIfPossible(Answer.class)
    .fromPrompt("Ambiguous question");
```

When the LLM cannot provide a definitive answer, guardrails can automate further analysis of the reasoning.

## Message Combining

In multi-turn conversations, guardrails receive a list of messages:

### Default Behavior

Messages joined with newlines:
```
Hello
How are you?
Tell me about X
```

### Custom Combining

```java
public class AuditGuardRail implements UserInputGuardRail {
    @Override
    public String combineMessages(List<UserMessage> messages) {
        return IntStream.range(0, messages.size())
            .mapToObj(i -> "[%d] %s".formatted(i, messages.get(i).getContent()))
            .collect(Collectors.joining("\n"));
    }
}
```

## Global Guardrails Configuration

### Property Configuration

```yaml
embabel:
  agent:
    platform:
      guardrails:
        user-input: com.example.ToxicityGuardRail,com.example.PiiGuardRail
        assistant-message: com.example.ResponseValidator
        fail-on-error: true
```

### POJO Guardrails

Property-registered guardrails are **plain POJOs**, not Spring beans:
- `@Autowired`, `@Value`, constructor injection **do not work**
- `@PostConstruct` / `@PreDestroy` lifecycle callbacks **are not invoked**
- Instantiate via `BeanUtils.instantiateClass(…)`

### Accessing Spring Beans from POJO Guardrails

```java
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    }
}

public class CostGuardRail implements UserInputGuardRail {
    @Override
    public ValidationResult validate(String input) {
        // Resolve lazily at validation time, not in constructor
        var costService = SpringContextHolder.getContext()
            .getBean(CostService.class);
        if (costService.getCurrentCost() > MAX_COST) {
            return ValidationResult.failure(ValidationSeverity.CRITICAL, "Budget exceeded");
        }
        return ValidationResult.success();
    }
}
```

### Merging with Per-Call Guardrails

- Global guardrails run first, then per-call guardrails
- Duplicates removed by **class identity** (keeps the global instance)
- A class registered both globally and per-call is invoked only once

### Strict Mode

```yaml
embabel:
  agent:
    platform:
      guardrails:
        fail-on-error: true  # Fail startup if any guardrail can't be instantiated
```

## Programmatic Access

```java
@Autowired
GlobalGuardRailsRegistry registry;

// Add guardrails programmatically
registry.addUserInputGuardRail(new ToxicityGuardRail());

// Get all registered guardrails
List<UserInputGuardRail> guardrails = registry.getUserInputGuardRails();
```

## Relationship with Other Validation

| Mechanism | Purpose | Example |
|-----------|---------|---------|
| **Guardrails** | Content safety/compliance | Toxicity, PII, budget |
| **Bean Validation (JSR-380)** | Data structure validity | `@NotNull`, `@Size` on domain objects |
| **@SecureAgentTool** | Access control | `hasAuthority('admin:read')` |

Guardrails and bean validation are complementary. `@SecureAgentTool` is orthogonal — it enforces access control, not content safety.

## Key Points

- Guardrails can access the `Blackboard` for context-aware validation
- `CRITICAL` severity blocks execution for user input guardrails
- Global guardrails apply to every LLM call; per-call guardrails are additive
- POJO guardrails registered via properties cannot use Spring injection — use `ApplicationContextAware` holder
- `fail-on-error: true` treats missing guardrails as deployment errors
