Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 1.1. Glossary
Before we begin, in this glossary we’ll explain some terms that may be new if you’re taking your first steps as
an applied AI software developer. It is assumed that you already know what a large language model (LLM) is from an
end-user’s point of view.
| | You may skim or skip this section if you’re already a seasoned agentic AI engineer. |
Agent An Agent in the Embabel framework is a self-contained component that bundles together domain logic, AI capabilities,
and tool usage to achieve a specific goal on behalf of the user.Inside, it exposes multiple `@Action` methods, each representing discrete steps the agent can take. Actions depend on
typically structured (sometimes natural language) input. The input is used to perform tasks on behalf of the user -
executing domain code, calling AI models or even calling other agents as a sub-process.When an AI model is called it may be given access to tools that expand its capabilities in order to achieve a goal.
The output is a new type, representing a transformation of the input, however during execution one or more side-effects
can occur. An example of side effects might be new records stored in a database, orders placed on an e-commerce site
and so on.ToolsTools extend the raw capabilities of an LLM by letting it interact with the outside world.
On its own, a language model can only generate responses from its training data and context window, which risks
producing inaccurate or “hallucinated” answers.While tool usage is inspired by an technique known as **ReAct** (Reason + Act), which itself builds on **Chain of Thought**
reasoning, most recent LLMs allow specifying tools specifically instead of relying on prompt engineering techniques.When tools are present, the LLM interprets the user request, plans steps, and then delegates certain tasks to tools in a loop. This
lets the model alternate between reasoning (“what needs to be done?”) and acting (“which tool can do it?”).**Benefits of tools include:**
- The ability to answer questions or perform tasks beyond what the LLM was trained on, by delegating to domain-specific
or external systems.
- Producing useful **side effects**, such as creating database records, generating visualizations, booking flights, or
invoking any process the system designer provides.In short, tools are one way to bridge the gap between **text prediction** and **real-world action**, turning an LLM into a
practical agent capable of both reasoning and execution. In Embabel many tools are bound domain objects.MCPModel Context Protocol (MCP) is a standardized way of hosting and sharing tools.
Unlike plain tools, which are usually wired directly into one agent or app, an MCP Server makes tools discoverable and
reusable across models and runtimes they can be registered system-wide or at runtime, and invoked through a common
protocol. Embabel can both consume and publish such tools for systems integration.Domain Integrated Context Engineering (DICE)Enhances context engineering by grounding both LLM inputs and outputs in typed domain objects.
Instead of untyped prompts, context is structured with business-aware models that provide precision, testability,
and seamless integration with existing systems. DICE transforms context into a re-usable, inspectable, and reliably
manipulable artifact.

### 1.2. Why do we need an Agent Framework?
Aren’t LLMs smart enough to solve our problems directly?
Aren’t MCP tools all we need to allow them to solve complex problems?
LLMs seem to get more capable by the day and MCPs can
give LLMs access to a lot of empowering tools, making them even more capable.But there are still many reasons that a higher level orchestration technology is needed, especially for business applications.
Here are some of the most important:
- **Explainability**: Why were choices made in solving a problem?
- **Discoverability**: How do we find the right tools at each point, and ensure that models aren’t confused in choosing between them?
- **Ability to mix models**, so that we are not reliant only on the largest models but can use local, cheaper, private models for many tasks
- **Ability to inject guardrails** at any point in a flow
- **Ability to manage flow execution** and introduce greater resilience
- **Composability of flows at scale**.
We’ll soon be seeing not just agents running on one system, but federations of agents.
- **Safer integration with sensitive existing systems** such as databases, where it is dangerous to allow even the best LLM write access.Agent frameworks break complex tasks into smaller, manageable components, offering greater control and predictability.Agent frameworks offer "code agency" as well as "LLM agency." This division is well described in this
paper from NVIDIA Research.Further reading:
- Embabel: A new Agent Platform For the JVM
- The Embabel Vision

### 1.3. Embabel Differentiators
So how does Embabel differ from other agent frameworks? We like to believe the Embabel agent framework is to be the best fit for developing agentic AI in the enterprise.
#### 1.3.1. Sophisticated Planning
Goes beyond a finite state machine or sequential execution with nesting by introducing a true planning step, using a non-LLM AI algorithm.
This enables the system to perform tasks it wasn’t programmed to do by combining known steps in a novel order, as well as make decisions about parallelization and other runtime behavior.
#### 1.3.2. Superior Extensibility and Reuse
Because of dynamic planning, adding more domain objects, actions, goals and conditions can extend the capability of the system, *without editing FSM definitions* or existing code.
#### 1.3.3. Strong Typing and Object Orientation
Actions, goals and conditions are informed by a domain model, which can include behavior.
Everything is strongly typed and prompts and manually authored code interact cleanly.
No more magic maps.
Enjoy full refactoring support.
#### 1.3.4. Platform Abstraction
Clean separation between programming model and platform internals allows running locally while potentially offering higher QoS in production without changing application code.
#### 1.3.5. LLM Mixing
It is easy to build applications that mix LLMs, ensuring the most cost-effective yet capable solution.
This enables the system to leverage the strengths of different models for different tasks.
In particular, it facilitates the use of local models for point tasks.
This can be important for cost and privacy.
#### 1.3.6. Spring and JVM Integration
Built on Spring and the JVM, making it easy to access existing enterprise functionality and capabilities.
For example:
- Spring can inject and manage agents, including using Spring AOP to decorate functions.
- Robust persistence and transaction management solutions are available.
#### 1.3.7. Designed for Testability
Both unit testing and agent end-to-end testing are easy from the ground up.