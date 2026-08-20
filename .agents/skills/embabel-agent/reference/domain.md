# Domain Objects

Domain objects in Embabel are not just strongly-typed data structures — they are real objects with behavior that can be selectively exposed to LLMs and used in agent actions. This is the core of DICE (Domain-Integrated Context Engineering): domain understanding is fundamental to effective context engineering, and domain objects serve as the bridge between business domain, agent behavior, and code actions.

## Exposing Methods with @Tool

The `@Tool` annotation selectively exposes domain object methods to LLMs. Only annotated methods become callable tools; unannotated methods remain hidden regardless of visibility.

### Java Example

```java
@Entity
public class Customer {
    private String name;
    private LoyaltyLevel loyaltyLevel;
    private List<Order> orders;

    @Tool(description = "Calculate the customer's loyalty discount percentage")
    public BigDecimal getLoyaltyDiscount() {
        return loyaltyLevel.calculateDiscount(orders.size());
    }

    @Tool(description = "Check if customer is eligible for premium service")
    public boolean isPremiumEligible() {
        return orders.stream()
            .mapToDouble(Order::getTotal)
            .sum() > 1000.0;
    }

    // Unannotated — never exposed to LLMs
    public void updateLoyaltyLevel() {
        // Internal business logic
    }
}
```

### Kotlin Example

```kotlin
@Entity
class Customer(
    private val name: String,
    private val loyaltyLevel: LoyaltyLevel,
    private val orders: List<Order>
) {
    @Tool(description = "Calculate the customer's loyalty discount percentage")
    fun getLoyaltyDiscount(): BigDecimal {
        return loyaltyLevel.calculateDiscount(orders.size)
    }

    @Tool(description = "Check if customer is eligible for premium service")
    fun isPremiumEligible(): Boolean {
        return orders.sumOf { it.total } > 1000.0
    }

    // Unannotated — never exposed to LLMs
    fun updateLoyaltyLevel() {
        // Internal business logic
    }
}
```

### What Gets Exposed

| Method | Exposed? | Why |
|--------|----------|-----|
| `getLoyaltyDiscount()` with `@Tool` | Yes | Annotated |
| `isPremiumEligible()` with `@Tool` | Yes | Annotated |
| `updateLoyaltyLevel()` without `@Tool` | No | Unannotated — hidden regardless of visibility |

**Key rule:** Unannotated methods are **never** exposed to LLMs, regardless of their visibility level. This ensures tool exposure is safe, explicit, and controlled.

### Adding a Domain Object to a Prompt

```java
@Action
public Recommendation generateRecommendation(Customer customer, OperationContext context) {
    var prompt = String.format(
        "Generate a personalized recommendation for %s based on their profile",
        customer.getName()
    );

    return context.ai()
        .withToolObject(customer)   // exposes @Tool methods to the LLM
        .withDefaultLlm()
        .createObject(prompt, Recommendation.class);
}
```

```kotlin
@Action
fun generateRecommendation(customer: Customer, context: OperationContext): Recommendation {
    val prompt = "Generate a personalized recommendation for ${customer.name} based on their profile"

    return context.ai()
        .withToolObject(customer)   // exposes @Tool methods to the LLM
        .withDefaultLlm()
        .createObject(prompt, Recommendation::class.java)
}
```

The LLM gains access to `customer.getLoyaltyDiscount()` and `customer.isPremiumEligible()` as callable tools.

**Important:** Domain object methods, even if annotated with `@Tool`, will **not** be exposed to LLMs unless explicitly added via `withToolObject()`.

## Best Practices

### What to Expose

| Category | Description | Example |
|----------|-------------|---------|
| **Business Logic** | Methods that provide safely invocable business value to the LLM | `calculateDiscount()`, `isEligible()` |
| **Calculated Properties** | Methods that compute derived values the LLM might get wrong | `getLoyaltyDiscount()`, `getTotal()` |
| **Business Rules** | Methods that implement domain-specific rules | `isPremiumEligible()`, `validateOrder()` |

### General Guidelines

- **Encapsulate business logic** within domain objects where it belongs — avoid anemic DTOs
- **Expose selectively** — only methods the LLM should call get `@Tool`
- **Keep internal details hidden** — think carefully before exposing methods that mutate state or have side effects

### Benefits

- **Rich Context** — LLMs receive both data structure and behavioral context
- **Encapsulation** — business logic stays within domain objects where it belongs
- **Reusability** — domain objects can be used across multiple agents
- **Testability** — domain logic can be unit tested independently
- **Evolution** — adding new `@Tool` methods to domain objects extends agent capabilities

This approach ensures agents work with meaningful business entities rather than generic data structures, leading to more natural and effective AI interactions.
---

*Source: Embabel Agent v1.0.0 documentation*
