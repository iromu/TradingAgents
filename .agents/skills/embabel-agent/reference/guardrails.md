# Guardrails Reference

Guardrails validate user inputs and LLM responses using configurable policies. Embabel provides a framework for building custom guardrails that integrate with the `PromptRunner` API.

## Adding Guardrails

Register guardrails on a `PromptRunner` via `withGuardRails()`, which is chainable:

```java
PromptRunner runner = ai.withDefaultLlm()
    .withGuardRails(new MyGuardRail());
```

```kotlin
val runner = ai.withDefaultLlm()
    .withGuardRails(MyGuardRail())
```

Two guardrail interfaces:

- **`UserInputGuardRail`** — validates user prompts *before* the LLM call
- **`AssistantMessageGuardRail`** — validates LLM responses *after* the LLM call

Both receive a `Blackboard` object for context-aware validation.

## Severity Levels

Guardrail validation returns a `ValidationResult` with a list of `ValidationError` objects. Each error carries a `ValidationSeverity`:

| Severity | Behavior |
|----------|----------|
| `CRITICAL` | Throws `GuardRailViolationException`, preventing the LLM operation from executing |
| `WARN` | Logs a warning; execution continues |
| `INFO` | Logs an info message; execution continues |

Errors are sorted by severity and logged accordingly.

### Basic Guardrail

```java
class MyGuardRail implements UserInputGuardRail {
    @Override public String getName() { return "my-guardrail"; }
    @Override public String getDescription() { return "Validates user input"; }

    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        if (input.contains("forbidden")) {
            return new ValidationResult(false, List.of(
                new ValidationError("forbidden", "Contains forbidden word",
                    ValidationSeverity.CRITICAL)
            ));
        }
        return ValidationResult.VALID;
    }
}
```

```kotlin
class MyGuardRail : UserInputGuardRail {
    override val name = "my-guardrail"
    override val description = "Validates user input"

    override fun validate(input: String, blackboard: Blackboard): ValidationResult {
        if (input.contains("forbidden")) {
            return ValidationResult(false, listOf(
                ValidationError("forbidden", "Contains forbidden word",
                    ValidationSeverity.CRITICAL)
            ))
        }
        return ValidationResult.VALID
    }
}
```

### Multiple Guardrails with Different Severities

```java
ai.withDefaultLlm()
    .withGuardRails(
        new CriticalGuardRail(),  // CRITICAL — blocks on violation
        new WarningGuardRail(),   // WARN — logs, continues
        new InfoGuardRail()       // INFO — logs, continues
    );
```

```kotlin
ai.withDefaultLlm()
    .withGuardRails(
        CriticalGuardRail(),  // CRITICAL — blocks on violation
        WarningGuardRail(),   // WARN — logs, continues
        InfoGuardRail()       // INFO — logs, continues
    )
```

## Built-in Guardrails

Embabel provides the framework for building custom guardrails. For vendor-supported guardrails, see:

- [Guardrails Hub](https://guardrailsai.com/docs)
- [Amazon Bedrock Guardrails](https://docs.aws.amazon.com/bedrock/latest/userguide/guardrails.html)

Common guardrail types to implement:

- **Prompt injection detection** — block adversarial prompts
- **Jailbreak detection** — prevent attempts to bypass safety policies
- **PII detection** — identify and redact personally identifiable information
- **Toxicity filtering** — block toxic or harmful language

## Custom Guardrails

### User Input Guardrail

```java
class PiiGuardRail implements UserInputGuardRail {
    @Override public String getName() { return "pii-detector"; }
    @Override public String getDescription() { return "Detects PII in user input"; }
    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        if (containsPii(input)) {
            return new ValidationResult(false, List.of(
                new ValidationError("pii-detected", "Input contains PII",
                    ValidationSeverity.WARN)));
        }
        return ValidationResult.VALID;
    }
}
```

```kotlin
class PiiGuardRail : UserInputGuardRail {
    override val name = "pii-detector"
    override val description = "Detects PII in user input"
    override fun validate(input: String, blackboard: Blackboard): ValidationResult {
        if (containsPii(input)) {
            return ValidationResult(false, listOf(
                ValidationError("pii-detected", "Input contains PII",
                    ValidationSeverity.WARN)))
        }
        return ValidationResult.VALID
    }
}
```

### Assistant Message Guardrail

Two `validate` overloads: one for structured output (`String`), one for thinking mode (`ThinkingResponse`):

```java
class ResponseGuardRail implements AssistantMessageGuardRail {
    @Override public String getName() { return "response-guardrail"; }
    @Override public String getDescription() { return "Validates LLM responses"; }

    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        if (containsSensitiveData(input)) {
            return new ValidationResult(false, List.of(
                new ValidationError("sensitive", "Contains sensitive data",
                    ValidationSeverity.CRITICAL)));
        }
        return ValidationResult.VALID;
    }

    @Override
    public ValidationResult validate(ThinkingResponse<?> response, Blackboard blackboard) {
        logger.info("Validating thinking blocks: {}", response.getThinkingBlocks());
        return new ValidationResult(true, Collections.emptyList());
    }
}
```

```kotlin
class ResponseGuardRail : AssistantMessageGuardRail {
    override val name = "response-guardrail"
    override val description = "Validates LLM responses"

    override fun validate(input: String, blackboard: Blackboard): ValidationResult {
        if (containsSensitiveData(input)) {
            return ValidationResult(false, listOf(
                ValidationError("sensitive", "Contains sensitive data",
                    ValidationSeverity.CRITICAL)))
        }
        return ValidationResult.VALID
    }

    override fun validate(response: ThinkingResponse<*>, blackboard: Blackboard): ValidationResult {
        logger.info("Validating thinking blocks: {}", response.thinkingBlocks)
        return ValidationResult(true, emptyList())
    }
}
```

### Custom Message Combining

In multi-turn conversations, each `UserInputGuardRail` receives a list of messages. Override `combineMessages` to control how they are joined:

```java
class AuditGuardRail implements UserInputGuardRail {
    @Override public String getName() { return "audit-guard"; }
    @Override public String getDescription() { return "Logs conversation with message markers"; }

    @Override
    public String combineMessages(List<UserMessage> userMessages) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < userMessages.size(); i++) {
            if (i > 0) result.append("\n");
            result.append("[Turn ").append(i + 1).append("]: ")
                  .append(userMessages.get(i).getContent());
        }
        return result.toString();
    }

    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        logger.info("Audit trail: {}", input);
        return ValidationResult.VALID;
    }
}
```

The default implementation joins messages with newlines.

## Error Handling

A `CRITICAL` severity on a user input guardrail throws `GuardRailViolationException`:

```java
try {
    String result = ai.withDefaultLlm().withGuardRails(new CriticalGuardRail())
        .create("What is 2+2?", String.class);
} catch (GuardRailViolationException ex) {
    logger.error("Guardrail blocked execution: {}", ex.getMessage());
}
```

```kotlin
try {
    val result = ai.withDefaultLlm().withGuardRails(CriticalGuardRail())
        .create("What is 2+2?", String::class.java)
} catch (ex: GuardRailViolationException) {
    logger.error("Guardrail blocked execution: {}", ex.message)
}
```

In thinking mode, `GuardRailViolationException` is wrapped inside `ThinkingResponse`. `createObjectIfPossible` handles exceptions gracefully.

## Global Guardrails

Declare guardrails globally via `application.properties`:

```properties
embabel.agent.guardrails.user-input=com.example.ProfanityFilter,com.example.LengthValidator
embabel.agent.guardrails.assistant-message=com.example.OutputValidator
embabel.agent.guardrails.fail-on-error=false
```

| Rule | Detail |
|------|--------|
| **Order** | Global first, then per-call |
| **Dedup** | By class identity (not `name`); global instance kept |
| **POJOs** | Plain POJOs — `@Autowired` does not work |
| **Strict** | `fail-on-error=true` fails startup on instantiation failure |

### Accessing Spring Beans from POJO Guardrails

Use an `ApplicationContextAware` holder to resolve dependencies lazily at validation time:

```java
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static volatile ApplicationContext context;
    @Override public void setApplicationContext(ApplicationContext ctx) { context = ctx; }
    public static ApplicationContext context() { return context; }
    public static <T> T getBean(Class<T> type) { return context != null ? context.getBean(type) : null; }
}
```

```java
public class CostGuardRail implements UserInputGuardRail {
    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        var costService = SpringContextHolder.getBean(CostService.class);
        if (costService.getCurrentCost() > MAX_COST) {
            return new ValidationResult(false, List.of(
                new ValidationError("budget", "Budget exceeded",
                    ValidationSeverity.CRITICAL)));
        }
        return ValidationResult.VALID;
    }
}
```

Resolve dependencies **inside `validate()`**, not in the constructor — the holder may not be wired yet at construction time.

## Programmatic Access

```java
@Autowired GlobalGuardRailsRegistry registry;

// Static access
GlobalGuardRailsRegistry registry = GlobalGuardRailsRegistry.get();
List<UserInputGuardRail> userGuards = GlobalGuardRailsRegistry.getUserInputGuardRails();
List<AssistantMessageGuardRail> assistantGuards = GlobalGuardRailsRegistry.getAssistantMessageGuardRails();
```

## Relationship with Other Validation

| Mechanism | Purpose | Example |
|-----------|---------|---------|
| **Guardrails** | Content safety/compliance | Toxicity, PII, budget |
| **Bean Validation** | Data structure validity | `@NotNull`, `@Size` |
| **@SecureAgentTool** | Access control | `hasAuthority('admin:read')` |

Guardrails and bean validation are complementary; `@SecureAgentTool` is orthogonal -- it enforces access control, not content safety.
---

*Source: Embabel Agent v1.0.0 documentation*
