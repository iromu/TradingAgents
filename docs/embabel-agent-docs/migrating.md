Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.40. Migrating from other frameworks
Many people start their journey with Python frameworks.This section covers how to migrate from popular frameworks when it’s time to use a more robust and secure platform with access to existing code and services.
#### 4.40.1. Migrating from CrewAI
CrewAI uses a collaborative multi-agent approach where agents work together on tasks.
Embabel provides similar capabilities with stronger type safety and better integration with existing Java/Kotlin codebases.
##### Core Concept Mapping

| CrewAI Concept | Embabel Equivalent | Notes || **Agent Role/Goal/Backstory** | `RoleGoalBackstory` PromptContributor | Convenience class for agent personality || **Sequential Tasks** | Typed data flow between actions | Type-driven execution with automatic planning || **Crew (Multi-agent coordination)** | Actions with shared PromptContributors | Agents can adopt personalities as needed || **YAML Configuration** | Standard Spring `@ConfigurationProperties` backed by `application.yml` or profile-specific configuration files | Type-safe configuration with validation |

##### Migration Example
**CrewAI Pattern:****Embabel Equivalent:****Key Advantages:**
- **Type Safety**: Compile-time validation of data flow
- **Spring Integration**: Leverage existing enterprise infrastructure
- **Automatic Planning**: GOAP planner handles task sequencing, and is capable of more sophisticated planning
- **Tool Integration with the JVM**: Native access to existing Java/Kotlin services
#### 4.40.2. Migrating from Pydantic AI
Pydantic AI provides a Python framework for building AI agents with type safety and validation.
Embabel offers similar capabilities in the JVM ecosystem with stronger integration into enterprise environments.
##### Core Concept Mapping

| Pydantic AI Concept | Embabel Equivalent | Notes || **@system_prompt decorator** | PromptContributor classes | More flexible and composable prompt management || **@tool decorator** | Equivalent `@Tool` annotated methods can be included on agent classes and domain objects | **Agent class** || `@Agent` annotated record/class | Declarative agent definition with Spring integration | **RunContext** || Blackboard state, accessible via `OperationContext` but normally not a concern for user code | **SystemPrompt** | Custom `PromptContributor` || Structured prompt contribution system | **deps parameter** | Spring dependency injection |

##### Migration Example
**Pydantic AI Pattern:****Embabel Equivalent:****Key Advantages:**
- **Enterprise Integration**: Native Spring Boot integration with existing services
- **Compile-time Safety**: Strong typing catches errors at build time
- **Automatic Planning**: GOAP planner handles complex multi-step operations
- **JVM Ecosystem**: Access to mature libraries and enterprise infrastructure
#### 4.40.3. Migrating from LangGraph
LangGraph builds agent workflows using a state machine.See the blog Build Better Agents in Java vs Python: Embabel vs LangGraph
for a detailed comparison of common patterns between LangGraph and Embabel.
#### 4.40.4. Migrating from Google ADK
tbd