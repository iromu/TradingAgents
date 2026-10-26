## Description: <br>
Build interactive web UIs with htmx — attribute-driven AJAX, CSS transitions, WebSockets, and SSE using HTML attributes, with no JavaScript framework or build step. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: Frontmatter names a personal author (non-NVIDIA email) and the repo is a third-party awesome list; the card_link points at the source repository because no separate vendor card exists. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Developers and engineers building server-rendered, hypermedia-driven web UIs with htmx 4.x, or migrating an existing htmx 2.x / React-Vue front-end to htmx 4 markup and APIs. <br>

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
- [attributes.md](references/attributes.md) <br>
- [configuration.md](references/configuration.md) <br>
- [events-api.md](references/events-api.md) <br>
- [extensions.md](references/extensions.md) <br>
- [upgrade-from-2.md](references/upgrade-from-2.md) <br>


## Skill Output: <br>
**Output Type(s):** [Code, Configuration instructions] <br>
**Output Format:** [Markdown with inline HTML/JavaScript code blocks] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None] <br>

## Evaluation Tasks: <br>
Validated against 5 internal eval cases in `evals/evals.json` (trigger checks plus per-case expected-output assertions), plus 9 automated schema/repository and semantic-version checks. <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the generated htmx markup uses real v4 attributes, events and config keys rather than invented or 2.x-era syntax. <br>
- Discoverability: Whether the skill triggers on htmx/hypermedia requests and does not fire on unrelated frontend work. <br>
- Reliability: Whether the skill reaches the expected result consistently across repeat runs. <br>
- Efficiency: Whether the skill completes the task without unnecessary steps or context overhead. <br>

Underlying evaluation signals used in this run: <br>
- `expected_output`: Per-case assertion set checking the attributes, swap strategies and triggers the answer should contain. <br>
- `should_trigger`: Routing check that the skill activates for htmx work and stays silent for React/Vue or non-web requests. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 89.5 | B | resource-based |
| Correctness | 95.0 | — | — |
| Discoverability | 80.0 | — | — |
| Reliability | 85.0 | — | — |
| Efficiency | 100.0 | — | — |

## Skill Version(s): <br>
1.1.0 (source: frontmatter) <br>


