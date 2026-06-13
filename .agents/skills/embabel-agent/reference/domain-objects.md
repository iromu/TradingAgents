# Domain Objects Reference

Domain objects in Embabel are not just strongly-typed data structures — they are real objects with behavior that can be selectively exposed to LLMs and used in agent actions.

## DICE: Domain-Integrated Context Engineering

Domain understanding is fundamental to effective context engineering. Domain objects serve as the bridge between:
- **Business Domain** — Real-world entities and their relationships
- **Agent Behavior** — How LLMs understand and interact with the domain
- **Code Actions** — Traditional programming logic that operates on domain objects

Avoid the anemic domain model. Domain objects should encapsulate business logic and expose it selectively.

## @Tool: Selective Tool Exposure

```java
public record Customer(long id, String name, double balance) {
    @Tool
    public double getLoyaltyDiscount() {
        return balance > 1000 ? 0.15 : 0.05;
    }

    // Unannotated methods are never exposed to LLMs
    void updateLoyaltyLevel() { ... }
}
```

### Rules

- `@Tool` methods can have any visibility, static or instance scope
- Return type must be serializable
- Not supported: Optional, async types, reactive types, functional types
- Tools can be stateful — often encapsulate domain objects with private state
- Unannotated methods are **never** exposed to LLMs, regardless of visibility

### Adding to Prompts

```java
context.ai().withDefaultLlm()
    .withToolObject(customer)
    .creating(Order.class)
    .fromPrompt("Create an order for this customer");
```

The LLM can call `customer.getLoyaltyDiscount()` as a tool.

## Domain Objects in Actions

Domain objects can be used naturally in action methods:

```java
@Action
public Order createOrder(Customer customer, Ai ai) {
    // customer is available as a tool AND as a parameter
    return ai.withDefaultLlm()
        .withToolObject(customer)  // expose @Tool methods
        .creating(Order.class)
        .fromPrompt("Create an order for this customer");
}
```

## Domain Tools

Tools from `@Tool` methods on domain objects are automatically available when the object is added via `withToolObject()`.

## Domain Understanding Best Practices

- **Encapsulate business logic** within domain objects where it belongs
- **Expose selectively** — only methods the LLM should call get `@Tool`
- **Keep internal details hidden** — think carefully before exposing methods that mutate state
- **Design for toolability** — consider which methods should be callable by LLMs
- **Reuse across agents** — domain objects work across multiple agents

## Key Points

- Domain objects carry both data and behavior (not anemic DTOs)
- `@Tool` annotation selectively exposes methods to LLMs
- Unannotated methods stay hidden regardless of visibility
- Tools can be stateful — domain objects with private state are common
- Use `withToolObject()` to make `@Tool` methods available to the LLM
- Domain objects drive planning through type-based blackboard binding
