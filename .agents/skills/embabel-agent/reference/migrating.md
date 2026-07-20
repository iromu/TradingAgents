# Migration Guide

How to migrate from popular AI frameworks to Embabel. See SKILL.md for the core workflow.

## Migrating from CrewAI

CrewAI uses a collaborative multi-agent approach. Embabel provides similar capabilities with stronger type safety and better integration with existing Java/Kotlin codebases.

### Core Concept Mapping

| CrewAI Concept | Embabel Equivalent | Notes |
|----------------|--------------------|-------|
| Agent Role/Goal/Backstory | `RoleGoalBackstory` PromptContributor | Convenience class for agent personality |
| Sequential Tasks | Typed data flow between actions | Type-driven execution with automatic planning |
| Crew (Multi-agent coordination) | Actions with shared PromptContributors | Agents can adopt personalities as needed |
| YAML Configuration | Standard Spring `@ConfigurationProperties` backed by `application.yml` | Type-safe configuration with validation |

### Migration Example

**CrewAI Pattern:**

```python
research_agent = Agent(
    role='Research Specialist',
    goal='Find comprehensive information',
    backstory='Expert researcher with 10+ years experience'
)

writer_agent = Agent(
    role='Content Writer',
    goal='Create engaging content',
    backstory='Professional writer specializing in technical content'
)

crew = Crew(
    agents=[research_agent, writer_agent],
    tasks=[research_task, write_task],
    process=Process.sequential
)
```

**Embabel Equivalent:**

```java
@ConfigurationProperties("examples.book-writer")
record BookWriterConfig(
    LlmOptions researcherLlm,
    LlmOptions writerLlm,
    RoleGoalBackstory researcher,
    RoleGoalBackstory writer
) {}

@Agent(description = "Write a book by researching, outlining, and writing chapters")
public record BookWriter(BookWriterConfig config) {

    @Action
    ResearchReport researchTopic(BookRequest request, OperationContext context) {
        return context.ai()
            .withLlm(config.researcherLlm())
            .withPromptElements(config.researcher(), request)
            .withToolGroup(CoreToolGroups.WEB)
            .createObject("Research the topic thoroughly...", ResearchReport.class);
    }

    @Action
    BookOutline createOutline(BookRequest request, ResearchReport research, OperationContext context) {
        return context.ai()
            .withLlm(config.writerLlm())
            .withPromptElements(config.writer(), request, research)
            .createObject("Create a book outline...", BookOutline.class);
    }

    @Goal
    @Action
    Book writeBook(BookRequest request, BookOutline outline, OperationContext context) {
        var chapters = context.parallelMap(outline.chapterOutlines(),
            config.maxConcurrency(),
            chapterOutline -> writeChapter(request, outline, chapterOutline, context));
        return new Book(request, outline.title(), chapters);
    }
}
```

**Key Advantages:**

- **Type Safety**: Compile-time validation of data flow
- **Spring Integration**: Leverage existing enterprise infrastructure
- **Automatic Planning**: GOAP planner handles task sequencing and sophisticated planning
- **Tool Integration with the JVM**: Native access to existing Java/Kotlin services

## Migrating from Pydantic AI

Pydantic AI provides a Python framework for building AI agents with type safety and validation. Embabel offers similar capabilities in the JVM ecosystem.

### Core Concept Mapping

| Pydantic AI Concept | Embabel Equivalent | Notes |
|---------------------|--------------------|-------|
| `@system_prompt` decorator | PromptContributor classes | More flexible and composable prompt management |
| `@tool` decorator | `@Tool` annotated methods on agent classes or domain objects | |
| Agent class | `@Agent` annotated record/class | Declarative agent definition with Spring integration |
| RunContext | Blackboard state via `OperationContext` | Normally not a concern for user code |
| SystemPrompt | Custom `PromptContributor` | Structured prompt contribution system |
| deps parameter | Spring dependency injection | |

### Migration Example

**Pydantic AI Pattern:**

```python
@system_prompt
def support_prompt() -> str:
    return "You are a support agent in our bank"

@tool
async def get_customer_balance(customer_id: int, include_pending: bool = False) -> float:
    customer = find_customer(customer_id)
    return customer.balance + (customer.pending if include_pending else 0)

agent = Agent(
    'openai:gpt-4-mini',
    system_prompt=support_prompt,
    tools=[get_customer_balance],
)

result = agent.run("What's my balance?", deps={'customer_id': 123})
```

**Embabel Equivalent:**

```java
record Customer(Long id, String name, float balance, float pendingAmount) {

    @Tool(description = "Find the balance of a customer by id")
    float balance(boolean includePending) {
        return includePending ? balance + pendingAmount : balance;
    }
}

record SupportInput(
    @JsonPropertyDescription("Customer ID") Long customerId,
    @JsonPropertyDescription("Query from the customer") String query) {
}

record SupportOutput(
    @JsonPropertyDescription("Advice returned to the customer") String advice,
    @JsonPropertyDescription("Whether to block their card or not") boolean blockCard,
    @JsonPropertyDescription("Risk level of query") int risk) {
}

@Agent(description = "Customer support agent")
record SupportAgent(CustomerRepository customerRepository) {

    @Goal(description = "Help bank customer with their query")
    @Action
    SupportOutput supportCustomer(SupportInput supportInput, OperationContext context) {
        var customer = customerRepository.findById(supportInput.customerId());
        if (customer == null) {
            return new SupportOutput("Customer not found with this id", false, 0);
        }
        return context.ai()
            .withLlm(OpenAiModels.GPT_41_MINI)
            .withToolObject(customer)
            .createObject(
                """
                You are a support agent in our bank, give the
                customer support and judge the risk level of their query.
                In some cases, you may need to block their card. In this case, explain why.
                Reply using the customer's name, "%s".
                Currencies are in $.

                Their query: [%s]
                """.formatted(customer.name(), supportInput.query()),
                SupportOutput.class);
    }
}
```

**Key Advantages:**

- **Enterprise Integration**: Native Spring Boot integration with existing services
- **Compile-time Safety**: Strong typing catches errors at build time
- **Automatic Planning**: GOAP planner handles complex multi-step operations
- **JVM Ecosystem**: Access to mature libraries and enterprise infrastructure

## Migrating from LangGraph

LangGraph builds agent workflows using a state machine. For a detailed comparison of common patterns between LangGraph and Embabel, see the [Embabel vs LangGraph blog post](https://medium.com/@springrod/build-better-agents-in-java-vs-python-embabel-vs-langgraph-f7951a0d855c).

Key differences:

| LangGraph | Embabel |
|-----------|---------|
| Explicit state graph edges | Type-driven data flow between actions |
| Manual node execution | GOAP planner auto-generates execution plans |
| Pydantic state models | Java records / Kotlin classes with `@State` |
| Python tool functions | `@Tool` / `@LlmTool` JVM methods |
| Checkpointing for persistence | Blackboard + context IDs for session state |

TBD.---

*Source: Embabel Agent v1.0.0 documentation*
