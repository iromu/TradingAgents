## Description: <br>
Curated catalogue of 194 agentic AI patterns across 8 categories — context and memory, feedback loops, learning and adaptation, orchestration and control, reliability and evaluation, security and safety, tool use and environment, UX and collaboration — used to look up a proven pattern for an agent design question, diagnose an agent misbehaving in production, or compare two candidate approaches. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: Personal, non-NVIDIA author email in the SKILL.md frontmatter and the skill lives in a third-party awesome list; card_link is the source repository because no separate vendor card exists. The distilled content itself comes from the third-party nibzard/awesome-agentic-patterns catalogue. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Developers and engineers building or operating autonomous agents in production. They use it as an index into `references/` to find which documented pattern fits an agent design problem (context contamination, token waste, runaway loops, missing verification), to compare candidate approaches with their trade-offs, and to check a pattern's maturity label — it recommends patterns rather than writing or refactoring application code. <br>

### Deployment Geography for Use: <br>
Global <br>

## Requirements / Dependencies: <br>
**Requires API Key or External Credential:** [No] <br>
**Credential Type(s):** [None] <br>  

Do not include secrets in prompts/logs/output; use least-privilege credentials; rotate keys as appropriate. See skill body for more details. <br>

## Known Risks and Mitigations: <br>
Risk: Review before execution as proposals could introduce incorrect or misleading guidance into skills. <br>
Mitigation: Review and scan skill before deployment. <br>

## Reference(s): <br>
- [references/INDEX.md (complete registry of all 194 pattern files)](references/INDEX.md) <br>
- [references/context-memory/ (23 context and memory pattern files)](references/context-memory/) <br>
- [references/feedback-loops/ (20 feedback-loop pattern files)](references/feedback-loops/) <br>
- [references/learning-adaptation/ (9 learning and adaptation pattern files)](references/learning-adaptation/) <br>
- [references/orchestration-control/ (45 orchestration and control pattern files)](references/orchestration-control/) <br>
- [references/reliability-eval/ (24 reliability and evaluation pattern files)](references/reliability-eval/) <br>
- [references/security-safety/ (22 security and safety pattern files)](references/security-safety/) <br>
- [references/tool-use-environment/ (33 tool use and environment pattern files)](references/tool-use-environment/) <br>
- [references/ux-collaboration/ (16 UX and collaboration pattern files)](references/ux-collaboration/) <br>
- [agentic-patterns.com (interactive pattern explorer, compare tool, decision guide)](https://agentic-patterns.com) <br>
- [agentic-patterns.com/llms.txt (machine-readable pattern documentation)](https://agentic-patterns.com/llms.txt) <br>
- [nibzard/awesome-agentic-patterns (upstream catalogue this skill distils)](https://github.com/nibzard/awesome-agentic-patterns) <br>


## Skill Output: <br>
**Output Type(s):** [Analysis] <br>
**Output Format:** [Markdown analysis and design guidance — pattern recommendation named with its `references/<category>/<pattern>.md` path, its maturity label, trade-offs, a 'when NOT to use' caveat, and pattern combinations where genuinely needed] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None — documentation-only index; it reads reference files and answers. It never writes files, runs shell commands or makes API/tool calls, and per its own 'When NOT to use' section it should decline simple one-off tasks, deterministic workflows, tiny codebases and sub-second real-time paths.] <br>

## Evaluation Tasks: <br>
Validated against 8 eval cases in `evals/evals.json`: 6 pattern-recommendation scenarios (runaway tool calls, mid-task context exhaustion, a 400-file monorepo migration, an exfiltration-proofing proof, a runaway scheduled job, and embedding-versus-grep retrieval) whose assertions check that the answer names the correct `references/<category>/<pattern>.md` path, quotes its maturity label and keeps write scopes separate; plus 2 routing checks — one core-pattern question that must route to `agentic-patterns-core` and one Postgres indexing question that must not trigger this skill at all. The October 2026 SkillEvaluator run added 9 automated validator checks, all passing (frontmatter/structure, naming, line count, body sections, optional files, author format, semantic version, license, secrets, dead-link scan over 196 markdown files, dependency and test discovery, unicode scan, and the A-quality dimension scoring). <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the recommended pattern actually exists in `references/` and is described faithfully — including its maturity label and 'when NOT to use' caveat — rather than a plausible-sounding invented pattern. <br>
- Discoverability: Whether the skill fires on agent-architecture, reliability, security and tool-use questions and stays silent on core-pattern questions owned by `agentic-patterns-core` and on non-agentic work such as database indexing. <br>
- Reliability: Whether the skill reaches the expected pattern recommendation and reference path consistently across repeat runs. <br>
- Efficiency: Whether it narrows the 194-file catalogue through `references/INDEX.md` and answers without unnecessary steps or context overhead. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 95.2 | A | guide-only |
| Correctness | 90.0 | — | — |
| Discoverability | 95.0 | — | — |
| Reliability | 100.0 | — | — |
| Efficiency | 100.0 | — | — |

## Skill Version(s): <br>
1.0.0 (source: frontmatter) <br>


