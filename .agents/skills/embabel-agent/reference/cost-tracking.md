# Cost Tracking Reference

Embabel emits events for every LLM and embedding call, enabling real-time cost tracking and budget management.

## Events

### LlmInvocationEvent

Emitted once per LLM call:

```java
public class CostTrackingListener implements AgenticEventListener {
    @EventListener
    public void onLlmCall(LlmInvocationEvent event) {
        String model = event.getInvocation().getLlmMetadata().getModel();
        String provider = event.getInvocation().getLlmMetadata().getProvider();
        int inputTokens = event.getInvocation().getUsage().getInputTokens();
        int outputTokens = event.getInvocation().getUsage().getOutputTokens();
        double cost = event.getInvocation().getCost();
        String interactionId = event.getInteractionId().getValue();
        String processId = event.getAgentProcess().getId();
        String agentName = event.getAgentProcess().getAgent().getName();

        // Track cost by process, tenant, user, etc.
    }
}
```

### EmbeddingInvocationEvent

Emitted once per embedding call:

```java
@EventListener
public void onEmbedding(EmbeddingInvocationEvent event) {
    String model = event.getEmbeddingMetadata().getModel();
    double cost = event.getEmbeddingInvocation().getCost();
    // Track embedding costs similarly
}
```

## Cost Tracking by Dimension

### By Process

```java
public class ProcessCostListener implements AgenticEventListener {
    private final ConcurrentHashMap<String, Double> costs = new ConcurrentHashMap<>();

    @EventListener
    public void onLlmCall(LlmInvocationEvent event) {
        String processId = event.getAgentProcess().getId();
        costs.merge(processId, event.getInvocation().getCost(), Double::sum);
    }

    public double getProcessCost(String processId) {
        return costs.getOrDefault(processId, 0.0);
    }
}
```

### By Tenant

```java
public class TenantCostListener implements AgenticEventListener {
    private final ConcurrentHashMap<String, Double> tenantCosts = new ConcurrentHashMap<>();

    @EventListener
    public void onLlmCall(LlmInvocationEvent event) {
        String tenantId = event.getToolCallContext().get("tenantId");
        if (tenantId != null) {
            tenantCosts.merge(tenantId, event.getInvocation().getCost(), Double::sum);
        }
    }
}
```

## Budget Guardrail Pattern

Combine cost tracking events with guardrails to cap spending:

### Step 1: Cost Listener

```java
public class CostTrackingListener implements AgenticEventListener {
    private final ConcurrentHashMap<String, Double> costs = new ConcurrentHashMap<>();

    @EventListener
    public void onLlmCall(LlmInvocationEvent event) {
        String processId = event.getAgentProcess().getId();
        costs.merge(processId, event.getInvocation().getCost(), Double::sum);
    }

    public double getCost(String processId) {
        return costs.getOrDefault(processId, 0.0);
    }
}
```

### Step 2: Budget Guardrail

```java
public class BudgetGuardRail implements UserInputGuardRail {
    private final CostTrackingListener costListener;
    private final double maxCost;

    public BudgetGuardRail(CostTrackingListener costListener, double maxCost) {
        this.costListener = costListener;
        this.maxCost = maxCost;
    }

    @Override
    public ValidationResult validate(String input) {
        String processId = getCurrentProcessId(); // from context
        if (costListener.getCost(processId) > maxCost) {
            return ValidationResult.failure(ValidationSeverity.CRITICAL,
                "Budget exceeded: $" + costListener.getCost(processId));
        }
        return ValidationResult.success();
    }
}
```

### Step 3: Register Guardrail

```java
@Bean
public BudgetGuardRail budgetGuardRail(CostTrackingListener costListener) {
    return new BudgetGuardRail(costListener, 10.0); // $10 limit
}
```

## Early Termination Policy

For a hard cap on the agent process itself:

```java
var options = ProcessOptions.builder()
    .withEarlyTerminationPolicy(new EarlyTerminationPolicy() {
        @Override
        public boolean shouldTerminate(AgentProcess process) {
            return costListener.getCost(process.getId()) > 1.0; // $1 cap
        }
    })
    .build();
```

## Key Points

- Cost events fire **after** the call completes — they cannot stop the call that just ran
- Use a listener to accumulate costs, then a guardrail to block the **next** call
- For hard process-level caps, use `EarlyTerminationPolicy`
- Cost tracking is event-based — subscribe via `AgenticEventListener`
- Use thread-safe data structures for accumulated state (concurrent processes)
- Group by `agentProcess.id` for per-process tracking, or by tenant/user for multi-tenant scenarios
