## Description: <br>
Author agentic Java/Kotlin services on the JVM with the Embabel v1.5.3 Spring-based framework, which mixes LLM calls with non-LLM planning (GOAP, Utility AI, Hybrid, Supervisor) across agent authoring, tools, provider configuration, durable agent process persistence, MCP publishing over Streamable HTTP, testing and migration from CrewAI/Pydantic AI/LangGraph. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: The author e-mail in the frontmatter is a personal, non-NVIDIA address and the repository is a third-party awesome list; the card_link is the source repo because no separate vendor card exists. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Developers and engineers building agentic backend services on the JVM with Embabel: choosing a planner and execution mode, writing @Agent/@Action/@Tool code and domain models, selecting per-action model providers and roles, publishing an agent as an MCP server, and testing or migrating an existing Python agent framework (CrewAI, Pydantic AI, LangGraph) onto it. <br>

### Deployment Geography for Use: <br>
Global <br>

## Requirements / Dependencies: <br>
**Requires API Key or External Credential:** [Yes] <br>
**Credential Type(s):** [API key] <br>  

Do not include secrets in prompts/logs/output; use least-privilege credentials; rotate keys as appropriate. See skill body for more details. <br>

## Known Risks and Mitigations: <br>
Risk: Review before execution as proposals could introduce incorrect or misleading guidance into skills. <br>
Mitigation: Review and scan skill before deployment. <br>

## Reference(s): <br>
- [agent-skills.md](references/agent-skills.md) <br>
- [annotations.md](references/annotations.md) <br>
- [api-spi.md](references/api-spi.md) <br>
- [async-mode.md](references/async-mode.md) <br>
- [bedrock.md](references/bedrock.md) <br>
- [cache.md](references/cache.md) <br>
- [chatbots.md](references/chatbots.md) <br>
- [common-pitfalls.md](references/common-pitfalls.md) <br>
- [configuration.md](references/configuration.md) <br>
- [cost-tracking.md](references/cost-tracking.md) <br>
- [customizing.md](references/customizing.md) <br>
- [dashscope.md](references/dashscope.md) <br>
- [domain.md](references/domain.md) <br>
- [dsl-builders.md](references/dsl-builders.md) <br>
- [embabel-agent-examples (upstream examples)](https://github.com/embabel/embabel-agent-examples) <br>
- [error-handling.md](references/error-handling.md) <br>
- [examples.md](references/examples.md) <br>
- [flow.md](references/flow.md) <br>
- [guardrails.md](references/guardrails.md) <br>
- [integrations.md](references/integrations.md) <br>
- [interceptors.md](references/interceptors.md) <br>
- [invoking.md](references/invoking.md) <br>
- [java-agent-template (upstream scaffold)](https://github.com/embabel/java-agent-template) <br>
- [kotlin-agent-template (upstream scaffold)](https://github.com/embabel/kotlin-agent-template) <br>
- [llm-integration.md](references/llm-integration.md) <br>
- [migrating.md](references/migrating.md) <br>
- [minimax.md](references/minimax.md) <br>
- [persistence.md](references/persistence.md) <br>
- [planners.md](references/planners.md) <br>
- [production-deployment.md](references/production-deployment.md) <br>
- [providers.md](references/providers.md) <br>
- [rag.md](references/rag.md) <br>
- [states.md](references/states.md) <br>
- [streaming.md](references/streaming.md) <br>
- [structured-prompts.md](references/structured-prompts.md) <br>
- [termination.md](references/termination.md) <br>
- [testing.md](references/testing.md) <br>
- [thinking.md](references/thinking.md) <br>
- [tooling.md](references/tooling.md) <br>
- [tools.md](references/tools.md) <br>
- [troubleshooting.md](references/troubleshooting.md) <br>
- [types.md](references/types.md) <br>
- [zai.md](references/zai.md) <br>


## Skill Output: <br>
**Output Type(s):** [Code, Configuration instructions, Analysis] <br>
**Output Format:** [Markdown with inline Java/Kotlin code blocks plus full application.yml configuration and unit/integration test code] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None] <br>

## Evaluation Tasks: <br>
Validated against 17 internal eval cases in `evals/evals.json` (all positive-trigger, each with an expected-output description and per-case assertions on real Embabel symbols such as @Agent/@Action classes, FakeOperationContext tests, provider configuration and process-persistence setup), plus 9 automated validator checks (schema/repository governance, semantic version, license, code risk, secrets, dead links/dependencies, Unicode, quality and script lint). <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the Embabel API the answer asserts is real for v1.5.3 — correct starter artifacts, annotation attributes, planner and @State semantics — rather than asserted from memory. <br>
- Discoverability: Whether the skill fires on Embabel agent-authoring requests (agents, tools, planners, MCP publishing, migration) and stays silent on the non-Embabel work its 'When NOT to Use' table routes elsewhere. <br>
- Reliability: Whether the skill reaches the expected result consistently across repeat runs, including that it consults references/ rather than inventing symbols. <br>
- Efficiency: Whether the skill resolves the task through the right section and reference file without unnecessary steps or context overhead. <br>

Underlying evaluation signals used in this run: <br>
- `expected_output`: Per-case description of the class, test or configuration the answer should produce. <br>
- `assertions`: Per-case checks on the specific annotations, API calls and configuration keys the answer must contain. <br>
- `should_trigger`: Routing check that the skill activates for the request; every case in this file is a positive-trigger case. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 98.0 | A | script-based |
| Correctness | 100.0 | — | — |
| Discoverability | 95.0 | — | — |
| Reliability | 100.0 | — | — |
| Efficiency | 95.0 | — | — |

## Skill Version(s): <br>
1.5.3 (source: frontmatter) <br>


