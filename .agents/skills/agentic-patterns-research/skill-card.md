## Description: <br>
Research-backed collection of 220 deep-dive reports on agentic AI design patterns — context management, planning and reasoning, agent architecture, code and tooling, safety and security, CI/CD and testing, resource management and workflow design — used to design or evaluate an agent architecture with academic and production evidence rather than guesswork. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: Personal, non-NVIDIA author email in the SKILL.md frontmatter and the skill lives in a third-party awesome list; card_link is the source repository because no separate vendor card exists. The 220 reports it carries are verbatim copies of the third-party nibzard/awesome-agentic-patterns corpus. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Developers and engineers designing an agent architecture from scratch, refactoring an existing one, or hardening one already in production. They use it to weigh pattern trade-offs and check documented production evidence for safety controls (prompt-injection defence, egress lockdown, guardrails), cost levers (model routing, context minimisation, code-over-API), CI/CD integration and multi-agent orchestration before committing to a design. <br>

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
- [references/INDEX.md (complete registry — every report with title and length)](references/INDEX.md) <br>
- [references/01-pattern-taxonomy.md (master taxonomy across all categories)](references/01-pattern-taxonomy.md) <br>
- [references/01-context-memory-patterns.md](references/01-context-memory-patterns.md) <br>
- [references/01-reasoning-patterns.md](references/01-reasoning-patterns.md) <br>
- [references/02-reasoning-patterns-detailed.md](references/02-reasoning-patterns-detailed.md) <br>
- [references/01-orchestration-patterns.md](references/01-orchestration-patterns.md) <br>
- [references/02-orchestration-patterns-detailed.md](references/02-orchestration-patterns-detailed.md) <br>
- [references/01-security-patterns.md](references/01-security-patterns.md) <br>
- [references/01-reliability-patterns.md](references/01-reliability-patterns.md) <br>
- [references/01-tool-use-patterns.md](references/01-tool-use-patterns.md) <br>
- [references/01-feedback-loop-patterns.md](references/01-feedback-loop-patterns.md) <br>
- [references/02-feedback-loop-patterns-detailed.md](references/02-feedback-loop-patterns-detailed.md) <br>
- [references/01-learning-patterns.md](references/01-learning-patterns.md) <br>
- [references/01-ux-collaboration-patterns.md](references/01-ux-collaboration-patterns.md) <br>
- [nibzard/awesome-agentic-patterns (upstream corpus copied into references/)](https://github.com/nibzard/awesome-agentic-patterns) <br>


## Skill Output: <br>
**Output Type(s):** [Analysis] <br>
**Output Format:** [Markdown analysis and design guidance — pattern recommendation or comparison grounded in a named `references/*-report.md` research report, with its production-validation status, trade-offs and related patterns] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None — documentation-only corpus; reads reference reports and answers. Writes no files, runs no shell commands, makes no API or tool calls. Depth-oriented by design: for breadth or quick pattern comparison the skill itself defers to the sibling `agentic-patterns-extra` skill or to `agentic-patterns-core` for the canonical core patterns.] <br>

## Evaluation Tasks: <br>
Validated against 8 eval cases in `evals/evals.json`: 6 pattern-recommendation scenarios (code-over-API for ferrying API responses, context minimisation as delayed prompt-injection defence, hook-based guard rails for an agent that reasons past its safety rules, egress lockdown for an agent making outbound calls, budget-aware model routing with hard cost caps, and a taxonomy-level orientation request) whose assertions check that the answer names the correct `references/*.md` report, grounds the claim in that report's evidence, and keeps enforcement or cost caps outside the model's own reasoning loop; plus 2 routing checks — one breadth question that must route to `agentic-patterns-extra` and one Postgres indexing question that must not trigger this skill at all. The October 2026 SkillEvaluator run added 9 automated validator checks, all passing (frontmatter/structure, naming, line count, body sections, optional files, author format, semantic version, license, secrets, dead-link scan over 235 markdown files, dependency and test discovery, unicode scan, and the B-quality dimension scoring). <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the cited research report exists and is represented faithfully — including its validation status and any flagged-hallucinated source — rather than a plausible-sounding invented finding. <br>
- Discoverability: Whether the skill fires on agent-architecture, safety, cost and CI/CD questions and defers to `agentic-patterns-extra` for breadth and to non-agentic work such as database indexing. <br>
- Reliability: Whether the skill reaches the expected report and recommendation consistently across repeat runs. <br>
- Efficiency: Whether it orients via the category overviews and `references/INDEX.md` and answers without unnecessary steps or context overhead, given that each report is long-form. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 88.0 | B | guide-only |
| Correctness | 90.0 | — | — |
| Discoverability | 90.0 | — | — |
| Reliability | 85.0 | — | — |
| Efficiency | 85.0 | — | — |

## Skill Version(s): <br>
1.0.0 (source: frontmatter) <br>


