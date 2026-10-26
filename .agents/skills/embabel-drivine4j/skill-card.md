## Description: <br>
Build type-safe graph database clients with Drivine4j 0.0.81 for Neo4j, FalkorDB, Amazon Neptune, Memgraph and the in-process EMBABEL Cypher engine, choosing between the low-level PersistenceManager for hand-written Cypher and the high-level GraphObjectManager for annotated @NodeFragment/@GraphView models. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: The author e-mail in the frontmatter is a personal, non-NVIDIA address and the repository is a third-party awesome list; the card_link is the source repo because no separate vendor card exists. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Java and Kotlin developers wiring a graph-database persistence layer beneath an Embabel agent or service: wiring a DataSourceMap/ConnectionProperties, choosing between hand-written Cypher and annotation-driven graph models, mapping relationships and cascade behaviour, and provisioning HNSW vector, full-text, range and uniqueness schema against Neo4j, FalkorDB, Memgraph or Neptune. <br>

### Deployment Geography for Use: <br>
Global <br>

## Requirements / Dependencies: <br>
**Requires API Key or External Credential:** [Yes] <br>
**Credential Type(s):** [Other [Graph database credentials]] <br>  

Do not include secrets in prompts/logs/output; use least-privilege credentials; rotate keys as appropriate. See skill body for more details. <br>

## Known Risks and Mitigations: <br>
Risk: Review before execution as proposals could introduce incorrect or misleading guidance into skills. <br>
Mitigation: Review and scan skill before deployment. <br>

## Reference(s): <br>
- [graph-object-manager.md](references/graph-object-manager.md) <br>
- [multi-db.md](references/multi-db.md) <br>
- [persistence-manager.md](references/persistence-manager.md) <br>
- [schema-and-search.md](references/schema-and-search.md) <br>
- [liberation-data/drivine4j (upstream source of record, main @ 55249b4)](https://github.com/liberation-data/drivine4j) <br>


## Skill Output: <br>
**Output Type(s):** [Code, Configuration instructions, Analysis] <br>
**Output Format:** [Markdown with inline Kotlin/Java code blocks plus Gradle dependency and Spring/YAML datasource configuration snippets] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None] <br>

## Evaluation Tasks: <br>
Validated against 12 internal eval cases in `evals/evals.json` (10 positive-trigger and 2 negative-trigger cases, each with an expected-output description and per-case assertions on the pinned version coordinate, the PersistenceManager vs GraphObjectManager choice, cascade/NullPolicy semantics and the exact query method names), plus 9 automated validator checks (schema/repository governance, semantic version, license, code risk, secrets, dead links/dependencies, Unicode, quality and script lint). <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the answer uses the real Drivine4j 0.0.81 surface — pinned version, required ConnectionProperties.type, real annotation attributes and query method names — instead of the stale README coordinates or invented APIs. <br>
- Discoverability: Whether the skill fires on graph-client work (datasource wiring, graph mapping, Cypher, vector/full-text/keyset search) and stays silent on the agent-authoring, proposition and relational-persistence work its 'When NOT to Use' section routes elsewhere. <br>
- Reliability: Whether the skill reaches the documented API consistently across repeat runs, including gating on supportsSchemaManagement and engine capability rather than assuming an engine supports an index type. <br>
- Efficiency: Whether the graph layer is resolved through the right API and reference file without unnecessary steps or context overhead. <br>

Underlying evaluation signals used in this run: <br>
- `expected_output`: Per-case description of the dependency pin, API choice or mapping/query configuration the answer should produce. <br>
- `assertions`: Per-case checks on the exact versions, annotation attributes, method names and engine caveats the answer must state or avoid. <br>
- `should_trigger`: Routing check run in both directions — 10 cases expect the skill to activate and 2 expect it to stay silent. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 90.5 | A | guide-only |
| Correctness | 95.0 | — | — |
| Discoverability | 85.0 | — | — |
| Reliability | 90.0 | — | — |
| Efficiency | 90.0 | — | — |

## Skill Version(s): <br>
0.0.81 (source: frontmatter) <br>


