## Description: <br>
Canonical reference for 21 agentic design patterns — prompt chaining, routing, parallelization, reflection, tool use, planning, multi-agent collaboration, memory, learning, MCP, goal setting, exception handling, human-in-the-loop, RAG, agent-to-agent communication, resource-aware optimisation, reasoning, guardrails, evaluation, prioritisation and exploration — used to choose, compare and combine the right pattern for an AI agent workflow. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: Personal, non-NVIDIA author email in the SKILL.md frontmatter and the skill lives in a third-party awesome list; card_link is the source repository because no separate vendor card exists. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Developers and engineers designing or refactoring an AI agent workflow. They use it to map a problem to one of the five pattern categories, get the matching pattern's when-to-use conditions plus pros and cons, and pick workable pattern combinations rather than inventing a custom orchestrator. <br>

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
- [references/core/01-prompt-chaining.md](references/core/01-prompt-chaining.md) <br>
- [references/core/02-routing.md](references/core/02-routing.md) <br>
- [references/core/03-parallelization.md](references/core/03-parallelization.md) <br>
- [references/core/04-reflection.md](references/core/04-reflection.md) <br>
- [references/core/05-tool-use.md](references/core/05-tool-use.md) <br>
- [references/advanced/01-planning.md](references/advanced/01-planning.md) <br>
- [references/advanced/02-multi-agent-collaboration.md](references/advanced/02-multi-agent-collaboration.md) <br>
- [references/advanced/03-memory-management.md](references/advanced/03-memory-management.md) <br>
- [references/advanced/04-learning-and-adaptation.md](references/advanced/04-learning-and-adaptation.md) <br>
- [references/advanced/05-model-context-protocol.md](references/advanced/05-model-context-protocol.md) <br>
- [references/system/01-goal-setting-and-monitoring.md](references/system/01-goal-setting-and-monitoring.md) <br>
- [references/system/02-exception-handling-and-recovery.md](references/system/02-exception-handling-and-recovery.md) <br>
- [references/system/03-human-in-the-loop.md](references/system/03-human-in-the-loop.md) <br>
- [references/system/04-knowledge-retrieval-rag.md](references/system/04-knowledge-retrieval-rag.md) <br>
- [references/system/05-inter-agent-communication-a2a.md](references/system/05-inter-agent-communication-a2a.md) <br>
- [references/optimization/01-resource-aware-optimization.md](references/optimization/01-resource-aware-optimization.md) <br>
- [references/optimization/02-reasoning-techniques.md](references/optimization/02-reasoning-techniques.md) <br>
- [references/optimization/03-guardrails-safety-patterns.md](references/optimization/03-guardrails-safety-patterns.md) <br>
- [references/optimization/04-evaluation-and-monitoring.md](references/optimization/04-evaluation-and-monitoring.md) <br>
- [references/strategic/01-prioritization.md](references/strategic/01-prioritization.md) <br>
- [references/strategic/02-exploration-and-discovery.md](references/strategic/02-exploration-and-discovery.md) <br>
- [promptadvisers/agentic-design-patterns-docs (upstream pattern docs)](https://github.com/promptadvisers/agentic-design-patterns-docs) <br>


## Skill Output: <br>
**Output Type(s):** [Analysis] <br>
**Output Format:** [Markdown analysis and design guidance — pattern identification, matched patterns with pros/cons and latency-cost-complexity trade-offs, and suggested pattern combinations] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None — the skill is documentation only; it reads reference files and answers, and writes no files, runs no shell commands and makes no API or tool calls.] <br>

## Evaluation Tasks: <br>
No `evals/evals.json` case set exists for this skill, so no task-level eval cases were run. The October 2026 SkillEvaluator run scored it as `guide-only` and ran 9 automated validator checks, all passing: frontmatter/folder-structure and naming conformance, line-count limit, required body sections, optional supporting-file discovery, author-format check, semantic-version check, license compliance (nothing detected), code-risk analysis (skipped — no code files), Gitleaks secret scan (clean), dead-link scan over 22 markdown files, dependency and Python test-file discovery, unicode-smuggling scan, and the A-quality dimension scoring. <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the 21 pattern definitions, their shapes and their cross-references are accurate and faithful to the distilled upstream pattern docs rather than invented or garbled. <br>
- Discoverability: Whether the skill triggers on agentic-architecture and pattern-selection requests — including the 21 pattern names listed in its description — and stays silent on unrelated work. <br>
- Reliability: Whether the skill reaches the expected pattern recommendation and trade-off presentation consistently across repeat runs. <br>
- Efficiency: Whether it resolves the pattern question without unnecessary steps or context overhead, given that it is a lookup index into `references/`. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 90.2 | A | guide-only |
| Correctness | 90.0 | — | — |
| Discoverability | 90.0 | — | — |
| Reliability | 85.0 | — | — |
| Efficiency | 100.0 | — | — |

## Skill Version(s): <br>
1.0.0 (source: frontmatter) <br>


