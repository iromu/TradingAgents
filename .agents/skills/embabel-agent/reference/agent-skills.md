# Agent Skills Reference

Agent Skills provide a standardized way to extend agent capabilities with reusable, shareable skill packages. Embabel implements the [Agent Skills Specification](https://agentspec.dev).

## What are Agent Skills?

An Agent Skill is a directory containing a `SKILL.md` file with YAML frontmatter and markdown instructions. Skills can include bundled resources:

- `scripts/` — Executable scripts (Python, Bash, etc.)
- `references/` — Documentation and reference materials
- `assets/` — Static resources like templates and data files

Skills use **lazy loading** — only minimal metadata is included in the system prompt, with full instructions loaded when the skill is activated.

## Skill Directory Structure

```
my-skill/
├── SKILL.md        # Required — metadata and instructions
├── scripts/        # Optional — executable scripts
├── references/     # Optional — documentation
└── assets/         # Optional — static resources (templates, data)
```

### SKILL.md Format

```yaml
---
name: my-skill
description: What this skill does and when to use it
---

# My Skill

Instructions go here...
```

## Loading Skills

### From GitHub

```java
// From repository root
var skills = Skills.fromGitHub("github.com/owner/repo");

// Specific branch
var skills = Skills.fromGitHub("github.com/owner/repo/tree/main");

// Specific path within repository
var skills = Skills.fromGitHub("github.com/owner/repo/tree/main/path/to/skills");

// With explicit parameters
var skills = Skills.fromGitHub(
    "github.com/owner/repo",
    "main",
    "path/to/skills"
);
```

### From Local Directories

```java
// Single skill from directory
var skills = Skills.fromLocal(new File("./my-skill"));

// Multiple skills from parent directory (depth 1 only — no recursion)
var skills = Skills.fromLocal(new File("./skills/"));
```

## Using Skills with PromptRunner

```java
var skills = Skills.fromGitHub("github.com/owner/repo");

context.ai().withDefaultLlm()
    .withSkills(skills)
    .creating(Result.class)
    .fromPrompt("Use the available skills");
```

When skills are added as a reference, the agent can:
- See available skills in the system prompt
- Activate skills to get full instructions
- List and read skill resources

## Skill Activation

Skills are activated lazily. The system prompt contains only minimal metadata (~50-100 tokens per skill). When an agent needs a skill, it calls the `activate` tool to load full instructions.

The `Skills` class exposes three LLM tools:
- `activate(name)` — Load full instructions for a skill
- `listResources(skillName, resourceType)` — List files in scripts/references/assets
- `readResource(skillName, resourceType, fileName)` — Read a resource file

## Combining Skills with Other References

Skills can be combined with other `LlmReference` implementations:

```java
var skills = Skills.fromGitHub("github.com/owner/repo");
var persona = new Persona("Expert", "You are an expert.", "Help users.", "Professional");

context.ai().withDefaultLlm()
    .withSkills(skills)
    .withPromptContributor(persona)
    .creating(Result.class)
    .fromPrompt("...");
```

## Validation

Skills are validated when loaded:

- **Frontmatter validation** — Required fields (name, description) and field lengths
- **File reference validation** — Paths in instructions (e.g., `scripts/build.sh`) must exist
- **Name matching** — Skill name must match its parent directory name

To disable file reference validation:

```java
var skills = Skills.fromLocal(new File("./skills/"))
    .withValidation(false);
```

## Current Limitations

### Script Execution

Skills with `scripts/` directories are loaded, but script execution is not yet supported. A warning is logged when such skills are loaded.

### allowed-tools Field

The `allowed-tools` frontmatter field is parsed but not currently enforced.

## Key Points

- Skills use lazy loading — metadata in system prompt, full content on `activate`
- GitHub URLs support branch and path specification
- Local loading scans immediate subdirectories only (depth 1)
- Skills are validated on load (frontmatter, file references, name matching)
- Combine with other `LlmReference` implementations
- Script execution is not yet supported
