## Description: <br>
Build agentic, RAG-grounded chatbots on the JVM with Embabel — a long-lived AgentProcess that keeps blackboard state across a session, drives tool-based retrieval through ToolishRag, and optionally persists conversations through embabel-chat-store. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: The author e-mail in the frontmatter is a personal, non-NVIDIA address and the repository is a third-party awesome list; the card_link is the source repo because no separate vendor card exists. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Developers and engineers building a conversational assistant on the JVM that must answer over their own documents: wiring the Chatbot/ChatSession/Conversation surface and an @Action(trigger = UserMessage) handler, attaching agentic ToolishRag retrieval with metadata/entity filters, ingesting and chunking documents, and persisting conversation history in a Spring Boot service. <br>

### Deployment Geography for Use: <br>
Global <br>

## Requirements / Dependencies: <br>
**Requires API Key or External Credential:** [Not Specified] <br>
**Credential Type(s):** [None identified] <br>  

Do not include secrets in prompts/logs/output; use least-privilege credentials; rotate keys as appropriate. See skill body for more details. <br>

## Known Risks and Mitigations: <br>
Risk: Review before execution as proposals could introduce incorrect or misleading guidance into skills. <br>
Mitigation: Review and scan skill before deployment. <br>

## Reference(s): <br>
- [01-chatbot-api.md](references/01-chatbot-api.md) <br>
- [02-conversation-store.md](references/02-conversation-store.md) <br>
- [03-rag-architecture.md](references/03-rag-architecture.md) <br>
- [04-guardrails.md](references/04-guardrails.md) <br>
- [05-reasoning.md](references/05-reasoning.md) <br>
- [06-chatbot-patterns.md](references/06-chatbot-patterns.md) <br>
- [07-structured-output.md](references/07-structured-output.md) <br>
- [08-quickstart.md](references/08-quickstart.md) <br>
- [Embabel Agent guide 1.5.1 (upstream docs)](https://docs.embabel.com/embabel-agent/guide/1.5.1/) <br>


## Skill Output: <br>
**Output Type(s):** [Code, Configuration instructions] <br>
**Output Format:** [Markdown with inline Kotlin/Java code blocks plus Maven dependency and Spring configuration snippets] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None] <br>

## Evaluation Tasks: <br>
Validated against 8 internal eval cases in `evals/evals.json` (6 positive-trigger and 2 negative-trigger cases, each with an expected-output description and per-case assertions on the chatbot, conversation-store and RAG wiring — including checks that it refuses to invent LangChain4j-style ChatbotBuilder/ChatMemory APIs), plus 9 automated validator checks (schema/repository governance, semantic version, license, code risk, secrets, dead links/dependencies, Unicode, quality and script lint). <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the answer uses the real Embabel chatbot surface (Chatbot, ChatSession, Conversation, AgentProcessChatbot, ToolishRag, PropertyFilter/EntityFilter) instead of the LangChain4j idioms the skill explicitly lists as non-existent. <br>
- Discoverability: Whether the skill fires on chatbot/RAG/conversation-store requests and stays silent on the core planner, MCP-publishing and provider-configuration work its 'When NOT to Use' table routes to other skills. <br>
- Reliability: Whether the skill reproduces the documented minimal skeleton and reference guidance consistently across repeat runs rather than improvising a new wiring. <br>
- Efficiency: Whether the chatbot is assembled through the documented quick start and 'What to Add Next' table without unnecessary steps or context overhead. <br>

Underlying evaluation signals used in this run: <br>
- `expected_output`: Per-case description of the action class, session wiring or dependency configuration the answer should produce. <br>
- `assertions`: Per-case checks on the annotations, trigger wiring, filters and store types the answer must name or must not invent. <br>
- `should_trigger`: Routing check run in both directions — 6 cases expect the skill to activate and 2 expect it to stay silent. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 89.2 | B | guide-only |
| Correctness | 95.0 | — | — |
| Discoverability | 90.0 | — | — |
| Reliability | 80.0 | — | — |
| Efficiency | 90.0 | — | — |

## Skill Version(s): <br>
1.5.1 (source: frontmatter) <br>


