## Description: <br>
Build proposition-based knowledge graphs and agent memory with Embabel DICE (Domain-Integrated Context Engineering), which extracts confidence-weighted propositions from text, stores them as the system of record, and projects them to graph, Prolog, agent-memory and report backends. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: The author e-mail in the frontmatter is a personal, non-NVIDIA address and the repository is a third-party awesome list; the card_link is the source repo because no separate vendor card exists. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Developers and engineers adding a proposition-based memory and knowledge-graph layer beneath an Embabel agent: choosing the real DICE Maven modules and property prefixes, wiring PropositionPipeline, entity resolution and proposition queries, attaching the Memory facade to an agent call, and exposing the recall tools over MCP. <br>

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
- [entity-resolution.md](references/entity-resolution.md) <br>
- [mcp-exposure.md](references/mcp-exposure.md) <br>
- [memory.md](references/memory.md) <br>
- [metamodel.md](references/metamodel.md) <br>
- [modules.md](references/modules.md) <br>
- [pipeline.md](references/pipeline.md) <br>
- [projections.md](references/projections.md) <br>
- [upstream-state.md](references/upstream-state.md) <br>


## Skill Output: <br>
**Output Type(s):** [Code, Configuration instructions, Analysis] <br>
**Output Format:** [Markdown with inline Kotlin/Java code blocks plus YAML property configuration under the documented prefixes] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None] <br>

## Evaluation Tasks: <br>
Validated against 8 internal eval cases in `evals/evals.json` (6 positive-trigger and 2 negative-trigger cases, each with an expected-output description and per-case assertions on module coordinates, pipeline/entity-resolution and Memory-facade usage — including checks that it does not invent artifactIds, property prefixes or query factories), plus 9 automated validator checks (schema/repository governance, semantic version, license, code risk, secrets, dead links/dependencies, Unicode, quality and script lint). <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the module names, property prefixes, annotations and API identifiers the answer uses match pinned DICE 0.2.0 upstream (immutable PropositionPipeline, the five real prefixes, the nine real modules) rather than the stale README. <br>
- Discoverability: Whether the skill fires on DICE work — propositions, entity resolution, memory/projection, MCP exposure, metamodel versioning — and stays silent on the agent-authoring, MCP-server and Neo4j-mapping work its 'When NOT to Use' section routes to other skills. <br>
- Reliability: Whether the skill reaches the documented API consistently across repeat runs, including checking a backend capability before relying on it and flagging the EXPERIMENTAL metamodel DSL. <br>
- Efficiency: Whether the task is resolved through the Step 1-10 sequence and the reference files without unnecessary steps or context overhead. <br>

Underlying evaluation signals used in this run: <br>
- `expected_output`: Per-case description of the modules, pipeline calls or configuration the answer should produce. <br>
- `assertions`: Per-case checks naming the exact identifiers and prefixes the answer must use and the invented ones it must avoid. <br>
- `should_trigger`: Routing check run in both directions — 6 cases expect the skill to activate and 2 expect it to stay silent. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 92.0 | A | guide-only |
| Correctness | 95.0 | — | — |
| Discoverability | 90.0 | — | — |
| Reliability | 85.0 | — | — |
| Efficiency | 100.0 | — | — |

## Skill Version(s): <br>
0.2.0 (source: frontmatter) <br>


