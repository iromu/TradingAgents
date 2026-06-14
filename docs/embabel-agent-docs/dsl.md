Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.7. DSL
You can also create agents using a DSL in Kotlin or Java.This is useful for workflows where you want an atomic action that is complete in itself but may contain multiple steps or actions.
#### 4.7.1. Standard Workflows
There are a number of standard workflows, constructed by builders, that meet common requirements.
These can be used to create agents that will be exposed as Spring beans, or within `@Action` methods within other agents.
All are type safe.
As far as possible, they use consistent APIs.
- `SimpleAgentBuilder`: The simplest agent, with no preconditions or postconditions.
- `ScatterGatherBuilder`: Fork join pattern for parallel processing.
- `ConsensusBuilder`: A pattern for reaching consensus among multiple sources.
Specialization of `ScatterGather`.
- `RepeatUntil`: Repeats a step until a condition is met.
- `RepeatUntilAcceptable`: Repeats a step while a condition is met, with a separate evaluator providing feedback.Creating a simple agent:JavaKotlin
| **1** | Specify the return type. || **2** | specify the action to run.
Takes a `SupplierActionContext<RESULT>``OperationContext` parameter allowing access to the current `AgentProcess`. || **3** | Build an agent with the given name and description. |
A more complex example:JavaKotlin
| **1** | Start building a scatter gather agent. || **2** | Specify the return type of the overall agent. || **3** | Specify the type of elements to be gathered. || **4** | Specify the list of functions to run in parallel, each generating an element, here of type `FactCheck`. || **5** | Specify a function to consolidate the results.
In this case it will take a list of `FactCheck` and return a `FactCheck` and return a `FactChecks` object. || **6** | Build and run the agent as a subprocess of the current process.
This is an alternative to `buildAgent` shown in the `SimpleAgentBuilder` example.
The API is consistent. |

| | If you wish to experiment, the embabel-agent-examples repository includes the fact checker shown above. |

#### 4.7.2. Registering `Agent` beans
Whereas the `@Agent` annotation causes a class to be picked up immediately by Spring, with the DSL you’ll need an extra step to register an agent with Spring. As shown in the example below, any `@Bean` of `Agent` type results auto registration, just like declaring a class annotated with `@Agent` does.JavaKotlin