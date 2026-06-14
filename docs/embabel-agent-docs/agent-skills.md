Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.36. Agent Skills
Agent Skills provide a standardized way to extend agent capabilities with reusable, shareable skill packages.
Skills are loaded dynamically and provide instructions, resources, and tools to agents.Embabel implements the Agent Skills Specification.
#### 4.36.1. What are Agent Skills?
An Agent Skill is a directory containing a `SKILL.md` file with YAML frontmatter and markdown instructions.
Skills can also include bundled resources:
- `scripts/` - Executable scripts (Python, Bash, etc.)
- `references/` - Documentation and reference materials
- `assets/` - Static resources like templates and data filesSkills use a **lazy loading** pattern: only minimal metadata is included in the system prompt, with full instructions loaded when the skill is activated.
#### 4.36.2. Using Skills with PromptRunner
The `Skills` class implements `LlmReference`, allowing it to be passed to a `PromptRunner`:JavaKotlinWhen skills are added as a reference, the agent can:
- See available skills in the system prompt
- Activate skills to get full instructions
- List and read skill resources
#### 4.36.3. Loading Skills from GitHub
The simplest way to load skills is from a GitHub URL:JavaKotlinSupported URL formats:
- `github.com/owner/repo` - Load from repository root
- `github.com/owner/repo/tree/branch` - Specific branch
- `github.com/owner/repo/tree/branch/path/to/skills` - Specific pathFor more control, use explicit parameters:JavaKotlin
#### 4.36.4. Loading Skills from Local Directories
Load a single skill from a directory containing `SKILL.md`:JavaKotlinLoad multiple skills from a parent directory:JavaKotlin
| | `withLocalSkills` scans immediate subdirectories only (depth 1).
It does not recurse into nested directories. |

#### 4.36.5. Skill Directory Structure
A skill directory must contain a `SKILL.md` file:
```
my-skill/
├── SKILL.md # Required - metadata and instructions
├── scripts/ # Optional - executable scripts
├── references/ # Optional - documentation
└── assets/ # Optional - static resources
```
The `SKILL.md` file uses YAML frontmatter:
#### 4.36.6. Skill Activation
Skills are activated lazily.
The system prompt contains only minimal metadata (~50-100 tokens per skill).
When an agent needs a skill, it calls the `activate` tool to load full instructions.The `Skills` class exposes three LLM tools:
- `activate(name)` - Load full instructions for a skill
- `listResources(skillName, resourceType)` - List files in scripts/references/assets
- `readResource(skillName, resourceType, fileName)` - Read a resource file
#### 4.36.7. Combining Skills with Other References
Skills can be combined with other `LlmReference` implementations:JavaKotlin
#### 4.36.8. Validation
Skills are validated when loaded:
- **Frontmatter validation** - Required fields (name, description) and field lengths
- **File reference validation** - Paths in instructions (e.g., `scripts/build.sh`) must exist
- **Name matching** - Skill name must match its parent directory nameTo disable file reference validation:JavaKotlin
#### 4.36.9. Current Limitations
Script executionSkills with `scripts/` directories are loaded, but script execution is not yet supported.
A warning is logged when such skills are loaded.allowed-tools fieldThe `allowed-tools` frontmatter field is parsed but not currently enforced.See the Agent Skills Specification for the full specification.